/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BubbleSortExample {

    /**
     * Sorts the array using the Bubble Sort algorithm.
     *
     * @param arr the array to be sorted
     */
    public static void bubbleSort(final int[] arr) {
        final int n = arr.length;
        boolean swapped;

        // Outer loop for each pass
        for (int i = 0; i < n - 1; i++) {
            swapped = false;

            // Inner loop for each comparison within a pass
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    // Swap if the current element is greater than the next element
                    final int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }

            // If no elements were swapped in the pass, the array is sorted
            if (!swapped) {
                break;
            }
        }
    }

    /**
     * The main method which initiates the program.
     *
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        final int[] arr = {64, 34, 25, 12, 22, 11, 90};
        log.info("Original Array:");
        printArray(arr);

        bubbleSort(arr);

        log.info("Sorted Array:");
        printArray(arr);
    }

    /**
     * Helper method to print an array.
     *
     * @param arr the array to print
     */
    public static void printArray(final int[] arr) {
        final StringBuilder arrayString = new StringBuilder();
        for (final int num : arr) {
            arrayString.append(num).append(" ");
        }
        log.info(arrayString.toString());
    }
}


