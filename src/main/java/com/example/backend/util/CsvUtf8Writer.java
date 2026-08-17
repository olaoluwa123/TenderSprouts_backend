package com.example.backend.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvUtf8Writer {

    private CsvUtf8Writer() {
    }

    public static byte[] write(List<String> columns, List<List<Object>> rows) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            // UTF-8 BOM for Excel
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);
            writeLine(writer, columns.stream().map(CsvUtf8Writer::stringify).toList());
            for (List<Object> row : rows) {
                writeLine(writer, row.stream().map(CsvUtf8Writer::stringify).toList());
            }
            writer.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void writeLine(OutputStreamWriter writer, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(i)));
        }
        writer.write('\n');
    }

    private static String stringify(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String escape(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
