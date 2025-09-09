#!/usr/bin/env python3
#
# Copyright 2023 Nordix Foundation.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import argparse
import csv


def load_metrics_table(filename):
    with open(filename) as tsvFile:
        csvReader = csv.DictReader(tsvFile, dialect="excel-tab")
        table = {}
        for source_row in csvReader:
            method, count, sum_time = source_row['Method'], source_row['Count'], source_row['Sum']
            table[method] = { 'Count': int(float(count)), 'Sum': float(sum_time) }
    return table


def save_metrics_table(table, filename):
    with open(filename, 'w', newline='') as outfile:
        csvWriter = csv.Writer(outfile, dialect="excel-tab")
        csvWriter.writerow(["Method", "Count", "Sum"])
        for method in table:
            count, sum_time = table[method]['Count'], table[method]['Sum']
            csvWriter.writerow([method, count, sum_time])


def subtract_metrics_tables(table, table_to_subtract):
    result = {}
    for method in table:
        result[method] = table[method]
    for method in table_to_subtract:
        result[method]['Count'] = result[method]['Count'] - table_to_subtract[method]['Count']
        result[method]['Sum'] = result[method]['Sum'] - table_to_subtract[method]['Sum']
    return filter_null_metrics_from_metrics_table(result)


def filter_null_metrics_from_metrics_table(table):
    result = {}
    for method in table:
        if table[method]['Count'] > 0:
            result[method] = table[method]
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-a', '--metrics-after',
                        required=True,
                        help='path to metrics table to subtract from',
                        dest='tsvpath_after',
                        type=str)
    parser.add_argument('-b', '--metrics-before',
                        required=True,
                        help='path to metrics table to subtract',
                        dest='tsvpath_before',
                        type=str)
    parser.add_argument('-o', '--output',
                        required=True,
                        help='path to output metrics table',
                        dest='outpath',
                        type=str)
    args = parser.parse_args()
    table1 = load_metrics_table(args.tsvpath_before)
    table2 = load_metrics_table(args.tsvpath_after)
    table_diff = subtract_metrics_tables(table2, table1)
    save_metrics_table(table_diff, args.outpath)
