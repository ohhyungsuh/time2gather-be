"""Release requirements: free runner, required checks, no remote build, pinned actions."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]

class ReleaseWorkflowTest(unittest.TestCase):
    def workflow(self):
        path = ROOT / '.github/workflows/backend-release.yml'
        return path.read_text() if path.exists() else ''

    def test_release_uses_a_free_standard_x64_runner(self):
        self.assertIn('runs-on: ubuntu-24.04', self.workflow())

    def test_db_checks_and_native_smoke_precede_upload(self):
        text = self.workflow()
        required = ['./gradlew check processAot', 'docker build ', 'node scripts/sanity-api.mjs', 'docker push ']
        positions = [text.find(value) for value in required]
        self.assertTrue(all(value >= 0 for value in positions), positions)
        self.assertEqual(positions, sorted(positions))

    def test_release_never_submits_a_cloud_build_or_source_deploy(self):
        text = self.workflow()
        self.assertIn('python3 scripts/release_backend.py', text)
        self.assertNotRegex(text, r'gcloud\s+builds|--source|worker-pools|HIGHCPU|self-hosted')
        self.assertIn("github.ref == 'refs/heads/main'", text)

    def test_every_external_action_is_pinned_to_a_full_commit(self):
        actions = re.findall(r'uses:\s*([^\s]+)', self.workflow())
        self.assertGreater(len(actions), 3)
        for action in actions:
            self.assertRegex(action, r'^[\w-]+/[\w-]+@[a-f0-9]{40}$')

if __name__ == '__main__':
    unittest.main()
