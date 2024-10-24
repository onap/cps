package org.onap.cps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BubbleSortExample {

    public static void bubbleSort(int[] arr) {
        int n = arr.length;
        boolean swapped;

        // Outer loop for each pass
        for (int i = 0; i < n - 1; i++) {
            swapped = false;

            // Inner loop for each comparison within a pass
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    // Swap if the current element is greater than the next element
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                    swapped = true;
                }
            }

            // If no elements were swapped in the pass, the array is sorted
            if (!swapped) break;
        }
    }

    public static void main(String[] args) {
        int[] arr = {64, 34, 25, 12, 22, 11, 90};
        log.info("Original Array:");
        printArray(arr);

        bubbleSort(arr);

        log.info("Sorted Array:");
        printArray(arr);
    }

    // Helper method to print an array
    public static void printArray(int[] arr) {
        StringBuilder arrayString = new StringBuilder();
        for (int num : arr) {
            arrayString.append(num).append(" ");
        }
        log.info(arrayString.toString());
    }
}


