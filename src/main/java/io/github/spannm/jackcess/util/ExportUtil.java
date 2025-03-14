/*
Copyright (c) 2007 Health Market Science, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.impl.ByteUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Utility class for exporting tables from an Access database to other formats. See the {@link Builder} for convenient
 * configuration of the export functionality. Note that most scenarios for customizing output data can be handled by
 * implementing a custom {@link ExportFilter}.
 *
 * @author Frank Gerbig
 */
public class ExportUtil {

    public static final String DEFAULT_DELIMITER  = ",";
    public static final char   DEFAULT_QUOTE_CHAR = '"';
    public static final String DEFAULT_FILE_EXT   = "csv";

    private ExportUtil() {
    }

    /**
     * Copy all tables into new delimited text files <br>
     * Equivalent to: {@code exportAll(db, dir, "csv");}
     *
     * @param db Database the table to export belongs to
     * @param dir The directory where the new files will be created
     *
     * @see #exportAll(Database,File,String)
     * @see Builder
     */
    public static void exportAll(Database db, File dir) throws IOException {
        exportAll(db, dir, DEFAULT_FILE_EXT);
    }

    /**
     * Copy all tables into new delimited text files <br>
     * Equivalent to: {@code exportFile(db, name, f, false, null, '"',
     * SimpleExportFilter.INSTANCE);}
     *
     * @param db Database the table to export belongs to
     * @param dir The directory where the new files will be created
     * @param ext The file extension of the new files
     *
     * @see #exportFile(Database,String,File,boolean,String,char,ExportFilter)
     * @see Builder
     */
    public static void exportAll(Database db, File dir,
        String ext) throws IOException {
        for (String tableName : db.getTableNames()) {
            exportFile(db, tableName, new File(dir, tableName + "." + ext), false,
                DEFAULT_DELIMITER, DEFAULT_QUOTE_CHAR, SimpleExportFilter.INSTANCE);
        }
    }

    /**
     * Copy all tables into new delimited text files <br>
     * Equivalent to: {@code exportFile(db, name, f, false, null, '"',
     * SimpleExportFilter.INSTANCE);}
     *
     * @param db Database the table to export belongs to
     * @param dir The directory where the new files will be created
     * @param ext The file extension of the new files
     * @param header If <code>true</code> the first line contains the column names
     *
     * @see #exportFile(Database,String,File,boolean,String,char,ExportFilter)
     * @see Builder
     */
    public static void exportAll(Database db, File dir,
        String ext, boolean header) throws IOException {
        for (String tableName : db.getTableNames()) {
            exportFile(db, tableName, new File(dir, tableName + "." + ext), header,
                DEFAULT_DELIMITER, DEFAULT_QUOTE_CHAR, SimpleExportFilter.INSTANCE);
        }
    }

    /**
     * Copy all tables into new delimited text files <br>
     * Equivalent to: {@code exportFile(db, name, f, false, null, '"',
     * SimpleExportFilter.INSTANCE);}
     *
     * @param db Database the table to export belongs to
     * @param dir The directory where the new files will be created
     * @param ext The file extension of the new files
     * @param header If <code>true</code> the first line contains the column names
     * @param delim The column delimiter, <code>null</code> for default (comma)
     * @param quote The quote character
     * @param filter valid export filter
     *
     * @see #exportFile(Database,String,File,boolean,String,char,ExportFilter)
     * @see Builder
     */
    public static void exportAll(Database db, File dir,
        String ext, boolean header, String delim,
        char quote, ExportFilter filter) throws IOException {
        for (String tableName : db.getTableNames()) {
            exportFile(db, tableName, new File(dir, tableName + "." + ext), header,
                delim, quote, filter);
        }
    }

    /**
     * Copy a table into a new delimited text file <br>
     * Equivalent to: {@code exportFile(db, name, f, false, null, '"',
     * SimpleExportFilter.INSTANCE);}
     *
     * @param db Database the table to export belongs to
     * @param tableName Name of the table to export
     * @param f New file to create
     *
     * @see #exportFile(Database,String,File,boolean,String,char,ExportFilter)
     * @see Builder
     */
    public static void exportFile(Database db, String tableName,
        File f) throws IOException {
        exportFile(db, tableName, f, false, DEFAULT_DELIMITER, DEFAULT_QUOTE_CHAR,
            SimpleExportFilter.INSTANCE);
    }

