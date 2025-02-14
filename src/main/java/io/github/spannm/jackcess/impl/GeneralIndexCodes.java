package io.github.spannm.jackcess.impl;

/**
 * Various constants used for creating "general" (access 2010+) sort order text index entries.
 *
 * @author James Ahlborn
 */
@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public class GeneralIndexCodes extends GeneralLegacyIndexCodes {

    // stash the codes in some resource files
    private static final String CODES_FILE     =
        DatabaseImpl.RESOURCE_PATH + "index_codes_gen.txt";
    private static final String EXT_CODES_FILE =
        DatabaseImpl.RESOURCE_PATH + "index_codes_ext_gen.txt";

    private static final class Codes {
        /**
         * handlers for the first 256 chars. use nested class to lazy load the handlers
         */
        private static final CharHandler[] VALUES = loadCodes(CODES_FILE, FIRST_CHAR, LAST_CHAR);
    }

    private static final class ExtCodes {
        /**
         * handlers for the rest of the chars in BMP 0. use nested class to lazy load the handlers
         */
        private static final CharHandler[] VALUES = loadCodes(EXT_CODES_FILE, FIRST_EXT_CHAR, LAST_EXT_CHAR);
    }

    static final GeneralIndexCodes GEN_INSTANCE = new GeneralIndexCodes();

    GeneralIndexCodes() {
    }

    /**
     * Returns the CharHandler for the given character.
     */
    @Override
    CharHandler getCharHandler(char c) {
        if (c <= LAST_CHAR) {
            return Codes.VALUES[c];
        }

        int extOffset = asUnsignedChar(c) - asUnsignedChar(FIRST_EXT_CHAR);
        return ExtCodes.VALUES[extOffset];
    }

}
