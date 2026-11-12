import unittest
from unittest.mock import patch
from cloud_run_guard import main, make_tag, validate

SERVICE = 'time2gather-web'


class TrafficTagTest(unittest.TestCase):
    def test_boundary(self):
        validate(SERVICE, 'a' * 31)
        with self.assertRaises(ValueError):
            validate(SERVICE, 'a' * 32)

    def test_past_incidents_blocked_before_cloud_call(self):
        for tag in ['confirmation-recommendations-1103', 'calendar-location-20261102-check']:
            for flag in [['--tag', tag], ['--tag=' + tag]]:
                with self.subTest(tag=tag, flag=flag), patch('cloud_run_guard.subprocess.run') as run:
                    self.assertEqual(main(['run', 'deploy', SERVICE, *flag]), 2)
                    run.assert_not_called()

    def test_generated_tag_is_stable_and_distinct(self):
        for label in ['confirmation-recommendations-1103', 'calendar-location-20261102-check', '한글 배포', '20261103', 'UPPER Case']:
            tag = make_tag(SERVICE, label)
            validate(SERVICE, tag)
            self.assertEqual(tag, make_tag(SERVICE, label))
        self.assertNotEqual(make_tag(SERVICE, 'a' * 50 + 'x'), make_tag(SERVICE, 'a' * 50 + 'y'))
        self.assertEqual(make_tag(SERVICE, 'confirm-rec-1103'), 'confirm-rec-1103')

    def test_forwarding_preserves_deploy_options_and_exit_code(self):
        args = ['run', 'deploy', SERVICE, '--tag', 'confirm-rec-1103', '--no-traffic', '--image', 'image', '--project', 'project']
        with patch('cloud_run_guard.subprocess.run') as run:
            run.return_value.returncode = 7
            self.assertEqual(main(args), 7)
            run.assert_called_once_with(['gcloud', *args], check=False)

    def test_traffic_update_and_check_only(self):
        for flag in ['--set-tags', '--update-tags']:
            with patch('cloud_run_guard.subprocess.run') as run:
                self.assertEqual(main(['run', 'services', 'update-traffic', SERVICE, flag, 'ok=rev,' + 'a' * 32 + '=rev']), 2)
                run.assert_not_called()
                self.assertEqual(main(['--check-only', 'run', 'services', 'update-traffic', SERVICE, flag + '=ok=rev']), 0)
                run.assert_not_called()

    def test_invalid_names_and_missing_values(self):
        for tag in ['', 'Upper', '-start', 'end-', 'a_b', '1start']:
            with self.assertRaises(ValueError):
                validate(SERVICE, tag)
        with patch('cloud_run_guard.subprocess.run') as run:
            self.assertEqual(main(['run', 'deploy', SERVICE, '--tag']), 2)
            run.assert_not_called()


if __name__ == '__main__':
    unittest.main()
