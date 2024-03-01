package io.github.spannm.jackcess.test.source;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntRangeArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<IntRangeSource> {

    private IntRangeSource cfg;

    @Override
    public Stream<Arguments> provideArguments(ExtensionContext _context) {
        return IntStream.range(cfg.start(), cfg.endInclusive() ? cfg.end() + 1 : cfg.end()).boxed().map(Arguments::of);
    }

    @Override
    public void accept(IntRangeSource source) {
        cfg = source;
    }

    @Override
    public String toString() {
        return String.format("%s[start=%d, end=%d (inclusive=%s)]",
            getClass().getSimpleName(), cfg.start(), cfg.end(), cfg.endInclusive());
    }

}
