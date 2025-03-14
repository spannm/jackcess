package io.github.spannm.jackcess;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.CodecProvider;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.PropertyMapImpl;
import io.github.spannm.jackcess.util.MemFileChannel;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Builder style class for opening/creating a {@link Database}.
 * <p>
 * Simple example usage:
 *
 * <pre>
 * Database db = DatabaseBuilder.open(new File("test.mdb"));
 * </pre>
 * <p>
 * Advanced example usage:
 *
 * <pre>
 * Database db = new DatabaseBuilder(new File("test.mdb"))
 *     .setReadOnly(true)
 *     .open();
 * </pre>
 */
public class DatabaseBuilder {
    /** the file name of the mdb to open/create */
    private Path                              _mdbFile;
    /** whether or not to open existing mdb read-only */
    private boolean                           _readOnly;
    /** whether or not to auto-sync writes to the filesystem */
    private boolean                           _autoSync = Database.DEFAULT_AUTO_SYNC;
    /** optional charset for mdbs with unspecified charsets */
    private Charset                           _charset;
    /** optional timezone override for interpreting dates */
    private TimeZone                          _timeZone;
    /** optional CodecProvider for handling encoded mdbs */
    private CodecProvider                     _codecProvider;
    /** FileFormat to use when creating a new mdb */
    private FileFormat               _fileFormat;
    /**
     * optional pre-opened FileChannel, will _not_ be closed by Database close
     */
    private FileChannel                       _channel;
    /** database properties (if any) */
    private Map<String, PropertyMap.Property> _dbProps;
    /** database summary properties (if any) */
    private Map<String, PropertyMap.Property> _summaryProps;
    /** database user-defined (if any) */
    private Map<String, PropertyMap.Property> _userProps;
    /** flag indicating that the system catalog index is borked */
    private boolean                           _ignoreBrokenSystemCatalogIndex;

    public DatabaseBuilder() {
        this((Path) null);
    }

    @Deprecated
    public DatabaseBuilder(File mdbFile) {
        this(toPath(mdbFile));
    }

    @Deprecated
    public DatabaseBuilder(Path mdbFile) {
        _mdbFile = mdbFile;
    }

    /**
     * File containing an existing database for {@link #open} or target file for new database for {@link #create} (in
     * which case, <b>tf this file already exists, it will be overwritten.</b>)
     */
    public DatabaseBuilder withFile(File mdbFile) {
        return withPath(toPath(mdbFile));
    }

    /**
     * File containing an existing database for {@link #open} or target file for new database for {@link #create} (in
     * which case, <b>tf this file already exists, it will be overwritten.</b>)
     */
    public DatabaseBuilder withPath(Path mdbFile) {
        _mdbFile = mdbFile;
        return this;
    }

    /**
     * Sets flag which, iff {@code true}, will force opening file in read-only mode ({@link #open} only).
     */
    public DatabaseBuilder withReadOnly(boolean readOnly) {
        _readOnly = readOnly;
        return this;
    }

    /**
     * Sets whether or not to enable auto-syncing on write. if {@code true}, write operations will be immediately
     * flushed to disk upon completion. This leaves the database in a (fairly) consistent state on each write, but can
     * be very inefficient for many updates. if {@code false}, flushing to disk happens at the jvm's leisure, which can
     * be much faster, but may leave the database in an inconsistent state if failures are encountered during writing.
     * Writes may be flushed at any time using {@link Database#flush}.
     */
    public DatabaseBuilder withAutoSync(boolean autoSync) {
        _autoSync = autoSync;
        return this;
    }

    /**
     * Sets the Charset to use, if {@code null}, uses default.
     */
    public DatabaseBuilder withCharset(Charset charset) {
        _charset = charset;
        return this;
    }

    /**
     * Sets the TimeZone to use for interpreting dates, if {@code null}, uses default
     */
    public DatabaseBuilder withTimeZone(TimeZone timeZone) {
        _timeZone = timeZone;
        return this;
    }

    /**
     * Sets the CodecProvider for handling page encoding/decoding, may be {@code null} if no special encoding is
     * necessary
     */
    public DatabaseBuilder withCodecProvider(CodecProvider codecProvider) {
        _codecProvider = codecProvider;
        return this;
    }

