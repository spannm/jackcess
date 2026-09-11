/*
 * Copyright (c) 2016 James Ahlborn
 * Copyright (c) 2024 Markus Spann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.spannm.jackcess.impl.complex;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.Table;
import io.github.spannm.jackcess.complex.*;
import io.github.spannm.jackcess.impl.ByteUtil;
import io.github.spannm.jackcess.impl.ColumnImpl;
import io.github.spannm.jackcess.impl.JetFormat;
import io.github.spannm.jackcess.impl.PageChannel;

import java.io.*;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Complex column info for a column holding 0 or more attachments per row.
 */
public final class AttachmentColumnInfoImpl extends ComplexColumnInfoImpl<Attachment> implements AttachmentColumnInfo {

    /** some file formats which may not be worth re-compressing */
    private static final Set<String> COMPRESSED_FORMATS   = Set.of("jpg", "zip", "gz", "bz2", "z", "7z", "cab", "rar", "mp3", "mpg");

    private static final String      FILE_NAME_COL_NAME   = "FileName";
    private static final String      FILE_TYPE_COL_NAME   = "FileType";

    private static final int         DATA_TYPE_RAW        = 0;
    private static final int         DATA_TYPE_COMPRESSED = 1;

    private static final int         UNKNOWN_HEADER_VAL   = 1;
    private static final int         WRAPPER_HEADER_SIZE  = 8;
    private static final int         CONTENT_HEADER_SIZE  = 12;

    private final Column             fileUrlCol;
    private final Column             fileNameCol;
    private final Column             fileTypeCol;
    private final Column             fileDataCol;
    private final Column             fileTimeStampCol;
    private final Column             fileFlagsCol;

    public AttachmentColumnInfoImpl(Column column, int complexId, Table typeObjTable, Table flatTable) throws IOException {
        super(column, complexId, typeObjTable, flatTable);

        Column foundFileUrlCol = null;
        Column foundFileNameCol = null;
        Column foundFileTypeCol = null;
        Column foundFileDataCol = null;
        Column foundFileTimeStampCol = null;
        Column foundFileFlagsCol = null;

        for (Column col : getTypeColumns()) {
            switch (col.getType()) {
                case TEXT:
                    if (FILE_NAME_COL_NAME.equalsIgnoreCase(col.getName())) {
                        foundFileNameCol = col;
                    } else if (FILE_TYPE_COL_NAME.equalsIgnoreCase(col.getName())) {
                        foundFileTypeCol = col;
                    } else {
                        // if names don't match, assign in order: name, type
                        if (foundFileNameCol == null) {
                            foundFileNameCol = col;
                        } else if (foundFileTypeCol == null) {
                            foundFileTypeCol = col;
                        }
                    }
                    break;
                case LONG:
                    foundFileFlagsCol = col;
                    break;
                case SHORT_DATE_TIME:
                    foundFileTimeStampCol = col;
                    break;
                case OLE:
                    foundFileDataCol = col;
                    break;
                case MEMO:
                    foundFileUrlCol = col;
                    break;
                default:
                    // ignore
            }
        }

        fileUrlCol = foundFileUrlCol;
        fileNameCol = foundFileNameCol;
        fileTypeCol = foundFileTypeCol;
        fileDataCol = foundFileDataCol;
        fileTimeStampCol = foundFileTimeStampCol;
        fileFlagsCol = foundFileFlagsCol;
    }

    public Column getFileUrlColumn() {
        return fileUrlCol;
    }

    public Column getFileNameColumn() {
        return fileNameCol;
    }

    public Column getFileTypeColumn() {
        return fileTypeCol;
    }

    public Column getFileDataColumn() {
        return fileDataCol;
    }

    public Column getFileTimeStampColumn() {
        return fileTimeStampCol;
    }

    public Column getFileFlagsColumn() {
        return fileFlagsCol;
    }

    @Override
    public ComplexDataType getType() {
        return ComplexDataType.ATTACHMENT;
    }

    @Override
    protected AttachmentImpl toValue(ComplexValueForeignKey complexValueFk, Row rawValue) {
        ComplexValue.Id id = getValueId(rawValue);
        String url = (String) getFileUrlColumn().getRowValue(rawValue);
        String name = (String) getFileNameColumn().getRowValue(rawValue);
        String type = (String) getFileTypeColumn().getRowValue(rawValue);
        Integer flags = (Integer) getFileFlagsColumn().getRowValue(rawValue);
        Object ts = getFileTimeStampColumn().getRowValue(rawValue);
        byte[] data = (byte[]) getFileDataColumn().getRowValue(rawValue);

        return new AttachmentImpl(id, complexValueFk, url, name, type, null, ts, flags, data);
    }

