package io.github.spannm.jackcess.test.source;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IntRangeArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<Arguments> provideArguments(ExtensionContext context) {
        Optional<IntRangeSource> optSrc = context.getElement().map(elem -> AnnotationSupport.findAnnotation(elem, IntRangeSource.class).get());
        return optSrc.map(cfg -> IntStream.range(cfg.start(), cfg.endInclusive() ? cfg.end() + 1 : cfg.end()).boxed().map(Arguments::of)).orElse(Stream.empty());
    }

}
