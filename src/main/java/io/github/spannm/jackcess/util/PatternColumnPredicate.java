package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.expr.Expressionator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Predicate which tests a column value against a {@link Pattern}. The static factory methods can be used to construct
 * the Pattern from various forms of wildcard pattern syntaxes.
 * <p>
 * This class can be used as a value pattern in the various Cursor search methods, e.g.
 * {@link io.github.spannm.jackcess.Cursor#findFirstRow(io.github.spannm.jackcess.Column,Object)}.
 */
public class PatternColumnPredicate implements Predicate<Object> {
    private static final int LIKE_REGEX_FLAGS    = Pattern.DOTALL;
    private static final int CI_LIKE_REGEX_FLAGS = LIKE_REGEX_FLAGS | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

    private final Pattern    _pattern;

    public PatternColumnPredicate(Pattern pattern) {
        _pattern = pattern;
    }

    @Override
    public boolean test(Object value) {
        try {
            // convert column value to string
            CharSequence cs = ColumnImpl.toCharSequence(value);

            return _pattern.matcher(cs).matches();
        } catch (IOException _ex) {
            throw new UncheckedIOException("Could not coerece column value to string", _ex);
        }
    }

    private static Pattern sqlLikeToRegex(
        String value, boolean caseInsensitive) {
        StringBuilder sb = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);

            if (c == '%') {
                sb.append(".*");
            } else if (c == '_') {
                sb.append('.');
            } else if (c == '\\') {
                if (i + 1 < value.length()) {
                    appendLiteralChar(sb, value.charAt(++i));
                }
            } else {
                appendLiteralChar(sb, c);
            }
        }

        int flags = caseInsensitive ? CI_LIKE_REGEX_FLAGS : LIKE_REGEX_FLAGS;
        return Pattern.compile(sb.toString(), flags);
    }

    private static void appendLiteralChar(StringBuilder sb, char c) {
        if (Expressionator.isRegexSpecialChar(c)) {
            sb.append('\\');
        }
        sb.append(c);
    }

    /**
     * @return a PatternColumnPredicate which tests values against the given ms access wildcard pattern (always case
     *         insensitive)
     */
    public static PatternColumnPredicate forAccessLike(String pattern) {
        return new PatternColumnPredicate(Expressionator.likePatternToRegex(pattern));
    }

    /**
     * @return a PatternColumnPredicate which tests values against the given sql like pattern (supports escape char '\')
     */
    public static PatternColumnPredicate forSqlLike(String pattern) {
        return forSqlLike(pattern, false);
    }

    /**
     * @return a PatternColumnPredicate which tests values against the given sql like pattern (supports escape char
     *         '\'), optionally case insensitive
     */
    public static PatternColumnPredicate forSqlLike(
        String pattern, boolean caseInsensitive) {
        return new PatternColumnPredicate(sqlLikeToRegex(pattern, caseInsensitive));
    }

    /**
     * @return a PatternColumnPredicate which tests values against the given java regex pattern
     */
    public static PatternColumnPredicate forJavaRegex(String pattern) {
        return new PatternColumnPredicate(Pattern.compile(pattern));
    }
}
