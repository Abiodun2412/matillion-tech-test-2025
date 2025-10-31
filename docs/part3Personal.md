# Part 3 – Column Profiling (Type + Stats)

## Problem
After ingesting CSVs, users need to quickly understand their data:
- What type is each column? (`STRING`, `INTEGER`, `DECIMAL`, `BOOLEAN`)
- For numeric data, what are the basic stats (min, max, mean, median, stddev)?

## Solution
Extend the analysis to infer a data type per column and compute basic numeric statistics on non-empty values. Expose this profiling via a new read-only endpoint:
- `GET /api/analysis/{id}/profile` → returns the inferred type and (if numeric) stats for each column.

This does not change Part 1/2 response shapes and won’t break existing tests.

## How it Works
- **Nulls:** Empty tokens (`""` between commas) are treated as nulls (not trimmed).
- **Unique Count:** Excludes nulls (already implemented in Part 2).
- **Type Inference (non-empty values only):**
  1. If all values are `true|false` (case-insensitive) → `BOOLEAN`
  2. Else if all values match `^-?\d+$` → `INTEGER`
  3. Else if all values match `^-?\d+\.\d+$` or a mix of integers/decimals → `DECIMAL`
  4. Else → `STRING`
- **Numeric Stats:** For `INTEGER`/`DECIMAL` columns:
  - `count` (non-null numeric rows), `min`, `max`, `mean`, `median`, `stddev`
  - Computed from parsed doubles; nulls excluded.

## API
### GET `/api/analysis/{id}/profile`
**200 OK**:
```json
{
  "analysisId": 123,
  "columns": [
    {
      "columnName": "age",
      "dataType": "INTEGER",
      "numericCount": 10,
      "min": 18.0,
      "max": 54.0,
      "mean": 31.6,
      "median": 29.0,
      "stddev": 8.9
    },
    {
      "columnName": "name",
      "dataType": "STRING",
      "numericCount": null,
      "min": null,
      "max": null,
      "mean": null,
      "median": null,
      "stddev": null
    }
  ]
}
