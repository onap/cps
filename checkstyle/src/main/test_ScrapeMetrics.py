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
from ScrapeMetrics import (
    find_java_files,
    scrape_timed_metrics,
    scrape_gauge_metrics,
    scrape_count_metrics,
    scrape_all_metrics_from_file,
    write_metrics_to_file,
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

    def test_find_java_files(self):
        """Test finding .java files in a directory structure."""
        self._create_java_file("com/example/MyClass1.java", "")
        self._create_java_file("com/example/MyClass2.java", "")
        self._create_java_file("org/test/AnotherClass.java", "")
        self._create_java_file("textfile.txt", "")

        expected_files = {
            os.path.join(self.test_root, "com/example/MyClass1.java"),
            os.path.join(self.test_root, "com/example/MyClass2.java"),
            os.path.join(self.test_root, "org/test/AnotherClass.java"),
        }
        found_files = set(find_java_files(self.test_root))
        self.assertEqual(found_files, expected_files)

    def test_scrape_timed_metrics(self):
        """Test scraping @Timed metrics."""
        content = """
            @Timed(value = "method.execution.time", description = "Time taken to execute method")
            public void myMethod() {}

            @Timed(value="another.time", description="Another timed metric")
            public void anotherMethod() {}

            @NotTimed
            public void notTimedMethod() {}
        """
        expected_metrics = [
            '@Timed: value="method.execution.time", description="Time taken to execute method"',
            '@Timed: value="another.time", description="Another timed metric"',
        ]
        self.assertEqual(scrape_timed_metrics(content), expected_metrics)
        self.assertEqual(scrape_timed_metrics(""), [])
        self.assertEqual(scrape_timed_metrics("@Timed()"), [])

    def test_scrape_gauge_metrics(self):
        """Test scraping @CmHandleStateGaugeMetadata metrics."""
        content = """
            @CmHandleStateGaugeMetadata(value = "advisedCmHandlesCount", description = "Number of advised handles")
            public Gauge advisedCmHandles;

            @CmHandleStateGaugeMetadata(value="readyCmHandlesCount", description="Number of ready handles")
            public Gauge readyCmHandles;

            @NotAGauge
            public int notAGauge;
        """
        expected_metrics = [
            '@CmHandleStateGaugeMetadata: value="advisedCmHandlesCount", description="Number of advised handles"',
            '@CmHandleStateGaugeMetadata: value="readyCmHandlesCount", description="Number of ready handles"',
        ]
        self.assertEqual(scrape_gauge_metrics(content), expected_metrics)
        self.assertEqual(scrape_gauge_metrics(""), [])
        self.assertEqual(scrape_gauge_metrics("@CmHandleStateGaugeMetadata"), [])

    def test_scrape_count_metrics(self):
        """Test scraping @CountCmHandleSearchExecution metrics."""
        content = """
            @CountCmHandleSearchExecution(methodName = "searchCmHandles", interfaceName = "CPS-E-05")
            public List<User> searchCmHandles() {}

            @SomeOtherAnnotation
            public void doSomething() {}

            @CountCmHandleSearchExecution(methodName="searchCmHandleIds", interfaceName="CPS-NCMP-I-01")
            public List<Item> searchCmHandleIds() {}
        """
        expected_metrics = [
            '@CountCmHandleSearchExecution: methodName="searchCmHandles", interfaceName="CPS-E-05"',
            '@CountCmHandleSearchExecution: methodName="searchCmHandleIds", interfaceName="CPS-NCMP-I-01"',
        ]
        self.assertEqual(scrape_count_metrics(content), expected_metrics)
        self.assertEqual(scrape_count_metrics(""), [])
        self.assertEqual(scrape_count_metrics("@CountCmHandleSearchExecution"), [])

    def test_scrape_metrics_from_file(self):
        """Test scraping all metrics from a single Java file."""
        file_content = """
            package com.example;

            @Timed(value = "process.request.time", description = "Time to process request")
            public void processRequest() {}

            @CmHandleStateGaugeMetadata(value = "readyCmHandlesCount", description = "Number of ready handles")
            private int readyCmHandles;

            @CountCmHandleSearchExecution(methodName = "searchCmHandles", interfaceName = "CPS-E-05")
            public String searchCmHandles() {}
            
            @Timed(value="calculate.result.time", description="Time to calculate result")
            public int calculateResult() {}
        """
        test_file = self._create_java_file("com/example/MyService.java", file_content)
        expected_metrics = [
            '@Timed: value="process.request.time", description="Time to process request"',
            '@Timed: value="calculate.result.time", description="Time to calculate result"',
            '@CmHandleStateGaugeMetadata: value="readyCmHandlesCount", description="Number of ready handles"',
            '@CountCmHandleSearchExecution: methodName="searchCmHandles", interfaceName="CPS-E-05"'
        ]
        self.assertEqual(scrape_all_metrics_from_file(test_file), expected_metrics)
        self.assertEqual(scrape_all_metrics_from_file("non_existent_file.java"), [])

    def test_write_metrics_to_file_success(self):
        """Test writing metrics to a file successfully."""
        metrics_data = [
            '@Timed: value="test.metric", description="A test metric"',
            '@CmHandleStateGaugeMetadata: value="test.gauge", description="A test gauge"',
        ]
        output_file = os.path.join(self.test_root, "output.txt")
        write_metrics_to_file(metrics_data, output_file)
        self.assertTrue(os.path.exists(output_file))
        with open(output_file, 'r') as f:
            written_content = f.read().splitlines()
        self.assertEqual(written_content, metrics_data)

    def test_write_metrics_to_file_no_data(self):
        """Test writing metrics when no data is provided."""
        metrics_data = []
        output_file = os.path.join(self.test_root, "empty_output.txt")
        write_metrics_to_file(metrics_data, output_file)
        self.assertFalse(os.path.exists(output_file)) # File should not be created if no data

    def test_write_metrics_to_file_directory_creation(self):
        """Test that the output directory is created if it doesn't exist."""
        output_dir = os.path.join(self.test_root, "reports")
        output_file = os.path.join(output_dir, "metrics_report.txt")
        metrics_data = ['@Timed: value="another.test", description="Another test"']
        write_metrics_to_file(metrics_data, output_file)
        self.assertTrue(os.path.exists(output_dir))
        self.assertTrue(os.path.exists(output_file))

if __name__ == '__main__':
    # Ensure the script's directory is in the Python path for importing
    import sys
    script_dir = os.path.dirname(os.path.abspath(__file__))
    if script_dir not in sys.path:
        sys.path.insert(0, script_dir)
    unittest.main()