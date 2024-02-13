/*
Copyright (c) 2013 James Ahlborn

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

package io.github.spannm.jackcess;

import io.github.spannm.jackcess.expr.EvalConfig;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.query.Query;
import io.github.spannm.jackcess.util.ColumnValidatorFactory;
import io.github.spannm.jackcess.util.ErrorHandler;
import io.github.spannm.jackcess.util.LinkResolver;
import io.github.spannm.jackcess.util.TableIterableBuilder;

import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An Access database instance. A new instance can be instantiated by opening an existing database file
 * ({@link DatabaseBuilder#open(File)}) or creating a new database file
 * ({@link DatabaseBuilder#create(Database.FileFormat,File)}) (for more advanced opening/creating use
 * {@link DatabaseBuilder}). Once a Database has been opened, you can interact with the data via the relevant
 * {@link Table}. When a Database instance is no longer useful, it should <b>always</b> be closed ({@link #close}) to
 * avoid corruption.
 * <p>
 * Database instances (and all the related objects) are <i>not</i> thread-safe. However, separate Database instances
 * (and their respective objects) can be used by separate threads without a problem.
 * <p>
 * Database instances do not implement any "transactional" support, and therefore concurrent editing of the same
 * database file by multiple Database instances (or with outside programs such as MS Access) <i>will generally result in
 * database file corruption</i>.
 *
 * @author James Ahlborn
 */
public interface Database extends Iterable<Table>, Closeable, Flushable {
    /**
     * default value for the auto-sync value ({@code true}). this is slower, but leaves more chance of a useable
     * database in the face of failures.
     */
    boolean           DEFAULT_AUTO_SYNC                     = true;

    /**
     * the default sort order for table columns.
     */
    Table.ColumnOrder DEFAULT_COLUMN_ORDER                  =
        Table.ColumnOrder.DATA;

    /**
     * system property which can be used to set the default TimeZone used for date calculations.
     */
    String            TIMEZONE_PROPERTY                     =
        "jackcess.timeZone";

    /**
     * system property prefix which can be used to set the default Charset used for text data (full property includes
     * the JetFormat version).
     */
    String            CHARSET_PROPERTY_PREFIX               =
        "jackcess.charset.";

    /**
     * system property which can be used to set the path from which classpath resources are loaded (must end with a "/"
     * if non-empty). Default value is {@value io.github.spannm.jackcess.impl.DatabaseImpl#DEFAULT_RESOURCE_PATH}
     * if unspecified.
     */
    String            RESOURCE_PATH_PROPERTY                =
        "jackcess.resourcePath";

    /**
     * (boolean) system property which can be used to indicate that the current vm has a poor nio implementation
     * (specifically for {@code FileChannel.transferFrom})
     */
    String            BROKEN_NIO_PROPERTY                   =
        "jackcess.brokenNio";

    /**
     * system property which can be used to set the default sort order for table columns. Value should be one of
     * {@link Table.ColumnOrder} enum values.
     */
    String            COLUMN_ORDER_PROPERTY                 =
        "jackcess.columnOrder";

    /**
     * system property which can be used to set the default enforcement of foreign-key relationships. Defaults to
     * {@code true}.
     */
    String            FK_ENFORCE_PROPERTY                   =
        "jackcess.enforceForeignKeys";

    /**
     * system property which can be used to set the default allow auto number insert policy. Defaults to {@code false}.
     */
    String            ALLOW_AUTONUM_INSERT_PROPERTY         =
        "jackcess.allowAutoNumberInsert";

    /**
     * system property which can be used to disable expression evaluation if necessary. Defaults to {@code true}.
     */
    String            ENABLE_EXPRESSION_EVALUATION_PROPERTY =
        "jackcess.enableExpressionEvaluation";

    /**
     * system property which can be used to set the default date/Time type. Value should be one of {@link DateTimeType}
     * enum values.
     */
    String            DATE_TIME_TYPE_PROPERTY               =
        "jackcess.dateTimeType";

    /**
     * Enum which indicates which version of Access created the database.
     *
     */
    enum FileFormat {

        /** A database which was created by MS Access 97 */
        V1997(".mdb"),
        /**
         * A database which was most likely created programmatically (e.g. using windows ADOX)
         */
        GENERIC_JET4(".mdb"),
        /** A database which was created by MS Access 2000 */
        V2000(".mdb"),
        /** A database which was created by MS Access 2002/2003 */
        V2003(".mdb"),
        /** A database which was created by MS Access 2007 */
        V2007(".accdb"),
        /** A database which was created by MS Access 2010+ */
        V2010(".accdb"),
        /** A database which was created by MS Access 2016+ */
        V2016(".accdb"),
        /** A database which was created by MS Access 2019+ (Office 365) */
        V2019(".accdb"),
        /** A database which was created by MS Money */
        MSISAM(".mny");

        private final String _ext;

        FileFormat(String ext) {
            _ext = ext;
        }

        /**
         * @return the file extension used for database files with this format.
         */
        public String getFileExtension() {
            return _ext;
        }

