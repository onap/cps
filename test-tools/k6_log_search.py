#!/usr/bin/env python3
# ============LICENSE_START=======================================================
# Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
"""
Pull Jenkins console logs for a sequence of CI builds and count how many
contain a given search string.

Usage:
    python k6_log_search.py <from_build> <to_build> <search_string> [--job-url BASE_URL]

Example:
    python k6_log_search.py 9440 9441 "K P I"

The default job URL points at:
    https://jenkins.nordix.org/job/onap-kpi-regular-performance-test-k6

The console log for a build is fetched from:
    <job-url>/<build_number>/consoleText
"""

import argparse
import sys
import urllib.request
import urllib.error

DEFAULT_JOB_URL = "https://jenkins.nordix.org/job/onap-kpi-regular-performance-test-k6"


def fetch_console_text(job_url, build_number, timeout=60):
    """Fetch the console text for a single build. Returns the text, or None on failure."""
    url = f"{job_url.rstrip('/')}/{build_number}/consoleText"
    try:
        request = urllib.request.Request(url, headers={"User-Agent": "jenkins-log-search/1.0"})
        with urllib.request.urlopen(request, timeout=timeout) as response:
            charset = response.headers.get_content_charset() or "utf-8"
            return response.read().decode(charset, errors="replace")
    except urllib.error.HTTPError as error:
        print(f"  build {build_number}: HTTP error {error.code} ({error.reason})", file=sys.stderr)
    except urllib.error.URLError as error:
        print(f"  build {build_number}: connection error ({error.reason})", file=sys.stderr)
    except Exception as error:  # noqa: BLE001 - report and continue over the range
        print(f"  build {build_number}: unexpected error ({error})", file=sys.stderr)
    return None


def search_builds(job_url, from_build, to_build, search_string):
    """Iterate over the build range and report which builds contain the search string."""
    step = 1 if to_build >= from_build else -1
    total_checked = 0
    total_matched = 0
    total_occurrences = 0
    fetch_failures = 0
    matched_builds = []

    for build_number in range(from_build, to_build + step, step):
        console_text = fetch_console_text(job_url, build_number)
        if console_text is None:
            fetch_failures += 1
            continue

        total_checked += 1
        occurrences = console_text.count(search_string)
        build_url = f"{job_url.rstrip('/')}/{build_number}/consoleText"
        print(f"{build_url} {occurrences}")
        if occurrences > 0:
            total_matched += 1
            total_occurrences += occurrences
            matched_builds.append((build_number, occurrences))

    return total_checked, total_matched, total_occurrences, fetch_failures, matched_builds


def main():
    parser = argparse.ArgumentParser(
        description="Count how many Jenkins CI builds contain a given string in their console log."
    )
    parser.add_argument("from_build", type=int, help="First build number in the range (inclusive).")
    parser.add_argument("to_build", type=int, help="Last build number in the range (inclusive).")
    parser.add_argument("search_string", help="String to search for in each build's console log.")
    parser.add_argument(
        "--job-url",
        default=DEFAULT_JOB_URL,
        help=f"Base Jenkins job URL (default: {DEFAULT_JOB_URL}).",
    )
    args = parser.parse_args()

    print(
        f'Searching builds {args.from_build}..{args.to_build} for "{args.search_string}"'
        f" at {args.job_url}\n"
    )

    total_checked, total_matched, total_occurrences, fetch_failures, matched_builds = search_builds(
        args.job_url, args.from_build, args.to_build, args.search_string
    )

    print("\n--- Summary ---")
    print(f'Search string        : "{args.search_string}"')
    print(f"Builds checked       : {total_checked}")
    print(f"Builds with match    : {total_matched}")
    print(f"Total occurrences    : {total_occurrences}")
    if fetch_failures:
        print(f"Builds unreachable   : {fetch_failures}")
    if matched_builds:
        matched_list = ", ".join(f"{number} ({count})" for number, count in matched_builds)
        print(f"Matching builds      : {matched_list}")


if __name__ == "__main__":
    main()
