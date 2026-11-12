#!/usr/bin/env python3
"""Audit duplicate votes; optionally keep the latest record with guarded atomic writes."""
import argparse
from collections import defaultdict
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request


def document_key(meeting, user):
    return f'selection-{meeting}-{user}'


def modified(document):
    fields = document['fields']
    value = fields.get('updatedAt', fields.get('createdAt', {})).get('stringValue', '')
    if not value:
        return ''
    if not re.fullmatch(r'\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d{1,9})?', value):
        raise ValueError('Unrecognized modification timestamp')
    datetime.fromisoformat(value)
    base, _, fraction = value.partition('.')
    return base + '.' + fraction.ljust(9, '0')


def payload(document):
    return {key: value for key, value in document['fields'].items()
            if key not in ('id', 'createdAt', 'updatedAt')}


def latest(documents):
    timestamps = [modified(document) for document in documents]
    newest = max(timestamps)
    candidates = [document for document, timestamp in zip(documents, timestamps) if timestamp == newest]
    if len({json.dumps(payload(document), sort_keys=True) for document in candidates}) > 1:
        raise ValueError('Conflicting votes have the same or missing modification timestamp')
    # Match the repository's deterministic fallback when timestamps tie.
    return max(candidates, key=lambda document: int(document['fields'].get('id', {}).get('integerValue', '0')))


class Client:
    def __init__(self, project, emulator_host=None):
        self.project = project
        if emulator_host:
            assert project.startswith('demo-') and re.fullmatch(r'(localhost|127\.0\.0\.1):\d+', emulator_host)
            self.origin = 'http://' + emulator_host
            self.headers = {}
        else:
            self.origin = 'https://firestore.googleapis.com'
            token = subprocess.check_output(['gcloud', 'auth', 'print-access-token'], text=True).strip()
            self.headers = {'Authorization': 'Bearer ' + token, 'x-goog-user-project': project}
        self.path = f'projects/{project}/databases/(default)/documents'
        self.base = self.origin + '/v1/' + self.path

    def request(self, suffix, method='GET', data=None):
        request = urllib.request.Request(self.base + suffix, method=method,
            headers={**self.headers, 'Content-Type': 'application/json'},
            data=None if data is None else json.dumps(data).encode())
        with urllib.request.urlopen(request, timeout=40) as response:
            raw = response.read()
            return json.loads(raw) if raw else {}

    def votes(self):
        documents = []
        token = ''
        while True:
            page = self.request('/meetingUserSelections?' + urllib.parse.urlencode({'pageSize': 1000, 'pageToken': token}))
            documents.extend(page.get('documents', []))
            token = page.get('nextPageToken')
            if not token:
                return documents


def audit(documents):
    groups = defaultdict(list)
    malformed = []
    for document in documents:
        try:
            fields = document['fields']
            group = (int(fields['meetingId']['integerValue']), int(fields['userId']['integerValue']))
            groups[group].append(document)
        except (KeyError, TypeError, ValueError):
            malformed.append(document['name'])
    duplicates = []
    for (meeting, user), values in sorted(groups.items()):
        if len(values) < 2:
            continue
        item = {'meetingId': str(meeting), 'userId': str(user), 'records': len(values)}
        try:
            chosen = latest(values)
            item.update({'chosen': chosen['name'], 'modified': modified(chosen), 'repairable': True})
        except ValueError as error:
            item.update({'repairable': False, 'reason': str(error)})
        duplicates.append(item)
    return {'records': len(documents), 'participants': len(groups), 'malformed': malformed,
            'duplicateGroups': duplicates}, groups


def repair(client, group, documents):
    selected = latest(documents)
    canonical = client.path + '/meetingUserSelections/' + document_key(*group)
    existing = next((document for document in documents if document['name'] == canonical), None)
    writes = [{'update': {'name': canonical, 'fields': selected['fields']},
               'currentDocument': {'updateTime': existing['updateTime']} if existing else {'exists': False}}]
    writes.extend({'delete': document['name'], 'currentDocument': {'updateTime': document['updateTime']}}
                  for document in documents if document['name'] != canonical)
    assert len(writes) <= 450, 'Unexpectedly large duplicate group'
    return client.request(':commit', 'POST', {'writes': writes})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project', required=True)
    parser.add_argument('--emulator-host')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    client = Client(args.project, args.emulator_host)
    documents = client.votes()
    before, groups = audit(documents)
    result = {'project': args.project, 'verifiedAt': datetime.now(timezone.utc).isoformat(),
              'applied': args.apply, 'before': before, 'repairs': []}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.apply:
        # Preserve exact fields and preconditions before any mutation. Never overwrite a backup.
        backup = args.output.with_suffix('.backup.json')
        with backup.open('x') as stream:
            json.dump({'project': args.project, 'documents': [document for values in groups.values() if len(values) > 1 for document in values]}, stream, indent=2)
        result['backup'] = str(backup)
        for item in before['duplicateGroups']:
            if not item['repairable']:
                continue
            group = (int(item['meetingId']), int(item['userId']))
            try:
                commit = repair(client, group, groups[group])
                result['repairs'].append({**item, 'committed': True, 'commitTime': commit['commitTime']})
            except urllib.error.HTTPError as error:
                result['repairs'].append({**item, 'committed': False, 'status': error.code})
        result['after'], _ = audit(client.votes())
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({'records': before['records'], 'duplicateGroups': len(before['duplicateGroups']),
                      'ambiguousGroups': sum(not item['repairable'] for item in before['duplicateGroups']),
                      'applied': args.apply, 'committedGroups': sum(item['committed'] for item in result['repairs'])}))
    if args.apply and (result['after']['duplicateGroups'] or result['after']['malformed']):
        raise SystemExit('Some records require another audit; no unguarded retries were attempted')


if __name__ == '__main__':
    main()
