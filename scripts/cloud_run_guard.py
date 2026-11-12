#!/usr/bin/env python3
"""Generate traffic tags or validate them before forwarding a gcloud command."""
import hashlib
import re
import subprocess
import sys

NAME = re.compile(r'[a-z](?:[a-z0-9-]*[a-z0-9])?\Z')
COMBINED_LIMIT = 46


def validate(service, tag):
    if not NAME.fullmatch(service) or len(service) > 49:
        raise ValueError('Invalid Cloud Run service name')
    limit = COMBINED_LIMIT - len(service)
    if not NAME.fullmatch(tag):
        raise ValueError('Tag must start with a lowercase letter and contain lowercase letters, digits or hyphens; no trailing hyphen')
    if len(tag) > limit:
        raise ValueError(f'Tag has {len(tag)} characters; service {service} allows at most {limit}. Generate a short tag with the tag command.')


def make_tag(service, label):
    tag = re.sub(r'[^a-z0-9]+', '-', label.lower()).strip('-')
    if not tag or not tag[0].isalpha():
        tag = 'r-' + tag
    limit = COMBINED_LIMIT - len(service)
    # Include a digest when normalizing or shortening so similar labels stay distinct.
    if tag != label or len(tag) > limit:
        if limit < 10:
            raise ValueError('Service name leaves insufficient room for a generated tag')
        digest = hashlib.sha256(label.encode()).hexdigest()[:8]
        tag = tag[:limit - 9].rstrip('-') + '-' + digest
    validate(service, tag)
    return tag


def validate_command(args):
    if args[:2] == ['run', 'deploy']:
        pos = 2
    elif args[:3] == ['run', 'services', 'update-traffic']:
        pos = 3
    else:
        raise ValueError('Supported commands: run deploy SERVICE or run services update-traffic SERVICE')
    if len(args) <= pos or args[pos].startswith('-'):
        raise ValueError('Place the service name immediately after the command')
    service = args[pos]
    i = pos + 1
    while i < len(args):
        flag, sep, value = args[i].partition('=')
        if flag in ('--tag', '--set-tags', '--update-tags'):
            if not sep:
                i += 1
                if i >= len(args) or args[i].startswith('--'):
                    raise ValueError(f'Missing value for {flag}')
                value = args[i]
            if flag == '--tag':
                validate(service, value)
            else:
                for item in value.split(','):
                    tag, eq, revision = item.partition('=')
                    if not eq or not revision:
                        raise ValueError(f'{flag} requires tag=revision pairs')
                    validate(service, tag)
        i += 1


def main(argv=None):
    args = list(sys.argv[1:] if argv is None else argv)
    try:
        if args[:1] == ['tag'] and len(args) == 3:
            print(make_tag(args[1], args[2]))
            return 0
        if args[:1] == ['check'] and len(args) == 3:
            validate(args[1], args[2])
            return 0
        dry_run = args[:1] == ['--check-only']
        if dry_run:
            args = args[1:]
        validate_command(args)
        if dry_run:
            return 0
        return subprocess.run(['gcloud', *args], check=False).returncode
    except ValueError as exc:
        print(f'Deployment blocked locally: {exc}', file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