    /**
     * Sets the version of new database ({@link #create} only).
     */
    public DatabaseBuilder withFileFormat(FileFormat fileFormat) {
        _fileFormat = fileFormat;
        return this;
    }

    /**
     * Sets a pre-opened FileChannel. if provided explicitly, <i>it will not be closed by the Database instance</i>.
     * This allows ultimate control of where the mdb file exists (which may not be on disk, e.g.
     * {@link MemFileChannel}). If provided, the File parameter will be available from {@link Database#getFile}, but
     * otherwise ignored.
     */
    public DatabaseBuilder withChannel(FileChannel channel) {
        _channel = channel;
        return this;
    }

    /**
     * Sets the database property with the given name to the given value. Attempts to determine the type of the property
     * (see {@link PropertyMap#put(String,Object)} for details on determining the property type).
     */
    public DatabaseBuilder putDatabaseProperty(String name, Object value) {
        return putDatabaseProperty(name, null, value);
    }

    /**
     * Sets the database property with the given name and type to the given value.
     */
    public DatabaseBuilder putDatabaseProperty(String name, DataType type,
        Object value) {
        _dbProps = putProperty(_dbProps, name, type, value);
        return this;
    }

    /**
     * Sets the summary database property with the given name to the given value. Attempts to determine the type of the
     * property (see {@link PropertyMap#put(String,Object)} for details on determining the property type).
     */
    public DatabaseBuilder putSummaryProperty(String name, Object value) {
        return putSummaryProperty(name, null, value);
    }

    /**
     * Sets the summary database property with the given name and type to the given value.
     */
    public DatabaseBuilder putSummaryProperty(String name, DataType type,
        Object value) {
        _summaryProps = putProperty(_summaryProps, name, type, value);
        return this;
    }

    /**
     * Sets the user-defined database property with the given name to the given value. Attempts to determine the type of
     * the property (see {@link PropertyMap#put(String,Object)} for details on determining the property type).
     */
    public DatabaseBuilder putUserDefinedProperty(String name, Object value) {
        return putUserDefinedProperty(name, null, value);
    }

    /**
     * Sets the user-defined database property with the given name and type to the given value.
     */
    public DatabaseBuilder putUserDefinedProperty(String name, DataType type,
        Object value) {
        _userProps = putProperty(_userProps, name, type, value);
        return this;
    }

    private static Map<String, PropertyMap.Property> putProperty(
        Map<String, PropertyMap.Property> props, String name, DataType type,
        Object value) {
        if (props == null) {
            props = new HashMap<>();
        }
        props.put(name, PropertyMapImpl.createProperty(name, type, value));
        return props;
    }

    /**
     * Sets flag which, if {@code true}, will make the database ignore the index on the system catalog when looking up
     * tables. This will make table retrieval slower, but can be used to workaround broken indexes.
     */
    public DatabaseBuilder withIgnoreBrokenSystemCatalogIndex(boolean ignore) {
        _ignoreBrokenSystemCatalogIndex = ignore;
        return this;
    }

    /**
     * Opens an existing new Database using the configured information.
     */
    public Database open() throws IOException {
        return DatabaseImpl.open(_mdbFile, _readOnly, _channel, _autoSync, _charset,
            _timeZone, _codecProvider,
            _ignoreBrokenSystemCatalogIndex);
    }

    /**
     * Creates a new Database using the configured information.
     */
    @SuppressWarnings("java:S2095") // suppress sonarcloud warning regarding try-with-resources
    public Database create() throws IOException {
        Database db = DatabaseImpl.create(_fileFormat, _mdbFile, _channel, _autoSync, _charset, _timeZone);
        if (_dbProps != null) {
            PropertyMap props = db.getDatabaseProperties();
            props.putAll(_dbProps.values());
            props.save();
        }
        if (_summaryProps != null) {
            PropertyMap props = db.getSummaryProperties();
            props.putAll(_summaryProps.values());
            props.save();
        }
        if (_userProps != null) {
            PropertyMap props = db.getUserDefinedProperties();
            props.putAll(_userProps.values());
            props.save();
        }
        return db;
    }

