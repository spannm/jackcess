package io.github.spannm.jackcess.impl.complex;

import io.github.spannm.jackcess.Column;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.DateTimeType;
import io.github.spannm.jackcess.Row;
import io.github.spannm.jackcess.complex.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Value which is returned for a complex column. This value corresponds to a foreign key in a secondary table which
 * contains the actual complex data for this row (which could be 0 or more complex values for a given row). This class
 * contains various convenience methods for interacting with the actual complex values.
 * <p>
 * This class will cache the associated complex values returned from one of the lookup methods. The various modification
 * methods will clear this cache automatically. The {@link #reset} method may be called manually to clear this internal
 * cache.
 *
 * @author James Ahlborn
 */
public class ComplexValueForeignKeyImpl extends ComplexValueForeignKey {
    private static final long                      serialVersionUID = 20110805L;

    private final transient Column                 _column;
    private final int                              _value;
    private transient List<? extends ComplexValue> _values;

    public ComplexValueForeignKeyImpl(Column column, int value) {
        _column = column;
        _value = value;
    }

    @Override
    public int get() {
        return _value;
    }

    @Override
    public Column getColumn() {
        return _column;
    }

    @Override
    public ComplexDataType getComplexType() {
        return getComplexInfo().getType();
    }

    protected ComplexColumnInfo<? extends ComplexValue> getComplexInfo() {
        return _column.getComplexInfo();
    }

    protected VersionHistoryColumnInfo getVersionInfo() {
        return (VersionHistoryColumnInfo) getComplexInfo();
    }

    protected AttachmentColumnInfo getAttachmentInfo() {
        return (AttachmentColumnInfo) getComplexInfo();
    }

    protected MultiValueColumnInfo getMultiValueInfo() {
        return (MultiValueColumnInfo) getComplexInfo();
    }

    protected UnsupportedColumnInfo getUnsupportedInfo() {
        return (UnsupportedColumnInfo) getComplexInfo();
    }

    @Override
    public int countValues() throws IOException {
        return getComplexInfo().countValues(get());
    }

    public List<Row> getRawValues() throws IOException {
        return getComplexInfo().getRawValues(get());
    }

    @Override
    public List<? extends ComplexValue> getValues() throws IOException {
        if (_values == null) {
            _values = getComplexInfo().getValues(this);
        }
        return _values;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Version> getVersions() throws IOException {
        if (getComplexType() != ComplexDataType.VERSION_HISTORY) {
            throw new UnsupportedOperationException();
        }
        return (List<Version>) getValues();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Attachment> getAttachments() throws IOException {
        if (getComplexType() != ComplexDataType.ATTACHMENT) {
            throw new UnsupportedOperationException();
        }
        return (List<Attachment>) getValues();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<SingleValue> getMultiValues() throws IOException {
        if (getComplexType() != ComplexDataType.MULTI_VALUE) {
            throw new UnsupportedOperationException();
        }
        return (List<SingleValue>) getValues();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<UnsupportedValue> getUnsupportedValues() throws IOException {
        if (getComplexType() != ComplexDataType.UNSUPPORTED) {
            throw new UnsupportedOperationException();
        }
        return (List<UnsupportedValue>) getValues();
    }

    @Override
    public void reset() {
        // discard any cached values
        _values = null;
    }

    @Override
    public Version addVersion(String value) throws IOException {
        return addVersionImpl(value, now());
    }

    @Override
    public Version addVersion(String value, Date modifiedDate) throws IOException {
        return addVersionImpl(value, modifiedDate);
    }

    @Override
    public Version addVersion(String value, LocalDateTime modifiedDate) throws IOException {
        return addVersionImpl(value, modifiedDate);
    }

    private Version addVersionImpl(String value, Object modifiedDate) throws IOException {
        reset();
        Version v = VersionHistoryColumnInfoImpl.newVersion(this, value, modifiedDate);
        getVersionInfo().addValue(v);
        return v;
    }

    @Override
    public Attachment addAttachment(byte[] data) throws IOException {
        return addAttachmentImpl(null, null, null, data, null, null);
    }

    @Override
    public Attachment addAttachment(
        String url, String name, String type, byte[] data,
        Date timeStamp, Integer flags) throws IOException {
        return addAttachmentImpl(url, name, type, data, timeStamp, flags);
    }

    @Override
    public Attachment addAttachment(
        String url, String name, String type, byte[] data,
        LocalDateTime timeStamp, Integer flags) throws IOException {
        return addAttachmentImpl(url, name, type, data, timeStamp, flags);
    }

    private Attachment addAttachmentImpl(
        String url, String name, String type, byte[] data,
        Object timeStamp, Integer flags) throws IOException {
        reset();
        Attachment a = AttachmentColumnInfoImpl.newAttachment(
            this, url, name, type, data, timeStamp, flags);
        getAttachmentInfo().addValue(a);
        return a;
    }

    @Override
    public Attachment addEncodedAttachment(byte[] encodedData) throws IOException {
        return addEncodedAttachmentImpl(null, null, null, encodedData, null, null);
    }

    @Override
    public Attachment addEncodedAttachment(
        String url, String name, String type, byte[] encodedData,
        Date timeStamp, Integer flags) throws IOException {
        return addEncodedAttachmentImpl(url, name, type, encodedData, timeStamp,
            flags);
    }

    @Override
    public Attachment addEncodedAttachment(
        String url, String name, String type, byte[] encodedData,
        LocalDateTime timeStamp, Integer flags) throws IOException {
        return addEncodedAttachmentImpl(url, name, type, encodedData, timeStamp,
            flags);
    }

    private Attachment addEncodedAttachmentImpl(
        String url, String name, String type, byte[] encodedData,
        Object timeStamp, Integer flags) throws IOException {
        reset();
        Attachment a = AttachmentColumnInfoImpl.newEncodedAttachment(
            this, url, name, type, encodedData, timeStamp, flags);
        getAttachmentInfo().addValue(a);
        return a;
    }

    @Override
    public Attachment updateAttachment(Attachment attachment) throws IOException {
        reset();
        getAttachmentInfo().updateValue(attachment);
        return attachment;
    }

    @Override
    public Attachment deleteAttachment(Attachment attachment) throws IOException {
        reset();
        getAttachmentInfo().deleteValue(attachment);
        return attachment;
    }

    @Override
    public SingleValue addMultiValue(Object value) throws IOException {
        reset();
        SingleValue v = MultiValueColumnInfoImpl.newSingleValue(this, value);
        getMultiValueInfo().addValue(v);
        return v;
    }

    @Override
    public SingleValue updateMultiValue(SingleValue value) throws IOException {
        reset();
        getMultiValueInfo().updateValue(value);
        return value;
    }

    @Override
    public SingleValue deleteMultiValue(SingleValue value) throws IOException {
        reset();
        getMultiValueInfo().deleteValue(value);
        return value;
    }

    @Override
    public UnsupportedValue addUnsupportedValue(Map<String, ?> values) throws IOException {
        reset();
        UnsupportedValue v = UnsupportedColumnInfoImpl.newValue(this, values);
        getUnsupportedInfo().addValue(v);
        return v;
    }

    @Override
    public UnsupportedValue updateUnsupportedValue(UnsupportedValue value) throws IOException {
        reset();
        getUnsupportedInfo().updateValue(value);
        return value;
    }

    @Override
    public UnsupportedValue deleteUnsupportedValue(UnsupportedValue value) throws IOException {
        reset();
        getUnsupportedInfo().deleteValue(value);
        return value;
    }

    @Override
    public void deleteAllValues() throws IOException {
        reset();
        getComplexInfo().deleteAllValues(this);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o)
            && _column == ((ComplexValueForeignKeyImpl) o)._column;
    }

    @Override
    public int hashCode() {
        return super.hashCode() + Objects.hash(_column);
    }

    private Object now() {
        Database db = getColumn().getDatabase();
        if (db.getDateTimeType() == DateTimeType.DATE) {
            return new Date();
        }
        return LocalDateTime.now(db.getZoneId());
    }
}