    /**
     * Copy a table into a new delimited text file <br>
     * Nearly equivalent to: {@code exportWriter(db, name, new BufferedWriter(f),
     * header, delim, quote, filter);}
     *
     * @param db Database the table to export belongs to
     * @param tableName Name of the table to export
     * @param f New file to create
     * @param header If <code>true</code> the first line contains the column names
     * @param delim The column delimiter, <code>null</code> for default (comma)
     * @param quote The quote character
     * @param filter valid export filter
     *
     * @see #exportWriter(Database,String,BufferedWriter,boolean,String,char,ExportFilter)
     * @see Builder
     */
    public static void exportFile(Database db, String tableName, File f, boolean header,
            String delim, char quote, ExportFilter filter) throws IOException {

        try (BufferedWriter out = new BufferedWriter(new FileWriter(f))) {
            exportWriter(db, tableName, out, header, delim, quote, filter);
        }
    }

    /**
     * Copy a table in this database into a new delimited text file <br>
     * Equivalent to: {@code exportWriter(db, name, out, false, null, '"',
     * SimpleExportFilter.INSTANCE);}
     *
     * @param db Database the table to export belongs to
     * @param tableName Name of the table to export
     * @param out Writer to export to
     *
     * @see #exportWriter(Database,String,BufferedWriter,boolean,String,char,ExportFilter)
     * @see Builder
     */
    public static void exportWriter(Database db, String tableName,
        BufferedWriter out) throws IOException {
        exportWriter(db, tableName, out, false, DEFAULT_DELIMITER,
            DEFAULT_QUOTE_CHAR, SimpleExportFilter.INSTANCE);
    }

    /**
     * Copy a table in this database into a new delimited text file. <br>
     * Equivalent to:
     * {@code exportWriter(Cursor.createCursor(db.getTable(tableName)), out, header, delim, quote, filter);}
     *
     * @param db Database the table to export belongs to
     * @param tableName Name of the table to export
     * @param out Writer to export to
     * @param header If <code>true</code> the first line contains the column names
     * @param delim The column delimiter, <code>null</code> for default (comma)
     * @param quote The quote character
     * @param filter valid export filter
     *
     * @see #exportWriter(Cursor,BufferedWriter,boolean,String,char,ExportFilter)
     * @see Builder
     */
    public static void exportWriter(Database db, String tableName, BufferedWriter out, boolean header, String delim,
        char quote, ExportFilter filter) throws IOException {
        exportWriter(CursorBuilder.createCursor(db.getTable(tableName)), out, header,
            delim, quote, filter);
    }

    /**
     * Copy a table in this database into a new delimited text file.
     *
     * @param cursor Cursor to export
     * @param out Writer to export to
     * @param header If <code>true</code> the first line contains the column names
     * @param delim The column delimiter, <code>null</code> for default (comma)
     * @param quote The quote character
     * @param filter valid export filter
     *
     * @see Builder
     */
    public static void exportWriter(Cursor cursor,
        BufferedWriter out, boolean header, String delim,
        char quote, ExportFilter filter) throws IOException {
        String delimiter = delim == null ? DEFAULT_DELIMITER : delim;

        // create pattern which will indicate whether or not a value needs to be
        // quoted or not (contains delimiter, separator, or newline)
        Pattern needsQuotePattern = Pattern.compile(
            "(?:" + Pattern.quote(delimiter) + ")|(?:" + Pattern.quote("" + quote) + ")|(?:[\n\r])");

        List<? extends Column> origCols = cursor.getTable().getColumns();
        List<Column> columns = new ArrayList<>(origCols);
        columns = filter.filterColumns(columns);

        Collection<String> columnNames = null;
        if (!origCols.equals(columns)) {

            // columns have been filtered
            columnNames = new HashSet<>();
            for (Column c : columns) {
                columnNames.add(c.getName());
            }
        }

        // print the header row (if desired)
        if (header) {
            for (Iterator<Column> iter = columns.iterator(); iter.hasNext();) {

                writeValue(out, iter.next().getName(), quote, needsQuotePattern);

                if (iter.hasNext()) {
                    out.write(delimiter);
                }
            }
            out.newLine();
        }

        // print the data rows
        Object[] unfilteredRowData = new Object[columns.size()];
        Row row;
        while ((row = cursor.getNextRow(columnNames)) != null) {

            // fill raw row data in array
            for (int i = 0; i < columns.size(); i++) {
                unfilteredRowData[i] = columns.get(i).getRowValue(row);
            }

            // apply filter
            Object[] rowData = filter.filterRow(unfilteredRowData);
            if (rowData == null) {
                continue;
            }

            // print row
            for (int i = 0; i < columns.size(); i++) {

                Object obj = rowData[i];
                if (obj != null) {

                    String value = null;
                    if (obj instanceof byte[]) {

                        value = ByteUtil.toHexString((byte[]) obj);

                    } else {

                        value = String.valueOf(rowData[i]);
                    }

                    writeValue(out, value, quote, needsQuotePattern);
                }

                if (i < columns.size() - 1) {
                    out.write(delimiter);
                }
            }

            out.newLine();
        }

        out.flush();
    }

