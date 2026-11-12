import importlib.util
import json
from pathlib import Path
import tempfile
import subprocess
import sys
import unittest

SPEC = importlib.util.spec_from_file_location("guard", Path(__file__).with_name("cloud_build_guard.py"))
guard = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(guard)


class CloudBuildGuardTest(unittest.TestCase):
    def test_ordinary_release_rejects_paid_pool_and_high_cpu(self):
        for options in ({"pool": {"name": "private-test-pool"}}, {"machineType": "E2_HIGHCPU_8"}):
            with self.subTest(options=options), self.assertRaises(ValueError):
                guard.validate_config({"steps": [{"name": "docker"}], "options": options}, "release")

    def test_native_and_runtime_validation_allow_paid_capacity_with_timeout_bound(self):
        config = {"steps": [{"name": "docker"}], "options": {"pool": {"name": "native-pool"}}, "timeout": "2400s"}
        for purpose in ("native", "runtime-validation"):
            self.assertTrue(guard.validate_config(config, purpose)["paidCapacity"])
        config["timeout"] = "2401s"
        with self.assertRaises(ValueError):
            guard.validate_config(config, "native")

    def test_hash_tracks_source_but_ignores_local_results(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "src").mkdir()
            code = root / "src/app.java"
            code.write_text("first")
            first = guard.source_hash(root)
            (root / "work").mkdir()
            (root / "work/preflight.json").write_text("result")
            self.assertEqual(first, guard.source_hash(root))
            code.write_text("changed")
            self.assertNotEqual(first, guard.source_hash(root))

    def test_submit_rejects_source_changed_after_checks_without_calling_gcloud(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "src").mkdir()
            code = root / "src/app.java"
            code.write_text("checked")
            proof = root / "proof.json"
            proof.write_text(json.dumps({"version": 1, "sourceSha256": guard.source_hash(root),
                                         "checks": [{"command": "tests", "exitCode": 0}]}))
            config = root / "build.json"
            config.write_text(json.dumps({"steps": [{"name": "docker"}]}))
            code.write_text("unchecked change")
            result = subprocess.run([sys.executable, str(Path(guard.__file__).resolve()), "submit",
                "--source", str(root), "--config", str(config), "--preflight", str(proof),
                "--purpose", "release", "--project", "demo-test", "--region", "asia-northeast3",
                "--check-only"], capture_output=True, text=True)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("current preflight checks", result.stderr)

    def test_failed_preflight_removes_previous_success_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            proof = root / "proof.json"
            proof.write_text("stale success")
            result = subprocess.run([sys.executable, str(Path(guard.__file__).resolve()), "preflight",
                "--source", str(root), "--output", str(proof),
                "--check", f'{sys.executable} -c "raise SystemExit(7)"'], capture_output=True)
            self.assertEqual(result.returncode, 7)
            self.assertFalse(proof.exists())


if __name__ == "__main__":
    unittest.main()
