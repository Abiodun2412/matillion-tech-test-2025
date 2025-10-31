package com.matillion.techtest2025.service;

import com.matillion.techtest2025.controller.response.DataAnalysisResponse;
import com.matillion.techtest2025.controller.response.ColumnProfileResponse;
import com.matillion.techtest2025.exception.BadRequestException;
import com.matillion.techtest2025.exception.NotFoundException;
import com.matillion.techtest2025.model.ColumnStatistics;
import com.matillion.techtest2025.model.DataType;
import com.matillion.techtest2025.repository.ColumnStatisticsRepository;
import com.matillion.techtest2025.repository.DataAnalysisRepository;
import com.matillion.techtest2025.repository.entity.ColumnStatisticsEntity;
import com.matillion.techtest2025.repository.entity.DataAnalysisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.DoubleStream;

@Service
@RequiredArgsConstructor
public class DataAnalysisService {

    private final DataAnalysisRepository dataAnalysisRepository;
    private final ColumnStatisticsRepository columnStatisticsRepository;

    /**
     * Analyze CSV:
     * - Validates input & forbidden token
     * - Counts rows/columns & per-column nulls (empty tokens only)
     * - Computes per-column unique (non-empty) counts
     * - NEW (Part 3): Infers column DataType and numeric stats for INTEGER/DECIMAL columns
     * - Persists parent + child entities
     * - Returns DTO (unchanged from Part 1/2)
     */
    public DataAnalysisResponse analyzeCsvData(String data) {
        // ---- Validation (Part 1) ----
        if (data == null || data.isEmpty()) {
            throw new BadRequestException("Input CSV is empty.");
        }
        if (data.contains("Sonny Hayes")) {
            throw new BadRequestException("Forbidden content: Sonny Hayes.");
        }

        long totalCharacters = data.length();

        // Normalize for parsing (preserve trailing empties)
        String normalized = data.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);
        if (lines.length == 0 || lines[0].isEmpty()) {
            throw new BadRequestException("CSV missing header row.");
        }

        String[] headers = splitRow(lines[0]); // preserve empty tokens
        int numberOfColumns = headers.length;

        // Per-column accumulators
        int[] nullCounts = new int[numberOfColumns];
        List<Set<String>> uniques = new ArrayList<>(numberOfColumns);
        for (int i = 0; i < numberOfColumns; i++) uniques.add(new HashSet<>());

        // NEW (Part 3): type inference + numeric values for stats
        DataType[] inferred = new DataType[numberOfColumns];
        Arrays.fill(inferred, null); // will be tightened as we see values

        List<List<Double>> numericValues = new ArrayList<>(numberOfColumns);
        for (int i = 0; i < numberOfColumns; i++) numericValues.add(new ArrayList<>());

        int numberOfRows = 0;

        // ---- Data rows ----
        for (int li = 1; li < lines.length; li++) {
            String line = lines[li];
            if (line.isBlank()) continue; // skip fully blank lines

            String[] cells = splitRow(line);
            if (cells.length != numberOfColumns) {
                throw new BadRequestException("Malformed CSV: inconsistent column count.");
            }

            numberOfRows++;

            for (int i = 0; i < numberOfColumns; i++) {
                String v = cells[i];
                if (v.isEmpty()) {
                    nullCounts[i]++;
                } else {
                    uniques.get(i).add(v);

                    // --- Type inference & numeric value capture ---
                    if (isBoolean(v)) {
                        inferred[i] = tighten(inferred[i], DataType.BOOLEAN);
                    } else if (isInteger(v)) {
                        inferred[i] = tighten(inferred[i], DataType.INTEGER);
                        numericValues.get(i).add(Double.valueOf(v));
                    } else if (isDecimal(v)) {
                        // widen INTEGER -> DECIMAL if previously integer
                        inferred[i] = tighten(inferred[i], DataType.DECIMAL);
                        numericValues.get(i).add(Double.valueOf(v));
                    } else {
                        // any other string forces STRING
                        inferred[i] = DataType.STRING;
                    }
                }
            }
        }

        // Default columns with only nulls to STRING
        for (int i = 0; i < numberOfColumns; i++) {
            if (inferred[i] == null) inferred[i] = DataType.STRING;
            // If we saw both integers & decimals via tighten, we already widened to DECIMAL.
        }

        // ---- Persist parent ----
        OffsetDateTime createdAt = OffsetDateTime.now();
        DataAnalysisEntity parent = DataAnalysisEntity.builder()
                .originalData(data)
                .numberOfRows(numberOfRows)
                .numberOfColumns(numberOfColumns)
                .totalCharacters(totalCharacters)
                .createdAt(createdAt)
                .build();
        dataAnalysisRepository.save(parent);

