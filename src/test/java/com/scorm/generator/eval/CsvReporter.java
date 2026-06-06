package com.scorm.generator.eval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CSV writer for AI evaluation results.
 *
 * <p>Each instance writes one CSV file with a fixed header. Rows are
 * flushed after every write so that a crash mid-run still preserves
 * partial results — important when each row costs real API money.
 */
public class CsvReporter implements AutoCloseable {

    private final Path path;
    private final String[] headers;
    private final BufferedWriter writer;
    private int rowCount = 0;

    public CsvReporter(Path path, String... headers) throws IOException {
        this.path = path;
        this.headers = headers;
        Files.createDirectories(path.getParent());
        this.writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        writer.write(String.join(",", headers));
        writer.newLine();
        writer.flush();
    }

    /**
     * Write one row. Caller must provide a value for every header (use null/"" for missing).
     */
    public void writeRow(Map<String, Object> values) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object v = values.get(headers[i]);
            sb.append(escape(v == null ? "" : v.toString()));
        }
        writer.write(sb.toString());
        writer.newLine();
        writer.flush();
        rowCount++;
    }

    /**
     * Convenience: build a row from key-value pairs.
     */
    public static Map<String, Object> row() {
        return new LinkedHashMap<>();
    }

    private static String escape(String s) {
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!needsQuote) {
            return s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public Path getPath() {
        return path;
    }

    public int getRowCount() {
        return rowCount;
    }

    @Override
    public void close() throws IOException {
        writer.flush();
        writer.close();
    }
}
