#!/usr/bin/env python3
"""Check service and Firestore coverage independently of controller/DTO coverage."""
import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET

MINIMUMS = {'services': {'LINE': 0.95, 'BRANCH': 0.80}, 'firestore': {'LINE': 0.85, 'BRANCH': 0.80}}


def layer(name):
    if '$' in name:
        return None
    if '/domain/' in name and name.endswith(('Service', 'BestSlotBuilder')):
        return 'services'
    if '/infra/' in name and name.rsplit('/', 1)[-1].startswith('Firestore') and name.endswith(('Repository', 'RepositorySupport', 'Store')):
        return 'firestore'
    return None


def review(root):
    totals = {name: {counter: {'covered': 0, 'missed': 0} for counter in limits} for name, limits in MINIMUMS.items()}
    for item in root.findall('./package/class'):
        name = layer(item.attrib['name'])
        if name is None:
            continue
        for counter in item.findall('counter'):
            kind = counter.attrib['type']
            if kind in totals[name]:
                for field in ['covered', 'missed']:
                    totals[name][kind][field] += int(counter.attrib[field])
    errors = []
    for name, counters in totals.items():
        for counter, values in counters.items():
            count = values['covered'] + values['missed']
            values['ratio'] = values['covered'] / count if count else 0
            values['minimum'] = MINIMUMS[name][counter]
            if not count or values['ratio'] < values['minimum']:
                errors.append(f"{name} {counter}: {values['ratio']:.2%} < {values['minimum']:.0%}")
    return {'layers': totals, 'failures': errors, 'passed': not errors}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = review(ET.parse(args.report).getroot())
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + '\n')
    for name, counters in result['layers'].items():
        print(name + ': ' + ', '.join(f"{kind} {values['ratio']:.2%}" for kind, values in counters.items()))
    if result['failures']:
        raise SystemExit('\n'.join(result['failures']))


if __name__ == '__main__':
    main()