    @Override
    protected Object[] asRow(Object[] row, Attachment attachment) throws IOException {
        super.asRow(row, attachment);
        getFileUrlColumn().setRowValue(row, attachment.getFileUrl());
        getFileNameColumn().setRowValue(row, attachment.getFileName());
        getFileTypeColumn().setRowValue(row, attachment.getFileType());
        getFileFlagsColumn().setRowValue(row, attachment.getFileFlags());
        getFileTimeStampColumn().setRowValue(row, attachment.getFileTimeStampObject());
        getFileDataColumn().setRowValue(row, attachment.getEncodedFileData());
        return row;
    }

    public static Attachment newAttachment(byte[] data) {
        return newAttachment(INVALID_FK, data);
    }

    public static Attachment newAttachment(ComplexValueForeignKey complexValueFk, byte[] data) {
        return newAttachment(complexValueFk, null, null, null, data, null, null);
    }

    public static Attachment newAttachment(String url, String name, String type, byte[] data, Object timeStamp, Integer flags) {
        return newAttachment(INVALID_FK, url, name, type, data, timeStamp, flags);
    }

    public static Attachment newAttachment(ComplexValueForeignKey complexValueFk, String url, String name, String type, byte[] data, Object timeStamp, Integer flags) {
        return new AttachmentImpl(INVALID_ID, complexValueFk, url, name, type, data, timeStamp, flags, null);
    }

    public static Attachment newEncodedAttachment(byte[] encodedData) {
        return newEncodedAttachment(INVALID_FK, encodedData);
    }

    public static Attachment newEncodedAttachment(ComplexValueForeignKey complexValueFk, byte[] encodedData) {
        return newEncodedAttachment(complexValueFk, null, null, null, encodedData, null, null);
    }

    public static Attachment newEncodedAttachment(String url, String name, String type, byte[] encodedData, Object timeStamp, Integer flags) {
        return newEncodedAttachment(INVALID_FK, url, name, type, encodedData, timeStamp, flags);
    }

    public static Attachment newEncodedAttachment(ComplexValueForeignKey complexValueFk, String url, String name, String type, byte[] encodedData, Object timeStamp, Integer flags) {
        return new AttachmentImpl(INVALID_ID, complexValueFk, url, name, type, null, timeStamp, flags, encodedData);
    }

    private static class AttachmentImpl extends ComplexValueImpl implements Attachment {
        private String  url;
        private String  name;
        private String  type;
        private byte[]  data;
        private Object  timeStamp;
        private Integer flags;
        private byte[]  encodedData;

        private AttachmentImpl(Id id, ComplexValueForeignKey complexValueFk, String url, String name, String type, byte[] data, Object timeStamp, Integer flags, byte[] encodedData) {

            super(id, complexValueFk);
            this.url = url;
            this.name = name;
            this.type = type;
            this.data = data;
            this.timeStamp = timeStamp;
            this.flags = flags;
            this.encodedData = encodedData;
        }

        @Override
        public byte[] getFileData() throws IOException {
            if (data == null && encodedData != null) {
                data = decodeData();
            }
            return data;
        }

        @Override
        public void setFileData(byte[] newData) {
            data = newData;
            encodedData = null;
        }

        @Override
        public byte[] getEncodedFileData() throws IOException {
            if (encodedData == null && data != null) {
                encodedData = encodeData();
            }
            return encodedData;
        }

        @Override
        public void setEncodedFileData(byte[] newEncodedData) {
            encodedData = newEncodedData;
            data = null;
        }

        @Override
        public String getFileName() {
            return name;
        }

        @Override
        public void setFileName(String fileName) {
            name = fileName;
        }

        @Override
        public String getFileUrl() {
            return url;
        }

        @Override
        public void setFileUrl(String fileUrl) {
            url = fileUrl;
        }

        @Override
        public String getFileType() {
            return type;
        }

        @Override
        public void setFileType(String fileType) {
            type = fileType;
        }

        @Override
        @SuppressWarnings("deprecation")
        public Date getFileTimeStamp() {
            return (Date) timeStamp;
        }

        @Override
        @SuppressWarnings("deprecation")
        public void setFileTimeStamp(Date fileTimeStamp) {
            timeStamp = fileTimeStamp;
        }

        @Override
        public LocalDateTime getFileLocalTimeStamp() {
            return (LocalDateTime) timeStamp;
        }

        @Override
        public void setFileLocalTimeStamp(LocalDateTime fileTimeStamp) {
            timeStamp = fileTimeStamp;
        }

        @Override
        public Object getFileTimeStampObject() {
            return timeStamp;
        }

        @Override
        public Integer getFileFlags() {
            return flags;
        }

