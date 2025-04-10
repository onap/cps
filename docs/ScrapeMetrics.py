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

import os
import re
import sys

def find_java_files(root_dir):
    """
    Recursively finds all .java files within the given root directory.

    Args:
        root_dir (str): The root directory to search.

    Returns:
        list: A list of absolute paths to .java files.
    """
    java_files = []
    for root, _, files in os.walk(root_dir):
        for file in files:
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))
    return java_files

def scrape_metrics(file_content):
    """
    Matches @CountCmHandleSearchExecution, @Timed, and @TimedCustom and
    Extracts name, value and description from the given Java file content.
    The regex will also handle the new line if the annotation would not fit in a single line.

    Args:
        file_content (str): The content of a Java file.

    Returns:
        list: A list of formatted metric strings.
    """
    pattern_regex = re.compile(r'@(CountCmHandleSearchExecution|Timed|TimedCustom)\((?:name\s*=\s*"(.*?)",?|value\s*=\s*"(.*?)",?)?.*?description\s*=\s*"(.*?)"', re.DOTALL)
    all_metrics = []
    matches = pattern_regex.findall(file_content)
    for match in matches:
        count_metric = match[0]
        if count_metric == "CountCmHandleSearchExecution":
            name = "cm_handle_search_invocation_total"
        else:
            name = match[1]
        value = match[2]
        description = match[3]
        all_metrics.append(f'"{name or value}","{description}"')
    return all_metrics

def scrape_all_metrics_from_file(file_path):
    """
    Scrapes all defined metrics from a single Java file.

    Args:
        file_path (str): The path to the Java file.

    Returns:
        list: A list of all extracted metric strings from the file.
    """
    all_metrics = []
    with open(file_path, 'r') as f:
        java_class_content = f.read()
        all_metrics.extend(scrape_metrics(java_class_content))
    return all_metrics

def write_metrics_to_file(metrics_data, output_file):
    """
    Writes the extracted metrics data to the specified output file.

    Args:
        metrics_data (list): A list of metric strings to write.
        output_file (str): The path to the output file.
    """
    if metrics_data:
        os.makedirs(os.path.dirname(output_file), exist_ok=True)
        with open(output_file, 'w') as outfile:
            for metric in metrics_data:
                outfile.write(metric + '\n')
        print(f"{len(metrics_data)} scraped metrics written to: {output_file}")

def search_metrics_and_scrape(root_dir, output_file):
    """
    Orchestrates the search and scraping of metrics from Java files.

    Args:
        root_dir (str): The root directory to search for .java files.
        output_file (str): The text file to store the metrics.
    """
    java_files = find_java_files(root_dir)
    all_scraped_metrics = []
    for java_file in java_files:
        metrics = scrape_all_metrics_from_file(java_file)
        all_scraped_metrics.extend(metrics)
    write_metrics_to_file(all_scraped_metrics, output_file)

if __name__ == "__main__":
    # Get the absolute path of the current directory.
    current_directory = os.path.dirname(os.path.abspath(__file__))

    # Get the absolute path of the cps root directory.
    cps_root_directory = os.path.abspath(os.path.join(current_directory, ".."))

    # Define the location for the output file, and ensure its directory exists.
    output_file = os.path.join(current_directory, "csv", "metrics.csv")

    # Search and scrape the metrics.
    search_metrics_and_scrape(cps_root_directory, output_file)

    # Exit with success code 0
    sys.exit(0)