        @Override
        public String toString() {
            return name() + " [" + DatabaseImpl.getFileFormatDetails(this).getFormat() + "]";
        }
    }

    /**
     * Returns the File underlying this Database
     */
    File getFile();

    /**
     * Returns the File underlying this Database
     */
    Path getPath();

    /**
     * @return The names of all of the user tables
     */
    Set<String> getTableNames() throws IOException;

    /**
     * @return The names of all of the system tables (String). Note, in order to read these tables, you must use
     *         {@link #getSystemTable}. <i>Extreme care should be taken if modifying these tables directly!</i>.
     */
    Set<String> getSystemTableNames() throws IOException;

    /**
     * @return an unmodifiable Iterator of the user Tables in this Database.
     * @throws UncheckedIOException if an IOException is thrown by one of the operations, the actual exception will be
     *             contained within
     * @throws ConcurrentModificationException if a table is added to the database while an Iterator is in use.
     */
    @Override
    Iterator<Table> iterator();

    /**
     * @return a Stream using the default Iterator.
     */
    default Stream<Table> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Convenience method for constructing a new TableIterableBuilder for this cursor. A TableIterableBuilder provides a
     * variety of options for more flexible iteration of Tables.
     */
    TableIterableBuilder newIterable();

    /**
     * @return an Iterable which returns an unmodifiable Iterator of the the TableMetaData for all tables in this
     *         Database.
     * @throws UncheckedIOException if an IOException is thrown by one of the operations, the actual exception will be
     *             contained within
     * @throws ConcurrentModificationException if a table is added to the database while an Iterator is in use.
     */
    Iterable<TableMetaData> newTableMetaDataIterable();

    /**
     * @return a Stream using the {@link #newTableMetaDataIterable}
     */
    default Stream<TableMetaData> newTableMetaDataStream() {
        return StreamSupport.stream(
            newTableMetaDataIterable().spliterator(), false);
    }

    /**
     * @param name User table name (case-insensitive)
     * @return The Table, or null if it doesn't exist (or is a system table)
     */
    Table getTable(String name) throws IOException;

    /**
     * @param name Table name (case-insensitive), may be any table type (i.e. includes system or linked tables).
     * @return The meta data for the table, or null if it doesn't exist
     */
    TableMetaData getTableMetaData(String name) throws IOException;

    /**
     * Finds all the relationships in the database between the given tables.
     */
    List<Relationship> getRelationships(Table table1, Table table2)
        throws IOException;

    /**
     * Finds all the relationships in the database for the given table.
     */
    List<Relationship> getRelationships(Table table) throws IOException;

    /**
     * Finds all the relationships in the database in <i>non-system</i> tables.
     * <p>
     * Warning, this may load <i>all</i> the Tables (metadata, not data) in the database which could cause memory
     * issues.
     */
    List<Relationship> getRelationships() throws IOException;

    /**
     * Finds <i>all</i> the relationships in the database, <i>including system tables</i>.
     * <p>
     * Warning, this may load <i>all</i> the Tables (metadata, not data) in the database which could cause memory
     * issues.
     */
    List<Relationship> getSystemRelationships()
        throws IOException;

    /**
     * Finds all the queries in the database.
     */
    List<Query> getQueries() throws IOException;

    /**
     * Returns a reference to <i>any</i> available table in this access database, including system tables.
     * <p>
     * Warning, this method is not designed for common use, only for the occassional time when access to a system table
     * is necessary. Messing with system tables can strip the paint off your house and give your whole family a
     * permanent, orange afro. You have been warned.
     *
     * @param tableName Table name, may be a system table
     * @return The table, or {@code null} if it doesn't exist
     */
    Table getSystemTable(String tableName) throws IOException;

    /**
     * @return the core properties for the database
     */
    PropertyMap getDatabaseProperties() throws IOException;

    /**
     * @return the summary properties for the database
     */
    PropertyMap getSummaryProperties() throws IOException;

    /**
     * @return the user-defined properties for the database
     */
    PropertyMap getUserDefinedProperties() throws IOException;

    /**
     * @return the current database password, or {@code null} if none set.
     */
    String getDatabasePassword() throws IOException;

    /**
     * Create a new table in this database
     *
     * @param name Name of the table to create in this database
     * @param linkedDbName path to the linked database
     * @param linkedTableName name of the table in the linked database
     */
    void createLinkedTable(String name, String linkedDbName,
        String linkedTableName)
        throws IOException;

    /**
     * Flushes any current changes to the database file (and any linked databases) to disk.
     */
    @Override
    void flush() throws IOException;

    /**
     * Close the database file (and any linked databases). A Database <b>must</b> be closed after use or changes could
     * be lost and the Database file corrupted. A Database instance should be treated like any other external resource
     * which would be closed in a finally block (e.g. an OutputStream or jdbc Connection).
     */
    @Override
    void close() throws IOException;

    /**
     * Gets the currently configured ErrorHandler (always non-{@code null}). This will be used to handle all errors
     * unless overridden at the Table or Cursor level.
     */
    ErrorHandler getErrorHandler();

