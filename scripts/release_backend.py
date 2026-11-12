#!/usr/bin/env python3
"""Deploy a prebuilt digest, preserving runtime settings and concurrent releases."""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import urllib.error
import urllib.request

PROJECT = 'time2gather-prod-2026'
REGION = 'asia-northeast3'
SERVICE = 'time2gather-api'
IMAGE = re.compile(r'asia-northeast3-docker\.pkg\.dev/time2gather-prod-2026/time2gather/api@sha256:[a-f0-9]{64}\Z')
LOCALES = ['en', 'zh-CN', 'hi', 'es', 'ar', 'fr', 'bn', 'pt', 'ru', 'id', 'ur', 'de', 'ja', 'pcm', 'ar-EG', 'mr', 'vi', 'te', 'ha', 'tr', 'ko', 'th']
GUARD = Path(__file__).with_name('cloud_run_guard.py')


def command(args, *, parse=False, guarded=False):
    prefix = [sys.executable, str(GUARD)] if guarded else ['gcloud']
    result = subprocess.run(prefix + args + ['--project', PROJECT, '--region', REGION, '--quiet'],
                            check=True, text=True, stdout=subprocess.PIPE)
    return json.loads(result.stdout) if parse else result.stdout


def service():
    return command(['run', 'services', 'describe', SERVICE, '--format=json'], parse=True)


def revision(name):
    if not re.fullmatch(r'time2gather-api-[a-z0-9-]+', name):
        raise ValueError('Invalid revision')
    return command(['run', 'revisions', 'describe', name, '--format=json'], parse=True)


def traffic(value):
    return {x['revisionName']: x['percent'] for x in value['status']['traffic'] if x.get('percent', 0) > 0}


def runtime_hash(value):
    spec = copy.deepcopy(value['spec']['template']['spec'])
    for container in spec['containers']:
        container.pop('image', None)
    # Deployment-generated client/operation annotations change on every release.
    template_annotations = value['spec']['template'].get('metadata', {}).get('annotations', {})
    annotations = {k: v for k, v in template_annotations.items() if k not in {
        'run.googleapis.com/client-name', 'run.googleapis.com/client-version', 'run.googleapis.com/operation-id'}}
    top_annotations = value['metadata'].get('annotations', {})
    top = {k: v for k, v in top_annotations.items() if k in {
        'run.googleapis.com/minScale', 'run.googleapis.com/maxScale', 'run.googleapis.com/ingress',
        'run.googleapis.com/invoker-iam-disabled', 'run.googleapis.com/default-url-disabled'}}
    return hashlib.sha256(json.dumps([spec, annotations, top], sort_keys=True).encode()).hexdigest()


def validate_runtime(value):
    limits = value['spec']['template']['spec']['containers'][0]['resources']['limits']
    if limits != {'cpu': '1', 'memory': '512Mi'}:
        raise ValueError('Expected the existing 1 CPU / 512Mi API runtime')
    if value['metadata'].get('annotations', {}).get('run.googleapis.com/minScale') != '1':
        raise ValueError('API service-level minimum instances must remain 1')


def validate_image(image):
    if not IMAGE.fullmatch(image):
        raise ValueError('Release needs an immutable API image digest in the existing project/repository')


def validate_state(current, baseline, expected_traffic):
    validate_runtime(current)
    if runtime_hash(current) != baseline['runtimeHash']:
        raise ValueError('Runtime settings changed; refusing to overwrite a concurrent change')
    if traffic(current) != expected_traffic:
        raise ValueError('Production traffic changed; refusing a stale deployment')


def save(path, value):
    Path(path).write_text(json.dumps(value, indent=2) + '\n')


def capture(path):
    current = service()
    validate_runtime(current)
    active = traffic(current)
    if len(active) != 1 or list(active.values()) != [100]:
        raise ValueError('Wait until the existing rollout has one revision at 100%')
    name = next(iter(active))
    image = revision(name)['spec']['containers'][0]['image']
    validate_image(image)
    result = {'version': 1, 'runtimeHash': runtime_hash(current), 'baseTraffic': active,
              'baseRevision': name, 'baseImage': image, 'expectedTraffic': active,
              'serviceUrl': current['status']['url']}
    save(path, result)
    print('Captured baseline:', name, image)


def fetch(url, locale='en'):
    req = urllib.request.Request(url, headers={'Accept-Language': locale, 'User-Agent': 'Time2Gather-release-check/1'})
    try:
        with urllib.request.urlopen(req, timeout=35) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


