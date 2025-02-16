package io.github.spannm.jackcess.util;

import io.github.spannm.jackcess.impl.OleUtil;

import java.io.*;
import java.sql.Blob;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Extensions of the Blob interface with additional functionality for working with the OLE content from an access
 * database. The ole data type in access has a wide range of functionality (including wrappers with nested wrappers with
 * nested filesystems!), and jackcess only supports a small portion of it. That said, jackcess should support the bulk
 * of the common functionality.
 * <p>
 * The main Blob methods will interact with the <i>entire</i> OLE field data which, in most cases, contains additional
 * wrapper information. In order to access the ultimate "content" contained within the OLE data, the {@link #getContent}
 * method should be used. The type of this content may be a variety of formats, so additional sub-interfaces are
 * available to interact with it. The most specific sub-interface can be determined by the {@link ContentType} of the
 * Content.
 * <p>
 * Once an OleBlob is no longer useful, <i>it should be closed</i> using {@link #free} or {@link #close} methods (after
 * which, the instance will no longer be functional).
 * <p>
 * Note, the OleBlob implementation is read-only (through the interface). In order to modify blob contents, create a new
 * OleBlob instance using {@link OleBlob.Builder} and write it to the access database.
 * <p>
 * <b>Example for interpreting an existing OLE field:</b>
 *
 * <pre>
 *   OleBlob oleBlob = null;
 *   try {
 *     oleBlob = row.getBlob("MyOleColumn");
 *     Content content = oleBlob.getContent()
 *     if(content.getType() == OleBlob.ContentType.SIMPLE_PACKAGE) {
 *       FileOutputStream out = ...;
 *       ((SimplePackageContent)content).writeTo(out);
 *       out.closee();
 *     }
 *   } finally {
 *     if(oleBlob != null) { oleBlob.close(); }
 *   }
 * </pre>
 * <p>
 * <b>Example for creating new, embedded ole data:</b>
 *
 * <pre>
 * OleBlob oleBlob = null;
 * try {
 *     oleBlob = new OleBlob.Builder()
 *         .setSimplePackage(new File("some_data.txt"))
 *         .toBlob();
 *     db.addRow(1, oleBlob);
 * } finally {
 *     if (oleBlob != null) {
 *         oleBlob.close();
 *     }
 * }
 * </pre>
 * <p>
 * <b>Example for creating new, linked ole data:</b>
 *
 * <pre>
 * OleBlob oleBlob = null;
 * try {
 *     oleBlob = new OleBlob.Builder()
 *         .setLink(new File("some_data.txt"))
 *         .toBlob();
 *     db.addRow(1, oleBlob);
 * } finally {
 *     if (oleBlob != null) {
 *         oleBlob.close();
 *     }
 * }
 * </pre>
 */
public interface OleBlob extends Blob, Closeable {
    /**
     * Enum describing the types of blob contents which are currently supported/understood
     */
    enum ContentType {
        /**
         * the blob contents are a link (file path) to some external content. Content will be an instance of LinkContent
         */
        LINK,
        /**
         * the blob contents are a simple wrapper around some embedded content and related file names/paths. Content
         * will be an instance SimplePackageContent
         */
        SIMPLE_PACKAGE,
        /**
         * the blob contents are a complex embedded data known as compound storage (aka OLE2). Working with compound
         * storage requires the optional POI library. Content will be an instance of CompoundContent. If the POI library
         * is not available on the classpath, then compound storage data will instead be returned as type
         * {@link #OTHER}.
         */
        COMPOUND_STORAGE,
        /**
         * the top-level blob wrapper is understood, but the nested blob contents are unknown, probably just some
         * embedded content. Content will be an instance of OtherContent
         */
        OTHER,
        /**
         * the top-level blob wrapper is not understood (this may not be a valid ole instance). Content will simply be
         * an instance of Content (the data can be accessed from the main blob instance)
         */
        UNKNOWN
    }

    /**
     * Writes the entire raw blob data to the given stream (this is the access db internal format, which includes all
     * wrapper information).
     *
     * @param out stream to which the blob will be written
     */
    void writeTo(OutputStream out) throws IOException;

    /**
     * Returns the decoded form of the blob contents, if understandable.
     */
    Content getContent() throws IOException;

    interface Content {
        /**
         * Returns the type of this content.
         */
        ContentType getType();

        /**
         * Returns the blob which owns this content.
         */
        OleBlob getBlob();
    }

    /**
     * Intermediate sub-interface for Content which has a nested package.
     */
    interface PackageContent extends Content {
        String getPrettyName();

        String getClassName();

        String getTypeName();
    }

    /**
     * Intermediate sub-interface for Content which has embedded content.
     */
    interface EmbeddedContent extends Content {
        long length();

        InputStream getStream() throws IOException;

        void writeTo(OutputStream out) throws IOException;
    }

    /**
     * Sub-interface for Content which has the {@link ContentType#LINK} type. The actual content is external to the
     * access database and can be found at {@link #getLinkPath}.
     */
    interface LinkContent extends PackageContent {
        String getFileName();

        String getLinkPath();

        String getFilePath();

        InputStream getLinkStream() throws IOException;
    }

    /**
     * Sub-interface for Content which has the {@link ContentType#SIMPLE_PACKAGE} type. The actual content is embedded
     * within the access database (but the original file source path can also be found at {@link #getFilePath}).
     */
    interface SimplePackageContent
        extends PackageContent, EmbeddedContent {
        String getFileName();

        String getFilePath();

        String getLocalFilePath();
    }

    /**
     * Sub-interface for Content which has the {@link ContentType#COMPOUND_STORAGE} type. Compound storage is a complex
     * embedding format also known as OLE2. In some situations (mostly non-microsoft office file formats) the actual
     * content is available from the {@link #getContentsEntry} method (if {@link #hasContentsEntry} returns
     * {@code true}). In other situations (e.g. microsoft office file formats), the actual content is most or all of the
     * compound content (but retrieving the final file may be a complex operation beyond the scope of jackcess). Note
     * that the CompoundContent type will only be available if the POI library is in the classpath, otherwise compound
     * content will be returned as OtherContent.
     */
    interface CompoundContent extends PackageContent, EmbeddedContent,
        Iterable<CompoundContent.Entry> {
        Entry getEntry(String entryName) throws IOException;

        boolean hasContentsEntry() throws IOException;

        Entry getContentsEntry() throws IOException;

        /**
         * @return a Stream using the default Iterator.
         */
        default Stream<CompoundContent.Entry> stream() {
            return StreamSupport.stream(spliterator(), false);
        }

        /**
         * A document entry in the compound storage.
         */
        interface Entry extends EmbeddedContent {
            String getName();

            /**
             * Returns the CompoundContent which owns this entry.
             */
            CompoundContent getParent();
        }
    }

    /**
     * Sub-interface for Content which has the {@link ContentType#OTHER} type. This may be a simple embedded file or
     * some other, currently not understood complex type.
     */
    interface OtherContent extends PackageContent, EmbeddedContent {
    }

    /**
     * Builder style class for constructing an OleBlob. See {@link OleBlob} for example usage.
     */
    class Builder {
        public static final String PACKAGE_PRETTY_NAME = "Packager Shell Object";
        public static final String PACKAGE_TYPE_NAME   = "Package";

        private ContentType        _type;
        private byte[]             _bytes;
        private InputStream        _stream;
        private long               _contentLen;
        private String             _fileName;
        private String             _filePath;
        private String             _prettyName;
        private String             _className;
        private String             _typeName;

        public ContentType getType() {
            return _type;
        }

        public byte[] getBytes() {
            return _bytes;
        }

        public InputStream getStream() {
            return _stream;
        }

        public long getContentLength() {
            return _contentLen;
        }

        public String getFileName() {
            return _fileName;
        }

        public String getFilePath() {
            return _filePath;
        }

        public String getPrettyName() {
            return _prettyName;
        }

        public String getClassName() {
            return _className;
        }

        public String getTypeName() {
            return _typeName;
        }

        public Builder withSimplePackageBytes(byte[] bytes) {
            _bytes = bytes;
            _contentLen = bytes.length;
            setDefaultPackageType();
            _type = ContentType.SIMPLE_PACKAGE;
            return this;
        }

        public Builder withSimplePackageStream(InputStream in, long length) {
            _stream = in;
            _contentLen = length;
            setDefaultPackageType();
            _type = ContentType.SIMPLE_PACKAGE;
            return this;
        }

        public Builder withSimplePackageFileName(String fileName) {
            _fileName = fileName;
            setDefaultPackageType();
            _type = ContentType.SIMPLE_PACKAGE;
            return this;
        }

        public Builder withSimplePackageFilePath(String filePath) {
            _filePath = filePath;
            setDefaultPackageType();
            _type = ContentType.SIMPLE_PACKAGE;
            return this;
        }

        public Builder withSimplePackage(File f) throws FileNotFoundException {
            _fileName = f.getName();
            _filePath = f.getAbsolutePath();
            return withSimplePackageStream(new FileInputStream(f), f.length());
        }

        public Builder withLinkFileName(String fileName) {
            _fileName = fileName;
            setDefaultPackageType();
            _type = ContentType.LINK;
            return this;
        }

        public Builder withLinkPath(String link) {
            _filePath = link;
            setDefaultPackageType();
            _type = ContentType.LINK;
            return this;
        }

        public Builder withLink(File f) {
            _fileName = f.getName();
            _filePath = f.getAbsolutePath();
            setDefaultPackageType();
            _type = ContentType.LINK;
            return this;
        }

        private void setDefaultPackageType() {
            if (_prettyName == null) {
                _prettyName = PACKAGE_PRETTY_NAME;
            }
            if (_className == null) {
                _className = PACKAGE_TYPE_NAME;
            }
        }

        public Builder withOtherBytes(byte[] bytes) {
            _bytes = bytes;
            _contentLen = bytes.length;
            _type = ContentType.OTHER;
            return this;
        }

        public Builder withOtherStream(InputStream in, long length) {
            _stream = in;
            _contentLen = length;
            _type = ContentType.OTHER;
            return this;
        }

        public Builder withOther(File f) throws FileNotFoundException {
            return withOtherStream(new FileInputStream(f), f.length());
        }

        public Builder withPackagePrettyName(String prettyName) {
            _prettyName = prettyName;
            return this;
        }

        public Builder withPackageClassName(String className) {
            _className = className;
            return this;
        }

        public Builder withPackageTypeName(String typeName) {
            _typeName = typeName;
            return this;
        }

        public OleBlob toBlob() throws IOException {
            return OleUtil.createBlob(this);
        }

        public static OleBlob fromInternalData(byte[] bytes) {
            return OleUtil.parseBlob(bytes);
        }
    }
}
