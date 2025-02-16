package io.github.spannm.jackcess.complex;

import io.github.spannm.jackcess.DateTimeType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Complex value corresponding to an attachment.
 */
public interface Attachment extends ComplexValue {
    byte[] getFileData() throws IOException;

    void setFileData(byte[] data);

    byte[] getEncodedFileData() throws IOException;

    void setEncodedFileData(byte[] data);

    String getFileName();

    void setFileName(String fileName);

    String getFileUrl();

    void setFileUrl(String fileUrl);

    String getFileType();

    void setFileType(String fileType);

    /**
     * @deprecated see {@link DateTimeType} for details
     */
    @Deprecated
    Date getFileTimeStamp();

    /**
     * @deprecated see {@link DateTimeType} for details
     */
    @Deprecated
    void setFileTimeStamp(Date fileTimeStamp);

    LocalDateTime getFileLocalTimeStamp();

    void setFileLocalTimeStamp(LocalDateTime fileTimeStamp);

    Object getFileTimeStampObject();

    Integer getFileFlags();

    void setFileFlags(Integer fileFlags);
}