def smoke(base):
    if not base.startswith('https://') or not ('.run.app' in base or base.rstrip('/') == 'https://time2gather.org'):
        raise ValueError('Unexpected verification origin')
    status, body = fetch(base.rstrip('/') + '/health')
    if status != 200 or body.get('status') != 'UP':
        raise ValueError('Health check failed')
    for locale in LOCALES:
        status, body = fetch(base.rstrip('/') + '/api/v1/meetings/mtg_release-missing', locale)
        message = body.get('message', '')
        if status != 404 or not message or message.startswith('error.'):
            raise ValueError('Localized read-only API check failed: ' + locale)
    print('Read-only health and 22 locale checks passed:', base)


def candidate(path, image, suffix):
    validate_image(image)
    if not re.fullmatch(r'gha-[0-9]+-[0-9]+', suffix):
        raise ValueError('Revision suffix must identify the GitHub run and attempt')
    baseline = json.loads(Path(path).read_text())
    validate_state(service(), baseline, baseline['baseTraffic'])
    # Check the active image, not the latest no-traffic revision's template image.
    if revision(baseline['baseRevision'])['spec']['containers'][0]['image'] != baseline['baseImage']:
        raise ValueError('The baseline image changed')
    tag = subprocess.check_output([sys.executable, str(GUARD), 'tag', SERVICE, suffix], text=True).strip()
    command(['run', 'deploy', SERVICE, '--image', image, '--no-traffic', '--tag', tag,
             '--revision-suffix', suffix], guarded=True)
    current = service()
    validate_state(current, baseline, baseline['baseTraffic'])
    entries = [x for x in current['status']['traffic'] if x.get('tag') == tag]
    if len(entries) != 1:
        raise ValueError('Candidate tag was not published exactly once')
    entry = entries[0]
    actual = revision(entry['revisionName'])['status']['imageDigest']
    if actual != image:
        raise ValueError('Candidate digest does not match the tested image')
    baseline.update(candidateRevision=entry['revisionName'], candidateUrl=entry['url'], candidateImage=image, tag=tag)
    save(path, baseline)
    smoke(entry['url'])


def promote(path):
    baseline = json.loads(Path(path).read_text())
    expected = baseline['baseTraffic']
    target = baseline['candidateRevision']
    validate_image(baseline['candidateImage'])
    if revision(target)['status']['imageDigest'] != baseline['candidateImage']:
        raise ValueError('Candidate digest changed')
    smoke(baseline['candidateUrl'])
    attempted = expected
    try:
        for percent in [10, 50, 100]:
            validate_state(service(), baseline, expected)
            desired = {target: percent}
            if percent < 100:
                desired[baseline['baseRevision']] = 100 - percent
            allocations = ','.join(f'{key}={value}' for key, value in desired.items())
            attempted = desired
            command(['run', 'services', 'update-traffic', SERVICE, '--to-revisions', allocations], guarded=True)
            expected = desired
            baseline['expectedTraffic'] = desired
            save(path, baseline)
            validate_state(service(), baseline, desired)
            smoke(baseline['serviceUrl'])
        # Hosting health is routed to the web service, so API-only checks below.
        for locale in LOCALES:
            status, body = fetch('https://time2gather.org/api/v1/meetings/mtg_release-missing', locale)
            if status != 404 or not body.get('message') or body['message'].startswith('error.'):
                raise ValueError('Public domain API verification failed: ' + locale)
        baseline['completed'] = True
        save(path, baseline)
        print('Promoted verified digest to 100%:', baseline['candidateImage'])
    except Exception:
        current = service()
        # Never roll back over a release made by someone else.
        if traffic(current) in [expected, attempted] and runtime_hash(current) == baseline['runtimeHash']:
            allocations = ','.join(f'{key}={value}' for key, value in baseline['baseTraffic'].items())
            command(['run', 'services', 'update-traffic', SERVICE, '--to-revisions', allocations], guarded=True)
            baseline['rolledBack'] = True
            save(path, baseline)
        raise


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='action', required=True)
    for name in ['capture', 'candidate', 'promote']:
        p = sub.add_parser(name)
        p.add_argument('--state', default='release-state.json')
        if name == 'candidate':
            p.add_argument('--image', required=True)
            p.add_argument('--suffix', required=True)
    args = parser.parse_args()
    if args.action == 'capture': capture(args.state)
    elif args.action == 'candidate': candidate(args.state, args.image, args.suffix)
    else: promote(args.state)

if __name__ == '__main__':
    main()
