package com.matillion.techtest2025.controller;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.controller.response.ColumnProfileResponse;
import com.matillion.techtest2025.exception.BadRequestException;
import com.matillion.techtest2025.service.DataAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for data analysis endpoints.
 * <p>
 * Part 1: POST /api/analysis/ingestCsv – ingest and analyze CSV data
 * Part 2: GET  /api/analysis/{id}     – retrieve previous analysis
 *          DELETE /api/analysis/{id}  – delete analysis by ID
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class DataAnalysisController {

    private static final String FORBIDDEN_DRIVER = "Sonny Hayes";

    private final DataAnalysisService dataAnalysisService;

    /**
     * Ingests and analyzes a CSV payload.
     * Rejects empty input and any containing “Sonny Hayes”.
     */
    @PostMapping(
            value = "/ingestCsv",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public DataAnalysisResponse ingestAndAnalyzeCsv(@RequestBody String data) {
        if (data == null || data.isEmpty()) {
            throw new BadRequestException("Input CSV is empty.");
        }
        if (data.contains(FORBIDDEN_DRIVER)) {
            throw new BadRequestException("CSV data containing '" + FORBIDDEN_DRIVER + "' is not allowed");
        }
        return dataAnalysisService.analyzeCsvData(data);
    }

    /**
     * Retrieves a previously analyzed CSV by its ID.
     * Returns HTTP 404 if the record doesn’t exist.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public DataAnalysisResponse getAnalysisById(@PathVariable Long id) {
        return dataAnalysisService.getAnalysisById(id);
    }

    /**
     * Deletes an analysis and its column statistics by ID.
     * Returns HTTP 204 No Content on success, 404 if not found.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnalysisById(@PathVariable Long id) {
        dataAnalysisService.deleteAnalysisById(id);
    }

    /**
     * Returns inferred data type and numeric stats for each column
     * in a previously ingested CSV.
     *
     * Example: GET /api/analysis/5/profile
     */
    @GetMapping(
            value = "/{id}/profile",
            produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    )
    public ColumnProfileResponse getProfile(@PathVariable Long id) {
        return dataAnalysisService.getProfile(id);
    }
}

