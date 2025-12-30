/*
 * Copyright (C) 2024- Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.test.converter;

import org.junit.jupiter.params.converter.TypedArgumentConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CsvToLocalDateTime extends TypedArgumentConverter<String, LocalDateTime> {
    public CsvToLocalDateTime() {
        super(String.class, LocalDateTime.class);
    }

    @Override
    public LocalDateTime convert(String _source) {
        if (_source == null || _source.isBlank()) {
            return LocalDateTime.now();
        }
        List<Integer> list = Arrays.stream(_source.split("\\s*,\\s*")).map(Integer::parseInt).collect(Collectors.toList());
        if (list.size() < 5 || list.size() > 6) {
            throw new IllegalArgumentException("5 or 6 integer parameters required to create " + LocalDateTime.class.getSimpleName());
        }
        LocalDate date = LocalDate.of(list.get(0), list.get(1), list.get(2));
        LocalTime time = list.size() == 5 ? LocalTime.of(list.get(3), list.get(4)) : LocalTime.of(list.get(3), list.get(4), list.get(5));
        return LocalDateTime.of(date, time);
    }
}
