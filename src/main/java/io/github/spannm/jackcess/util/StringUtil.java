package io.github.spannm.jackcess.util;

import java.nio.CharBuffer;
import java.util.stream.IntStream;

/**
 * <p> Static utility methods for null-safe {@link String} operations. </p>
 *
 * The class prefers interface {@link CharSequence} for inputs over {@code String} whenever possible, so that all implementations (e.g. {@link StringBuffer}, {@link StringBuilder}, {@link CharBuffer})
 * can benefit.
 *
 * @author Markus Spann
 */
public final class StringUtil {

    private StringUtil() {
    }

    /**
     * Gets the given char sequence's length or {@code 0} if it is {@code null}.
     *
     * @param cs string
     * @return length of string
     */
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * Checks if the given char sequence is either null or empty.
     *
     * @param cs char sequence to test
     * @return true if char sequence is empty or null, false otherwise
     */
    public static boolean isEmpty(CharSequence cs) {
        return length(cs) == 0;
    }

    /**
     * Returns {@code true} if the given char sequence is {@code null} or all blank space, {@code false} otherwise.
     */
    public static boolean isBlank(CharSequence cs) {
        int len = length(cs);
        return len == 0 || IntStream.range(0, len).allMatch(i -> Character.isWhitespace(cs.charAt(i)));
    }

    /**
     * Returns the given char sequence trimmed or {@code null} if the string is {@code null} or empty.
     */
    public static String trimToNull(CharSequence cs) {
        String str = cs == null ? null : cs.toString().trim();
        return isEmpty(str) ? null : str;
    }

    /**
     * Capitalizes a string changing its first character to title case as per {@link Character#toTitleCase(int)}.
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }

        int cp = str.codePointAt(0);
        int newCp = Character.toTitleCase(cp);
        return cp == newCp ? str : (char) newCp + str.substring(1);
    }

    public static String replace(String text, CharSequence searchString, CharSequence replacement) {
        return isEmpty(text) || isEmpty(searchString) ? text : text.replace(searchString, replacement);
    }

    /**
     * Removes all occurrences of character sequence {@code remove} from string {@code cs}.
     *
     * @param cs the character sequence to remove from
     * @param remove the character sequence to remove
     * @return modified input
     */
    public static String remove(CharSequence cs, CharSequence remove) {
        if (cs == null) {
            return null;
        }
        int len = cs.length();
        if (len == 0) {
            return "";
        } else if (isEmpty(remove) || remove.length() > len) {
            return cs.toString();
        }
        return cs.toString().replace(remove, "");
    }

}
