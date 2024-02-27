package io.github.spannm.jackcess.test.converter;

import org.junit.jupiter.params.converter.TypedArgumentConverter;

public class CsvToStringArray extends TypedArgumentConverter<String, String[]> {
    public CsvToStringArray() {
        super(String.class, String[].class);
    }

    @Override
    public String[] convert(String _source) {
        return _source == null || _source.isBlank() ? new String[0] : _source.split("\\s*,\\s*", -1);
    }
}
