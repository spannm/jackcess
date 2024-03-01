package io.github.spannm.jackcess.test.source;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntMatrixArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<IntMatrixSource> {

    private IntMatrixSource cfg;

    @Override
    public Stream<Arguments> provideArguments(ExtensionContext context) {
        int end = cfg.end() + (cfg.endInclusive() ? 1 : 0);
        return IntStream.range(cfg.start(), end).boxed()
            .flatMap(x -> IntStream.range(cfg.start(), end).mapToObj(y -> Arguments.of(x, y)));
    }

    @Override
    public void accept(IntMatrixSource source) {
        cfg = source;
    }

    @Override
    public String toString() {
        return String.format("%s[start=%d, end=%d (inclusive=%s)]",
            getClass().getSimpleName(), cfg.start(), cfg.end(), cfg.endInclusive());
    }

}
