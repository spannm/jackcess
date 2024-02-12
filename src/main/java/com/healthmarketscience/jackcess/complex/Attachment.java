/*
Copyright (c) 2011 James Ahlborn

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

package com.healthmarketscience.jackcess.complex;

import com.healthmarketscience.jackcess.DateTimeType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Complex value corresponding to an attachment.
 *
 * @author James Ahlborn
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
