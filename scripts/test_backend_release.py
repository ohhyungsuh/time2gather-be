"""Reject stale rollouts and preserve runtime configuration on deploy failures."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import release_backend as release

IMAGE = release.PROJECT
DIGEST = release.IMAGE.pattern.split('@')[0].replace('\\.', '.') + '@sha256:' + 'a' * 64

def fixture():
    return {'metadata': {'annotations': {'run.googleapis.com/minScale': '1', 'run.googleapis.com/maxScale': '3'}},
            'spec': {'template': {'metadata': {'annotations': {'run.googleapis.com/cpu-throttling': 'true'}},
                                  'spec': {'serviceAccountName': 'runtime@project.iam.gserviceaccount.com',
                                           'containers': [{'image': 'old', 'resources': {'limits': {'cpu': '1', 'memory': '512Mi'}},
                                                           'env': [{'name': 'JWT_SECRET', 'valueFrom': {'secretKeyRef': {'name': 'jwt', 'key': 'latest'}}}]}]}}},
            'status': {'traffic': [{'revisionName': 'time2gather-api-old', 'percent': 100}], 'url': 'https://api.run.app'}}

class BackendReleaseTest(unittest.TestCase):
    def test_mutable_and_foreign_images_are_rejected(self):
        valid = 'asia-northeast3-docker.pkg.dev/time2gather-prod-2026/time2gather/api@sha256:' + 'a' * 64
        release.validate_image(valid)
        for image in [valid.replace('@sha256:' + 'a' * 64, ':latest'), valid.replace('/api@', '/web@'), valid.replace('prod-2026', 'other-project')]:
            with self.subTest(image=image), self.assertRaises(ValueError):
                release.validate_image(image)

    def test_runtime_fingerprint_ignores_only_the_image_and_generated_metadata(self):
        old = fixture(); new = copy.deepcopy(old)
        new['spec']['template']['spec']['containers'][0]['image'] = 'new'
        new['spec']['template']['metadata']['annotations']['run.googleapis.com/operation-id'] = 'new-operation'
        self.assertEqual(release.runtime_hash(old), release.runtime_hash(new))
        for mutation in ['memory', 'secret', 'serviceAccount', 'minimum']:
            new = copy.deepcopy(old)
            if mutation == 'memory': new['spec']['template']['spec']['containers'][0]['resources']['limits']['memory'] = '1Gi'
            if mutation == 'secret': new['spec']['template']['spec']['containers'][0]['env'][0]['valueFrom']['secretKeyRef']['name'] = 'other'
            if mutation == 'serviceAccount': new['spec']['template']['spec']['serviceAccountName'] = 'other'
            if mutation == 'minimum': new['metadata']['annotations']['run.googleapis.com/minScale'] = '0'
            self.assertNotEqual(release.runtime_hash(old), release.runtime_hash(new), mutation)

    def test_a_concurrent_production_release_is_never_overwritten(self):
        old = fixture(); baseline = {'runtimeHash': release.runtime_hash(old)}
        changed = copy.deepcopy(old); changed['status']['traffic'][0]['revisionName'] = 'time2gather-api-someone-else'
        with self.assertRaisesRegex(ValueError, 'stale deployment'):
            release.validate_state(changed, baseline, release.traffic(old))

    def test_unexpected_runtime_settings_block_deployment(self):
        current = fixture(); baseline = {'runtimeHash': release.runtime_hash(current)}
        current['spec']['template']['spec']['serviceAccountName'] = 'other'
        with self.assertRaisesRegex(ValueError, 'Runtime settings changed'):
            release.validate_state(current, baseline, release.traffic(current))

    def test_failed_canary_restores_only_the_traffic_owned_by_this_rollout(self):
        for concurrent in [False, True]:
            with self.subTest(concurrent=concurrent), tempfile.TemporaryDirectory() as folder:
                old = fixture(); canary = copy.deepcopy(old)
                canary['status']['traffic'] = [{'revisionName': 'time2gather-api-new', 'percent': 10}, {'revisionName': 'time2gather-api-old', 'percent': 90}]
                after_failure = copy.deepcopy(canary)
                if concurrent: after_failure['status']['traffic'] = [{'revisionName': 'time2gather-api-other', 'percent': 100}]
                image = 'asia-northeast3-docker.pkg.dev/time2gather-prod-2026/time2gather/api@sha256:' + 'a' * 64
                state = {'runtimeHash': release.runtime_hash(old), 'baseTraffic': release.traffic(old), 'baseRevision': 'time2gather-api-old',
                         'candidateRevision': 'time2gather-api-new', 'candidateImage': image, 'candidateUrl': 'https://new.run.app', 'serviceUrl': 'https://api.run.app'}
                path = Path(folder) / 'state.json'; path.write_text(json.dumps(state))
                with patch.object(release, 'revision', return_value={'status': {'imageDigest': image}}), \
                     patch.object(release, 'service', side_effect=[old, canary, after_failure]), \
                     patch.object(release, 'smoke', side_effect=[None, ValueError('bad health')]), \
                     patch.object(release, 'command') as command:
                    with self.assertRaisesRegex(ValueError, 'bad health'): release.promote(path)
                    self.assertEqual(command.call_count, 1 if concurrent else 2)
                    saved = json.loads(path.read_text())
                    self.assertEqual(saved.get('rolledBack', False), not concurrent)

if __name__ == '__main__': unittest.main()
