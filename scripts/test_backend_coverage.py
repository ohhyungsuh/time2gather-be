import unittest
import xml.etree.ElementTree as ET

from check_backend_coverage import review


def report(service=(95, 80), database=(85, 80)):
    root = ET.Element('report')
    package = ET.SubElement(root, 'package')
    for name, values in [('com/cover/time2gather/domain/meeting/service/MeetingService', service),
                         ('com/cover/time2gather/infra/firestore/FirestoreUserRepository', database),
                         ('com/cover/time2gather/api/ExampleController', (100, 100))]:
        item = ET.SubElement(package, 'class', name=name)
        for kind, covered in zip(['LINE', 'BRANCH'], values):
            ET.SubElement(item, 'counter', type=kind, covered=str(covered), missed=str(100-covered))
    return root


class CoverageGuardTest(unittest.TestCase):
    def test_boundary_values_pass_without_rounding_up_a_failure(self):
        self.assertTrue(review(report())['passed'])
        self.assertFalse(review(report(service=(94, 80)))['passed'])

    def test_high_controller_coverage_cannot_hide_low_database_or_branch_coverage(self):
        result = review(report(database=(20, 100)))
        self.assertFalse(result['passed'])
        self.assertTrue(any('firestore LINE' in item for item in result['failures']))
        self.assertFalse(review(report(service=(100, 0)))['passed'])

    def test_an_empty_or_wrong_scope_report_cannot_silently_pass(self):
        result = review(ET.fromstring('<report/>'))
        self.assertFalse(result['passed'])
        self.assertEqual(len(result['failures']), 4)


if __name__ == '__main__':
    unittest.main()
