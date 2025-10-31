package com.matillion.techtest2025.model;

/**
 * Enumeration representing the inferred data type of a CSV column.
 * <p>
 * Used during column profiling in Part 3 to classify each column
 * as one of the basic data types recognised by the system.
 */
public enum DataType {
    /**
     * Column values are free-form text or mixed data.
     */
    STRING,

    /**
     * Column values are whole numbers (no decimals).
     */
    INTEGER,

    /**
     * Column values contain decimal points or fractional numbers.
     */
    DECIMAL,

    /**
     * Column values are true/false or yes/no (case-insensitive).
     */
    BOOLEAN
}
