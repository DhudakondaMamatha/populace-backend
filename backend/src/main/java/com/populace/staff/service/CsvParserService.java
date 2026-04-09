package com.populace.staff.service;

import com.populace.staff.contract.StaffFieldContract;
import com.populace.staff.dto.BulkStaffRow;
import com.populace.staff.dto.BulkUploadError;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses CSV files for bulk staff upload.
 */
@Service
public class CsvParserService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    /** Derived from {@link StaffFieldContract} — single source of truth for CSV headers. */
    private static final List<String> EXPECTED_HEADERS = StaffFieldContract.allCsvHeaders();

    /** Derived from {@link StaffFieldContract} — required headers for validation. */
    private static final Set<String> REQUIRED_HEADERS = StaffFieldContract.requiredCsvHeaders();

    public record ParseResult(
        List<BulkStaffRow> rows,
        List<BulkUploadError> errors
    ) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    public ParseResult parse(MultipartFile file) {
        List<BulkUploadError> errors = new ArrayList<>();

        if (file.isEmpty()) {
            errors.add(createError(0, null, null, "FILE_EMPTY", "Uploaded file is empty"));
            return new ParseResult(List.of(), errors);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            errors.add(createError(0, null, null, "FILE_TOO_LARGE", "File exceeds maximum size of 5MB"));
            return new ParseResult(List.of(), errors);
        }

        try (BufferedReader reader = createReader(file)) {
            return parseContent(reader, errors);
        } catch (IOException e) {
            errors.add(createError(0, null, null, "INVALID_FORMAT", "Failed to read CSV file: " + e.getMessage()));
            return new ParseResult(List.of(), errors);
        }
    }

    private ParseResult parseContent(BufferedReader reader, List<BulkUploadError> errors) throws IOException {
        String headerLine = reader.readLine();
        if (headerLine == null || headerLine.isBlank()) {
            errors.add(createError(0, null, null, "FILE_EMPTY", "File has no header row"));
            return new ParseResult(List.of(), errors);
        }

        Map<String, Integer> headerIndex = parseHeaders(headerLine, errors);
        if (!errors.isEmpty()) {
            return new ParseResult(List.of(), errors);
        }

        List<BulkStaffRow> rows = parseDataRows(reader, headerIndex);
        if (rows.isEmpty()) {
            errors.add(createError(0, null, null, "FILE_EMPTY", "File has no data rows"));
            return new ParseResult(List.of(), errors);
        }

        return new ParseResult(rows, errors);
    }

    private Map<String, Integer> parseHeaders(String headerLine, List<BulkUploadError> errors) {
        List<String> headers = parseCsvLine(headerLine);
        Map<String, Integer> headerIndex = new HashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).toLowerCase().trim();
            if (headerIndex.containsKey(header)) {
                errors.add(createError(1, header, null, "DUPLICATE_HEADER", "Duplicate header column: " + header));
            }
            headerIndex.put(header, i);
        }

        for (String required : REQUIRED_HEADERS) {
            if (!headerIndex.containsKey(required)) {
                errors.add(createError(1, required, null, "MISSING_HEADER", "Required header column missing: " + required));
            }
        }

        return headerIndex;
    }

    private List<BulkStaffRow> parseDataRows(BufferedReader reader, Map<String, Integer> headerIndex) throws IOException {
        List<BulkStaffRow> rows = new ArrayList<>();
        String line;
        int rowNumber = 1;

        while ((line = reader.readLine()) != null) {
            rowNumber++;
            if (line.isBlank()) {
                continue;
            }

            List<String> values = parseCsvLine(line);
            BulkStaffRow row = buildRowFromCsvValues(rowNumber, values, headerIndex);
            rows.add(row);
        }

        return rows;
    }

    private BulkStaffRow buildRowFromCsvValues(int rowNumber, List<String> values, Map<String, Integer> headerIndex) {
        return new BulkStaffRow(
            rowNumber,

            // Identity
            getValue(values, headerIndex, "employee_code"),
            getValue(values, headerIndex, "first_name"),
            getValue(values, headerIndex, "last_name"),
            getValue(values, headerIndex, "email"),
            getValue(values, headerIndex, "phone"),
            getValue(values, headerIndex, "employment_type"),

            // Assignments
            parseList(getValue(values, headerIndex, "roles")),
            parseList(getValue(values, headerIndex, "sites")),
            parseList(getValue(values, headerIndex, "competence_levels")),
            getValue(values, headerIndex, "primary_role"),

            // Compensation
            getValue(values, headerIndex, "compensation_type"),
            getValue(values, headerIndex, "hourly_rate"),
            getValue(values, headerIndex, "monthly_salary"),

            // Work hours
            getValue(values, headerIndex, "min_hours_per_day"),
            getValue(values, headerIndex, "max_hours_per_day"),
            getValue(values, headerIndex, "min_hours_per_month"),
            getValue(values, headerIndex, "max_hours_per_month"),
            getValue(values, headerIndex, "min_days_off_per_week"),
            getValue(values, headerIndex, "max_sites_per_day"),
            getValue(values, headerIndex, "min_hours_per_week"),
            getValue(values, headerIndex, "max_hours_per_week"),

            // Mandatory leave
            getValue(values, headerIndex, "must_go_on_leave_after_days"),
            getValue(values, headerIndex, "accrues_one_day_leave_after_days"),

            // Break overrides (optional)
            getValue(values, headerIndex, "min_break_minutes"),
            getValue(values, headerIndex, "max_break_minutes"),
            getValue(values, headerIndex, "min_work_minutes_before_break"),
            getValue(values, headerIndex, "max_continuous_work_minutes")
        );
    }

    public String generateSampleCsv() {
        StringBuilder csv = new StringBuilder();

        csv.append(String.join(",", EXPECTED_HEADERS)).append("\n");

        // Row 1: Full-time nurse with 2 roles, 1 site, hourly compensation, all fields filled including break overrides
        csv.append("EMP001,Sarah,Johnson,sarah.johnson@example.com,+61412345678,permanent,");
        csv.append("Nurse|Team Lead,Main Hospital,L2|L3,Nurse,hourly,45.00,,");
        csv.append("4,10,80,200,1,1,20,48,160,");
        csv.append("30,7,");
        csv.append("15,30,180,300\n");

        // Row 2: Part-time contract worker with 1 role, 2 sites, monthly compensation, no break overrides
        csv.append("EMP002,James,Chen,james.chen@example.com,+61498765432,contract,");
        csv.append("Cleaner|Security,\"Site Alpha|Site Beta\",L1|L2,Security,monthly,,5200,");
        csv.append("3,8,60,160,2,2,15,40,120,");
        csv.append(",,");
        csv.append(",,,\n");

        // Row 3: Minimal row with only required fields + all work params, no break overrides
        csv.append(",Emma,Williams,,,permanent,");
        csv.append("Doctor,Clinic A,,,hourly,85.00,,");
        csv.append("4,10,80,200,1,1,20,48,160,");
        csv.append(",,");
        csv.append(",,,\n");

        return csv.toString();
    }

    private BufferedReader createReader(MultipartFile file) throws IOException {
        return new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
    }

    private BulkUploadError createError(int row, String column, String value, String code, String message) {
        return new BulkUploadError(row, column, value, code, message);
    }

    private String getValue(List<String> values, Map<String, Integer> headerIndex, String header) {
        Integer index = headerIndex.get(header);
        if (index == null || index >= values.size()) {
            return null;
        }
        String value = values.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    private List<String> parseList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|", -1))
            .map(String::trim)
            .toList();
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());
        return result;
    }
}