        @Override
        public void setFileFlags(Integer fileFlags) {
            flags = fileFlags;
        }

        @Override
        public void update() throws IOException {
            getComplexValueForeignKey().updateAttachment(this);
        }

        @Override
        public void delete() throws IOException {
            getComplexValueForeignKey().deleteAttachment(this);
        }

        @Override
        public String toString() {
            String dataStr = null;
            try {
                dataStr = ByteUtil.toHexString(getFileData());
            } catch (IOException _ex) {
                dataStr = _ex.toString();
            }

            return "Attachment(" + getComplexValueForeignKey() + "," + getId() + ") "
                + getFileUrl() + ", " + getFileName() + ", " + getFileType()
                + ", " + getFileTimeStampObject() + ", " + getFileFlags() + ", " + dataStr;
        }

        /**
         * Decodes the raw attachment file data to get the _actual_ content.
         */
        @SuppressWarnings("PMD.UseTryWithResources")
        private byte[] decodeData() throws IOException {

            if (encodedData.length < WRAPPER_HEADER_SIZE) {
                // nothing we can do
                throw new IOException("Unknown encoded attachment data format");
            }

            // read initial header info
            ByteBuffer bb = PageChannel.wrap(encodedData);
            int typeFlag = bb.getInt();
            int dataLen = bb.getInt();

            DataInputStream contentStream = null;
            try {
                InputStream bin = new ByteArrayInputStream(encodedData, WRAPPER_HEADER_SIZE, encodedData.length - WRAPPER_HEADER_SIZE);

                if (typeFlag == DATA_TYPE_COMPRESSED) {
                    // actual content is deflate compressed
                    bin = new InflaterInputStream(bin);
                } else if (typeFlag != DATA_TYPE_RAW) {
                    throw new IOException("Unknown encoded attachment data type " + typeFlag);
                }

                contentStream = new DataInputStream(bin);

                // header is an unknown flag followed by the "file extension" of the
                // data (no clue why we need that again since it's already a separate
                // field in the attachment table). just skip all of it
                byte[] tmpBytes = new byte[4];
                contentStream.readFully(tmpBytes);
                int headerLen = PageChannel.wrap(tmpBytes).getInt();
                ByteUtil.skipFully(contentStream, headerLen - 4);

                // calculate actual data length and read it (note, header length
                // includes the bytes for the length)
                tmpBytes = new byte[dataLen - headerLen];
                contentStream.readFully(tmpBytes);

                return tmpBytes;

            } finally {
                ByteUtil.closeQuietly(contentStream);
            }
        }

        /**
         * Encodes the actual attachment file data to get the raw, stored format.
         */
        @SuppressWarnings("PMD.UseTryWithResources")
        private byte[] encodeData() throws IOException {

            // possibly compress data based on file type
            String lcType = type != null ? type.toLowerCase() : "";
            boolean shouldCompress = !COMPRESSED_FORMATS.contains(lcType);

            // encode extension, which ends w/ a null byte
            lcType += '\0';
            ByteBuffer typeBytes = ColumnImpl.encodeUncompressedText(lcType, JetFormat.VERSION_12.CHARSET);
            int headerLen = typeBytes.remaining() + CONTENT_HEADER_SIZE;

            int dataLen = data.length;
            ByteUtil.ByteStream dataStream = new ByteUtil.ByteStream(WRAPPER_HEADER_SIZE + headerLen + dataLen);

            // write the wrapper header info
            ByteBuffer bb = PageChannel.wrap(dataStream.getBytes());
            bb.putInt(shouldCompress ? DATA_TYPE_COMPRESSED : DATA_TYPE_RAW);
            bb.putInt(dataLen + headerLen);
            dataStream.skip(WRAPPER_HEADER_SIZE);

            OutputStream contentStream = dataStream;
            Deflater deflater = null;
            try {

                if (shouldCompress) {
                    contentStream = new DeflaterOutputStream(contentStream, deflater = new Deflater(3));
                }

                // write the header w/ the file extension
                byte[] tmpBytes = new byte[CONTENT_HEADER_SIZE];
                PageChannel.wrap(tmpBytes)
                    .putInt(headerLen)
                    .putInt(UNKNOWN_HEADER_VAL)
                    .putInt(lcType.length());
                contentStream.write(tmpBytes);
                contentStream.write(typeBytes.array(), 0, typeBytes.remaining());

                // write the _actual_ contents
                contentStream.write(data);
                contentStream.close();
                contentStream = null;

                return dataStream.toByteArray();

            } finally {
                ByteUtil.closeQuietly(contentStream);
                if (deflater != null) {
                    deflater.end();
                }
            }
        }
    }

}
