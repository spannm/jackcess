package io.github.spannm.jackcess.test.source;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntMatrixArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<Arguments> provideArguments(ExtensionContext context) {
        Optional<IntMatrixSource> optSrc = context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, IntMatrixSource.class).get());
        return optSrc.map(cfg -> {
            int end = cfg.end() + (cfg.endInclusive() ? 1 : 0);
            return IntStream.range(cfg.start(), end).boxed()
                .flatMap(x -> IntStream.range(cfg.start(), end).mapToObj(y -> Arguments.of(x, y)));
        }).orElse(Stream.empty());
    }

}