    /**
     * Sets a new ErrorHandler. If {@code null}, resets to the {@link ErrorHandler#DEFAULT}.
     */
    void setErrorHandler(ErrorHandler newErrorHandler);

    /**
     * Gets the currently configured LinkResolver (always non-{@code null}). This will be used to handle all linked
     * database loading.
     */
    LinkResolver getLinkResolver();

    /**
     * Sets a new LinkResolver. If {@code null}, resets to the {@link LinkResolver#DEFAULT}.
     */
    void setLinkResolver(LinkResolver newLinkResolver);

    /**
     * Returns an unmodifiable view of the currently loaded linked databases, mapped from the linked database file name
     * to the linked database. This information may be useful for implementing a LinkResolver.
     */
    Map<String, Database> getLinkedDatabases();

    /**
     * Returns {@code true} if this Database links to the given Table, {@code
     * false} otherwise.
     */
    boolean isLinkedTable(Table table) throws IOException;

    /**
     * Gets currently configured TimeZone (always non-{@code null} and aligned with the ZoneId).
     */
    TimeZone getTimeZone();

    /**
     * Sets a new TimeZone. If {@code null}, resets to the default value. Note that setting the TimeZone will alter the
     * ZoneId as well.
     */
    void setTimeZone(TimeZone newTimeZone);

    /**
     * Gets currently configured ZoneId (always non-{@code null} and aligned with the TimeZone).
     */
    ZoneId getZoneId();

    /**
     * Sets a new ZoneId. If {@code null}, resets to the default value. Note that setting the ZoneId will alter the
     * TimeZone as well.
     */
    void setZoneId(ZoneId newZoneId);

    /**
     * Gets currently configured Charset (always non-{@code null}).
     */
    Charset getCharset();

    /**
     * Sets a new Charset. If {@code null}, resets to the default value.
     */
    void setCharset(Charset newCharset);

    /**
     * Gets currently configured {@link Table.ColumnOrder} (always non-{@code
     * null}).
     */
    Table.ColumnOrder getColumnOrder();

    /**
     * Sets a new Table.ColumnOrder. If {@code null}, resets to the default value.
     */
    void setColumnOrder(Table.ColumnOrder newColumnOrder);

    /**
     * Gets current foreign-key enforcement policy.
     */
    boolean isEnforceForeignKeys();

    /**
     * Sets a new foreign-key enforcement policy. If {@code null}, resets to the default value.
     */
    void setEnforceForeignKeys(Boolean newEnforceForeignKeys);

    /**
     * Gets current allow auto number insert policy. By default, jackcess does not allow auto numbers to be inserted or
     * updated directly (they are always handled internally by the Table). Setting this policy to {@code
     * true} allows the caller to optionally set the value explicitly when adding or updating rows (if a value is not
     * provided, it will still be handled internally by the Table). This value can be set database-wide using
     * {@link #setAllowAutoNumberInsert} and/or on a per-table basis using {@link Table#setAllowAutoNumberInsert}
     * (and/or on a jvm-wide using the {@link #ALLOW_AUTONUM_INSERT_PROPERTY} system property). Note that <i>enabling
     * this feature should be done with care</i> to reduce the chances of screwing up the database.
     */
    boolean isAllowAutoNumberInsert();

    /**
     * Sets the new auto number insert policy for the database (unless overridden at the Table level). If {@code null},
     * resets to the default value.
     */
    void setAllowAutoNumberInsert(Boolean allowAutoNumInsert);

    /**
     * Gets the current expression evaluation policy. Expression evaluation is enabled by default but can be disabled if
     * necessary.
     */
    boolean isEvaluateExpressions();

    /**
     * Sets the current expression evaluation policy. Expression evaluation is enabled by default but can be disabled if
     * necessary. If {@code null}, resets to the default value.
     */
    void setEvaluateExpressions(Boolean evaluateExpressions);

    /**
     * Gets currently configured ColumnValidatorFactory (always non-{@code null}).
     */
    ColumnValidatorFactory getColumnValidatorFactory();

    /**
     * Sets a new ColumnValidatorFactory. If {@code null}, resets to the default value. The configured
     * ColumnValidatorFactory will be used to create ColumnValidator instances on any <i>user</i> tables loaded from
     * this point onward (this will not be used for system tables).
     */
    void setColumnValidatorFactory(ColumnValidatorFactory newFactory);

    /**
     * Returns the FileFormat of this database (which may involve inspecting the database itself).
     *
     * @throws IllegalStateException if the file format cannot be determined
     */
    FileFormat getFileFormat() throws IOException;

    /**
     * Returns the EvalConfig for configuring expression evaluation.
     */
    EvalConfig getEvalConfig();

    /**
     * Gets the currently configured DateTimeType.
     */
    DateTimeType getDateTimeType();

    /**
     * Sets the DateTimeType. If {@code null}, resets to the default value.
     */
    void setDateTimeType(DateTimeType dateTimeType);
}
