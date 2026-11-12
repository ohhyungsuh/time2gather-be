import importlib.util
import os
from pathlib import Path
import unittest
import urllib.error
import uuid

spec = importlib.util.spec_from_file_location('repair', Path(__file__).with_name('repair_vote_records.py'))
repair = importlib.util.module_from_spec(spec)
spec.loader.exec_module(repair)


def fields(identity, updated, slots=(9,), none=False):
    return {'id': {'integerValue': str(identity)}, 'meetingId': {'integerValue': '1'},
            'userId': {'integerValue': '2'}, 'updatedAt': {'stringValue': updated},
            'createdAt': {'stringValue': '2026-01-01T00:00:00'},
            'selectionType': {'stringValue': 'TIME'}, 'intervalMinutes': {'integerValue': '60'},
            'selections': {'mapValue': {'fields': {} if none else {'2027-01-10': {'arrayValue': {
                'values': [{'integerValue': str(value)} for value in slots]}}}}},
            **({'unavailableDates': {'arrayValue': {'values': [{'stringValue': '2027-01-10'}]}}} if none else {})}


class SelectionPolicyTest(unittest.TestCase):
    def test_equal_modification_times_with_conflicting_choices_are_not_guessed(self):
        values = [{'fields': fields(1, '2026-11-01T00:00:00', (9,))},
                  {'fields': fields(2, '2026-11-01T00:00:00', (12,))}]
        with self.assertRaisesRegex(ValueError, 'Conflicting'):
            repair.latest(values)

    def test_nanosecond_modifications_are_compared_without_truncating_precision(self):
        older = {'fields': fields(99, '2026-11-01T00:00:00.123456781', (9,))}
        newer = {'fields': fields(1, '2026-11-01T00:00:00.123456782', (12,))}
        self.assertIs(repair.latest([older, newer]), newer)


@unittest.skipUnless(os.environ.get('FIRESTORE_EMULATOR_HOST', '').startswith(('127.0.0.1:', 'localhost:')), 'Local emulator required')
class RepairEmulatorTest(unittest.TestCase):
    def setUp(self):
        self.client = repair.Client('demo-repair-' + uuid.uuid4().hex[:10], os.environ['FIRESTORE_EMULATOR_HOST'])

    def seed(self, identity, updated, slots=(9,), none=False):
        name = self.client.path + '/meetingUserSelections/' + str(identity)
        self.client.request(':commit', 'POST', {'writes': [{'update': {
            'name': name, 'fields': fields(identity, updated, slots, none)}}]})

    def test_repair_keeps_latest_complete_record_and_original_timestamps(self):
        self.seed(99, '2026-11-01T00:00:00', (9,))
        self.seed(1, '2026-11-02T00:00:00', (12,))
        before = self.client.votes()
        expected = repair.latest(before)['fields']
        repair.repair(self.client, (1, 2), before)
        after = self.client.votes()
        self.assertEqual(len(after), 1)
        self.assertTrue(after[0]['name'].endswith('/selection-1-2'))
        self.assertEqual(after[0]['fields'], expected)
        self.assertFalse(repair.audit(after)[0]['duplicateGroups'])

    def test_latest_none_response_does_not_resurrect_old_available_times(self):
        self.seed(99, '2026-11-01T00:00:00')
        self.seed(1, '2026-11-02T00:00:00', none=True)
        repair.repair(self.client, (1, 2), self.client.votes())
        saved = self.client.votes()[0]['fields']
        self.assertEqual(saved['selections'].get('mapValue', {}).get('fields', {}), {})
        self.assertIn('unavailableDates', saved)

    def test_concurrent_edit_rejects_entire_commit_without_deleting_any_record(self):
        self.seed(99, '2026-11-01T00:00:00')
        self.seed(1, '2026-11-02T00:00:00', (12,))
        snapshot = self.client.votes()
        self.seed(99, '2026-11-03T00:00:00', (10,))
        with self.assertRaises(urllib.error.HTTPError):
            repair.repair(self.client, (1, 2), snapshot)
        after = self.client.votes()
        self.assertEqual(len(after), 2)
        self.assertEqual(repair.latest(after)['fields']['updatedAt']['stringValue'], '2026-11-03T00:00:00')


if __name__ == '__main__':
    unittest.main()
