package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.impl.OleUtil.CompoundPackageFactory;
import io.github.spannm.jackcess.impl.OleUtil.ContentImpl;
import io.github.spannm.jackcess.impl.OleUtil.EmbeddedPackageContentImpl;
import io.github.spannm.jackcess.impl.OleUtil.OleBlobImpl;
import io.github.spannm.jackcess.util.MemFileChannel;
import io.github.spannm.jackcess.util.OleBlob.CompoundContent;
import io.github.spannm.jackcess.util.OleBlob.ContentType;
import io.github.spannm.jackcess.util.ToStringBuilder;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility code for working with OLE data which is in the compound storage format. This functionality relies on the
 * optional POI library.
 * <p>
 * Note that all POI usage is restricted to this file so that the basic ole support in OleUtil can be utilized without
 * requiring POI.
 *
 * @author James Ahlborn
 */
public class CompoundOleUtil implements CompoundPackageFactory {
    private static final String ENTRY_NAME_CHARSET = "UTF-8";
    private static final String ENTRY_SEPARATOR    = "/";
    private static final String CONTENTS_ENTRY     = "CONTENTS";

    static {
        // force a poi class to be loaded to ensure that when this class is
        // loaded, we know that the poi classes are available
        POIFSFileSystem.class.getName();
    }

    public CompoundOleUtil() {
    }

    /**
     * Creates a nes CompoundContent for the given blob information.
     */
    @Override
    public ContentImpl createCompoundPackageContent(
        OleBlobImpl blob, String prettyName, String className, String typeName,
        ByteBuffer blobBb, int dataBlockLen) {
        return new CompoundContentImpl(blob, prettyName, className, typeName,
            blobBb.position(), dataBlockLen);
    }

    /**
     * Gets a DocumentEntry from compound storage based on a fully qualified, encoded entry name.
     *
     * @param entryName fully qualified, encoded entry name
     * @param dir root directory of the compound storage
     *
     * @return the relevant DocumentEntry
     * @throws FileNotFoundException if the entry does not exist
     * @throws IOException if some other io error occurs
     */
    public static DocumentEntry getDocumentEntry(String entryName,
        DirectoryEntry dir) throws IOException {
        // split entry name into individual components and decode them
        List<String> entryNames = new ArrayList<>();
        for (String str : entryName.split(ENTRY_SEPARATOR)) {
            if (str.isEmpty()) {
                continue;
            }
            entryNames.add(decodeEntryName(str));
        }

        DocumentEntry entry = null;
        Iterator<String> iter = entryNames.iterator();
        while (iter.hasNext()) {
            org.apache.poi.poifs.filesystem.Entry tmpEntry = dir.getEntry(iter.next());
            if (tmpEntry instanceof DirectoryEntry) {
                dir = (DirectoryEntry) tmpEntry;
            } else if (!iter.hasNext() && tmpEntry instanceof DocumentEntry) {
                entry = (DocumentEntry) tmpEntry;
            } else {
                break;
            }
        }

        if (entry == null) {
            throw new FileNotFoundException("Could not find document " + entryName);
        }

        return entry;
    }

    private static String encodeEntryName(String name) {
        try {
            return URLEncoder.encode(name, ENTRY_NAME_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String decodeEntryName(String name) {
        try {
            return URLDecoder.decode(name, ENTRY_NAME_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class CompoundContentImpl
        extends EmbeddedPackageContentImpl implements CompoundContent {
        private POIFSFileSystem _fs;

        private CompoundContentImpl(
            OleBlobImpl blob, String prettyName, String className,
            String typeName, int position, int length) {
            super(blob, prettyName, className, typeName, position, length);
        }

        @Override
        public ContentType getType() {
            return ContentType.COMPOUND_STORAGE;
        }

        private POIFSFileSystem getFileSystem() throws IOException {
            if (_fs == null) {
                _fs = new POIFSFileSystem(MemFileChannel.newChannel(getStream(), "r"));
            }
            return _fs;
        }

        @Override
        public Iterator<Entry> iterator() {
            try {
                return getEntries(new ArrayList<>(), getFileSystem().getRoot(),
                    ENTRY_SEPARATOR).iterator();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public EntryImpl getEntry(String entryName) throws IOException {
            return new EntryImpl(entryName,
                getDocumentEntry(entryName, getFileSystem().getRoot()));
        }

        @Override
        public boolean hasContentsEntry() throws IOException {
            return getFileSystem().getRoot().hasEntry(CONTENTS_ENTRY);
        }

        @Override
        public EntryImpl getContentsEntry() throws IOException {
            return getEntry(CONTENTS_ENTRY);
        }

        private List<Entry> getEntries(List<Entry> entries, DirectoryEntry dir,
            String prefix) {
            for (org.apache.poi.poifs.filesystem.Entry entry : dir) {
                if (entry instanceof DirectoryEntry) {
                    // .. recurse into this directory
                    getEntries(entries, (DirectoryEntry) entry, prefix + ENTRY_SEPARATOR);
                } else if (entry instanceof DocumentEntry) {
                    // grab the entry name/details
                    DocumentEntry de = (DocumentEntry) entry;
                    String entryName = prefix + encodeEntryName(entry.getName());
                    entries.add(new EntryImpl(entryName, de));
                }
            }
            return entries;
        }

        @Override
        public void close() {
            ByteUtil.closeQuietly(_fs);
            _fs = null;
            super.close();
        }

        @Override
        public String toString() {
            ToStringBuilder sb = toString(ToStringBuilder.builder(this));

            try {
                sb.append("hasContentsEntry", hasContentsEntry())
                  .append("entries", getEntries(new ArrayList<>(), getFileSystem().getRoot(), ENTRY_SEPARATOR));
            } catch (IOException e) {
                sb.append("entries", "<" + e + ">");
            }

            return sb.toString();
        }

        private final class EntryImpl implements CompoundContent.Entry {
            private final String        _name;
            private final DocumentEntry _docEntry;

            private EntryImpl(String name, DocumentEntry docEntry) {
                _name = name;
                _docEntry = docEntry;
            }

            @Override
            public ContentType getType() {
                return ContentType.UNKNOWN;
            }

            @Override
            public String getName() {
                return _name;
            }

            @Override
            public CompoundContentImpl getParent() {
                return CompoundContentImpl.this;
            }

            @Override
            public OleBlobImpl getBlob() {
                return getParent().getBlob();
            }

            @Override
            public long length() {
                return _docEntry.getSize();
            }

            @Override
            public InputStream getStream() throws IOException {
                return new DocumentInputStream(_docEntry);
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                try (InputStream in = getStream()) {
                    ByteUtil.copy(in, out);
                }
            }

            @Override
            public String toString() {
                return ToStringBuilder.valueBuilder(this)
                    .append("name", _name)
                    .append("length", length())
                    .toString();
            }
        }
    }

}