        // ---- Persist children with profiling fields ----
        List<ColumnStatisticsEntity> children = new ArrayList<>(numberOfColumns);
        for (int i = 0; i < numberOfColumns; i++) {
            // Compute numeric stats only for numeric columns & at least 1 numeric value
            List<Double> values = numericValues.get(i);
            values.sort(Double::compareTo);

            Integer nCount = null;
            Double min = null, max = null, mean = null, median = null, stddev = null;

            boolean isNumericType = (inferred[i] == DataType.INTEGER || inferred[i] == DataType.DECIMAL);
            if (isNumericType && !values.isEmpty()) {
                nCount = values.size();
                min = values.get(0);
                max = values.get(values.size() - 1);
                mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                median = computeMedian(values);
                stddev = computeStdDev(values, mean);
            }

            children.add(ColumnStatisticsEntity.builder()
                    .dataAnalysis(parent)
                    .columnName(headers[i])
                    .nullCount(nullCounts[i])
                    .uniqueCount(uniques.get(i).size())
                    .dataType(inferred[i])
                    .numericCount(nCount)
                    .numericMin(min)
                    .numericMax(max)
                    .numericMean(mean)
                    .numericMedian(median)
                    .numericStddev(stddev)
                    .build());
        }
        columnStatisticsRepository.saveAll(children);

        // ---- Build response (unchanged DTO for P1/P2) ----
        return new DataAnalysisResponse(
                numberOfRows,
                numberOfColumns,
                totalCharacters,
                children.stream()
                        .map(e -> new ColumnStatistics(
                                e.getColumnName(),
                                e.getNullCount(),
                                e.getUniqueCount()
                        ))
                        .toList(),
                createdAt
        );
    }

    /**
     * Retrieve previously persisted analysis by id (Part 2).
     */
    public DataAnalysisResponse getAnalysisById(Long id) {
        DataAnalysisEntity entity = dataAnalysisRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Analysis not found with id " + id));
        return toResponse(entity);
    }

    /**
     * Delete previously persisted analysis by id (Part 2).
     * Transactional ensures cascade delete of child stats happens atomically.
     */
    @Transactional
    public void deleteAnalysisById(Long id) {
        if (!dataAnalysisRepository.existsById(id)) {
            throw new NotFoundException("Analysis with ID " + id + " not found");
        }
        dataAnalysisRepository.deleteById(id);
    }

    // ----------------- helpers -----------------

    private String[] splitRow(String row) {
        return row.split(",", -1); // preserve empty tokens
    }

    private DataAnalysisResponse toResponse(DataAnalysisEntity entity) {
        return new DataAnalysisResponse(
                entity.getNumberOfRows(),
                entity.getNumberOfColumns(),
                entity.getTotalCharacters(),
                entity.getColumnStatistics().stream()
                        .map(e -> new ColumnStatistics(
                                e.getColumnName(),
                                e.getNullCount(),
                                e.getUniqueCount()
                        ))
                        .toList(),
                entity.getCreatedAt()
        );
    }

    // --- Type inference helpers ---

    private boolean isBoolean(String s) {
        return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
    }

    private boolean isInteger(String s) {
        return s.matches("^-?\\d+$");
    }

    private boolean isDecimal(String s) {
        return s.matches("^-?\\d+\\.\\d+$");
    }

    public ColumnProfileResponse getProfile(Long id) {
        DataAnalysisEntity entity = dataAnalysisRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Analysis not found with id " + id));

        var cols = entity.getColumnStatistics().stream()
                .map(stat -> new ColumnProfileResponse.ColumnProfile(
                        stat.getColumnName(),
                        stat.getDataType(),
                        stat.getNumericCount(),
                        stat.getNumericMin(),
                        stat.getNumericMax(),
                        stat.getNumericMean(),
                        stat.getNumericMedian(),
                        stat.getNumericStddev()
                ))
                .toList();

        return new ColumnProfileResponse(entity.getId(), cols);
    }
    /**
     * Prefer the most specific type unless contradicted:
     * - Start null -> take candidate
     * - STRING remains STRING
     * - INTEGER can widen to DECIMAL
     * - BOOLEAN can widen to INTEGER/DECIMAL
     */
    private DataType tighten(DataType current, DataType candidate) {
        if (current == null) return candidate;
        if (current == DataType.STRING) return current;
        if (candidate == DataType.STRING) return candidate;
        if (current == candidate) return current;

        // BOOLEAN -> (INTEGER|DECIMAL)
        if (current == DataType.BOOLEAN && (candidate == DataType.INTEGER || candidate == DataType.DECIMAL)) {
            return candidate;
        }
        // INTEGER -> DECIMAL
        if (current == DataType.INTEGER && candidate == DataType.DECIMAL) {
            return DataType.DECIMAL;
        }
        // DECIMAL stays DECIMAL unless STRING appears elsewhere (handled above)
        return current;
    }

    private Double computeMedian(List<Double> sorted) {
        int n = sorted.size();
        if (n == 0) return null;
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private Double computeStdDev(List<Double> values, Double mean) {
        if (values.isEmpty() || mean == null) return null;
        double m = mean;
        double variance = values.stream()
                .mapToDouble(v -> (v - m) * (v - m))
                .sum() / values.size();
        return Math.sqrt(variance);
    }
}
