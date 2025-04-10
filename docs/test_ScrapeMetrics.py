#  ============LICENSE_START=======================================================
#  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
#  ================================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  SPDX-License-Identifier: Apache-2.0
#  ============LICENSE_END=========================================================

import unittest
import os
import tempfile
import time
from ScrapeMetrics import (
    scrape_all_metrics_from_file
)

class TestScrapeMetrics(unittest.TestCase):

    def setUp(self):
        """Set up temporary directory and files for testing."""
        self.temp_dir = tempfile.TemporaryDirectory()
        self.test_root = self.temp_dir.name

    def tearDown(self):
        """Clean up temporary directory and files."""
        self.temp_dir.cleanup()

    def _create_java_file(self, relative_path, content):
        """Helper function to create a test .java file."""
        file_path = os.path.join(self.test_root, relative_path)
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, 'w') as f:
            f.write(content)
        return file_path

    def test_scrape_metrics_from_file(self):
        """Test scraping all metrics from a single Java file."""
        file_content = """
            package com.example;

            @CountCmHandleSearchExecution(
                description = "A description does not fit the a single line")
            public void myMethod() {}

            @Timed(value="timed", description="A timed metric")
            public void anotherMethod() {}

            @TimedCustom(name="timedCustom", description="A custom timed metric")
            public void anotherMethod() {}

            @NotTimed
            public void notTimedMethod() {}
        """
        test_file = self._create_java_file("com/example/MyService.java", file_content)
        expected_metrics = [
            '"cm_handle_search_invocation_total","A description does not fit the a single line"',
            '"timed","A timed metric"',
            '"timedCustom","A custom timed metric"'
        ]
        result = scrape_all_metrics_from_file(test_file)
        self.assertEqual(len(result), 3)
        self.assertEqual(result, expected_metrics)

    def test_verify_metrics_file(self):
        """Test if metrics.csv was modified less than 1 minute ago and has 56 lines."""

        # Get the absolute path of the current directory.
        current_directory = os.path.dirname(os.path.abspath(__file__))

        metrics_file = os.path.join(current_directory, "csv/metrics.csv")

        # Check if the file exists
        self.assertTrue(os.path.exists(metrics_file), "metrics.csv does not exist.")

        # Check modification time
        modification_time_in_seconds = os.path.getmtime(metrics_file)
        time_difference_in_seconds = time.time() - modification_time_in_seconds
        self.assertLess(time_difference_in_seconds, 60, "metrics.csv was not modified in the last minute.")

        # Check number of lines
        with open(metrics_file, 'r') as f:
            lines = f.readlines()

        expected_number_of_metrics = 56
        expected_number_of_lines = expected_number_of_metrics + 1  # Header
        self.assertEqual(len(lines), expected_number_of_lines, f"metrics.csv does not have {expected_number_of_lines} lines.")

if __name__ == '__main__':
    # Ensure the script's directory is in the Python path for importing
    import sys
    script_dir = os.path.dirname(os.path.abspath(__file__))
    if script_dir not in sys.path:
        sys.path.insert(0, script_dir)
    unittest.main()