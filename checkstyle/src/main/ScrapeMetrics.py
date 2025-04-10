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

def scrape_timed_metrics(file_content):
    """
    Extracts @Timed metrics (value and description) from the given Java file content.

    Args:
        file_content (str): The content of a Java file.

    Returns:
        list: A list of formatted @Timed metric strings.
    """
    timed_regex = re.compile(r'@Timed\(value\s*=\s*"([^"]*)",\s*description\s*=\s*"([^"]*)"\)')
    timed_metrics = []
    timed_matches = timed_regex.findall(file_content)
    for value, description in timed_matches:
        timed_metrics.append(f"@Timed: value=\"{value}\", description=\"{description}\"")
    return timed_metrics

def scrape_gauge_metrics(file_content):
    """
    Extracts @CmHandleStateGaugeMetadata metrics (value and description)
    from the given Java file content.

    Args:
        file_content (str): The content of a Java file.

    Returns:
        list: A list of formatted @CmHandleStateGaugeMetadata metric strings.
    """
    gauge_regex = re.compile(r'@CmHandleStateGaugeMetadata\(value\s*=\s*"([^"]*)",\s*description\s*=\s*"([^"]*)"\)')
    gauge_metrics = []
    gauge_matches = gauge_regex.findall(file_content)
    for value, description in gauge_matches:
        gauge_metrics.append(f"@CmHandleStateGaugeMetadata: value=\"{value}\", description=\"{description}\"")
    return gauge_metrics

def scrape_count_metrics(file_content):
    """
    Extracts @CountCmHandleSearchExecution metrics (methodName and interfaceName)
    from the given Java file content.

    Args:
        file_content (str): The content of a Java file.

    Returns:
        list: A list containing the formatted @CountCmHandleSearchExecution metric string,
              or an empty list if no match is found.
    """
    count_regex = re.compile(r'@CountCmHandleSearchExecution\(methodName\s*=\s*"([^"]*)",\s*interfaceName\s*=\s*"([^"]*)"\)')
    count_metrics = []
    count_matches = count_regex.findall(file_content)
    for methodName, interfaceName in count_matches:
        count_metrics.append(f"@CountCmHandleSearchExecution: methodName=\"{methodName}\", interfaceName=\"{interfaceName}\"")
    return count_metrics

def scrape_all_metrics_from_file(file_path):
    """
    Scrapes all defined metrics from a single Java file.

    Args:
        file_path (str): The path to the Java file.

    Returns:
        list: A list of all extracted metric strings from the file.
    """
    all_metrics = []
    try:
        with open(file_path, 'r') as f:
            java_class_content = f.read()
            all_metrics.extend(scrape_timed_metrics(java_class_content))
            all_metrics.extend(scrape_gauge_metrics(java_class_content))
            all_metrics.extend(scrape_count_metrics(java_class_content))
    except FileNotFoundError:
        print(f"Error: File not found: {file_path}")
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
        print(f"Scraped metrics written to: {output_file}")
    else:
        print("No matching metrics found.")

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
    cps_root_directory = os.path.abspath(os.path.join(current_directory, "../../.."))

    # Define the location for the output file, and ensure its directory exists.
    output_file = os.path.join(cps_root_directory, "docs", "metrics.txt")

    # Search and scrape the metrics.
    search_metrics_and_scrape(cps_root_directory, output_file)

    # Exit with success code 0
    sys.exit(0)