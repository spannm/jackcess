package io.github.spannm.jackcess.test.source;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntRangeArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<IntRangeSource> {

    private int     start;
    private int     end;
    private boolean endInclusive;

    @Override
    public Stream<Arguments> provideArguments(ExtensionContext _context) {
        return IntStream.range(start, endInclusive ? end + 1 : end).boxed().map(Arguments::of);
    }

    @Override
    public void accept(IntRangeSource _rangeSource) {
        start = _rangeSource.start();
        end = _rangeSource.end();
        endInclusive = _rangeSource.endInclusive();
    }

    @Override
    public String toString() {
        return String.format("%s[start=%d, end=%d (inclusive=%s)]",
            getClass().getSimpleName(), start, end, endInclusive);
    }

}
