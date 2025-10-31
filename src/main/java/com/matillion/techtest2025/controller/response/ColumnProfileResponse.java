package com.matillion.techtest2025.controller.response;

import com.matillion.techtest2025.model.DataType;

import java.util.List;

/**
 * Response DTO for the profiling endpoint.
 * Describes inferred data characteristics for each column in an analysis.
 */
public record ColumnProfileResponse(
        Long analysisId,
        List<ColumnProfile> columns
) {
    public record ColumnProfile(
            String columnName,
            DataType dataType,
            Integer numericCount,
            Double min,
            Double max,
            Double mean,
            Double median,
            Double stddev
    ) { }
}