    /**
     * Open an existing Database. If the existing file is not writeable, the file will be opened read-only. Auto-syncing
     * is enabled for the returned Database.
     *
     * @param mdbFile File containing the database
     *
     * @see DatabaseBuilder for more flexible Database opening
     */
    public static Database open(File mdbFile) throws IOException {
        return new DatabaseBuilder().withFile(mdbFile).open();
    }

    /**
     * Open an existing Database. If the existing file is not writeable, the file will be opened read-only. Auto-syncing
     * is enabled for the returned Database.
     *
     * @param mdbFile File containing the database
     *
     * @see DatabaseBuilder for more flexible Database opening
     */
    public static Database open(Path mdbFile) throws IOException {
        return new DatabaseBuilder(mdbFile).open();
    }

    /**
     * Create a new Database for the given fileFormat
     *
     * @param fileFormat version of new database.
     * @param mdbFile Location to write the new database to. <b>If this file already exists, it will be overwritten.</b>
     *
     * @see DatabaseBuilder for more flexible Database creation
     */
    public static Database create(FileFormat fileFormat, File mdbFile) throws IOException {
        return new DatabaseBuilder().withFile(mdbFile).withFileFormat(fileFormat).create();
    }

    /**
     * Returns a SimpleDateFormat for the given format string which is configured with a compatible Calendar instance
     * (see {@link #toCompatibleCalendar}).
     */
    public static SimpleDateFormat createDateFormat(String formatStr) {
        SimpleDateFormat sdf = new SimpleDateFormat(formatStr);
        toCompatibleCalendar(sdf.getCalendar());
        return sdf;
    }

    /**
     * Ensures that the given {@link Calendar} is configured to be compatible with how Access handles dates.
     * Specifically, alters the gregorian change (the java default gregorian change switches to julian dates for dates
     * pre 1582-10-15, whereas Access uses
     * <a href="https://en.wikipedia.org/wiki/Proleptic_Gregorian_calendar">proleptic gregorian dates</a>).
     */
    public static Calendar toCompatibleCalendar(Calendar cal) {
        if (cal instanceof GregorianCalendar) {
            ((GregorianCalendar) cal).setGregorianChange(new Date(Long.MIN_VALUE));
        }
        return cal;
    }

    /**
     * Convenience method for constructing a DatabaseBuilder.
     */
    public static DatabaseBuilder newDatabase() {
        return new DatabaseBuilder();
    }

    /**
     * Convenience method for constructing a DatabaseBuilder.
     */
    public static DatabaseBuilder newDatabase(Path path) {
        return new DatabaseBuilder(path);
    }

    /**
     * Convenience method for constructing a DatabaseBuilder.
     */
    public static DatabaseBuilder newDatabase(File file) {
        return new DatabaseBuilder().withFile(file);
    }

    /**
     * Convenience method for constructing a TableBuilder.
     */
    public static TableBuilder newTable(String name) {
        return new TableBuilder(name);
    }

    /**
     * Convenience method for constructing a TableBuilder.
     */
    public static TableBuilder newTable(String name, boolean escapeIdentifiers) {
        return new TableBuilder(name, escapeIdentifiers);
    }

    /**
     * Convenience method for constructing a ColumnBuilder.
     */
    public static ColumnBuilder newColumn(String name) {
        return new ColumnBuilder(name);
    }

    /**
     * Convenience method for constructing a TableBuilder.
     */
    public static ColumnBuilder newColumn(String name, DataType type) {
        return new ColumnBuilder(name, type);
    }

    /**
     * Convenience method for constructing an IndexBuilder.
     */
    public static IndexBuilder newIndex(String name) {
        return new IndexBuilder(name);
    }

    /**
     * Convenience method for constructing a primary key IndexBuilder.
     */
    public static IndexBuilder newPrimaryKey(String... colNames) {
        return new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME)
            .withColumns(colNames)
            .withPrimaryKey();
    }

    /**
     * Convenience method for constructing a RelationshipBuilder.
     */
    public static RelationshipBuilder newRelationship(
        String fromTable, String toTable) {
        return new RelationshipBuilder(fromTable, toTable);
    }

    /**
     * Convenience method for constructing a RelationshipBuilder.
     */
    public static RelationshipBuilder newRelationship(
        Table fromTable, Table toTable) {
        return new RelationshipBuilder(fromTable, toTable);
    }

    private static Path toPath(File file) {
        return file != null ? file.toPath() : null;
    }
}