    private static void writeValue(BufferedWriter out, String value, char quote,
        Pattern needsQuotePattern) throws IOException {
        if (!needsQuotePattern.matcher(value).find()) {

            // no quotes necessary
            out.write(value);
            return;
        }

        // wrap the value in quotes and handle internal quotes
        out.write(quote);
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);

            if (c == quote) {
                out.write(quote);
            }
            out.write(c);
        }
        out.write(quote);
    }

    /**
     * Builder which simplifies configuration of an export operation.
     */
    public static class Builder {
        private Database     mdb;
        private String       mtableName;
        private String       mext    = DEFAULT_FILE_EXT;
        private Cursor       mcursor;
        private String       mdelim  = DEFAULT_DELIMITER;
        private char         mquote  = DEFAULT_QUOTE_CHAR;
        private ExportFilter mfilter = SimpleExportFilter.INSTANCE;
        private boolean      mheader;

        public Builder(Database _db) {
            this(_db, null);
        }

        public Builder(Database _db, String _tableName) {
            mdb = _db;
            mtableName = _tableName;
        }

        public Builder(Cursor _cursor) {
            mcursor = _cursor;
        }

        public Builder withDatabase(Database _db) {
            mdb = _db;
            return this;
        }

        public Builder withTableName(String _tableName) {
            mtableName = _tableName;
            return this;
        }

        public Builder withCursor(Cursor _cursor) {
            mcursor = _cursor;
            return this;
        }

        public Builder withDelimiter(String _delim) {
            mdelim = _delim;
            return this;
        }

        public Builder withQuote(char _quote) {
            mquote = _quote;
            return this;
        }

        public Builder withFilter(ExportFilter _filter) {
            mfilter = _filter;
            return this;
        }

        public Builder withHeader(boolean _header) {
            mheader = _header;
            return this;
        }

        public Builder withFileNameExtension(String _ext) {
            mext = _ext;
            return this;
        }

        /**
         * @see ExportUtil#exportAll(Database,File,String,boolean,String,char,ExportFilter)
         */
        public void exportAll(File dir) throws IOException {
            ExportUtil.exportAll(mdb, dir, mext, mheader, mdelim, mquote, mfilter);
        }

        /**
         * @see ExportUtil#exportFile(Database,String,File,boolean,String,char,ExportFilter)
         */
        public void exportFile(File f) throws IOException {
            ExportUtil.exportFile(mdb, mtableName, f, mheader, mdelim, mquote,
                mfilter);
        }

        /**
         * @see ExportUtil#exportWriter(Database,String,BufferedWriter,boolean,String,char,ExportFilter)
         * @see ExportUtil#exportWriter(Cursor,BufferedWriter,boolean,String,char,ExportFilter)
         */
        public void exportWriter(BufferedWriter writer) throws IOException {
            if (mcursor != null) {
                ExportUtil.exportWriter(mcursor, writer, mheader, mdelim,
                    mquote, mfilter);
            } else {
                ExportUtil.exportWriter(mdb, mtableName, writer, mheader, mdelim,
                    mquote, mfilter);
            }
        }
    }

}
