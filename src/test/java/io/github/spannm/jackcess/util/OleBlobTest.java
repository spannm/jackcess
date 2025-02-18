package io.github.spannm.jackcess.util;

import static io.github.spannm.jackcess.test.Basename.BLOB;

import io.github.spannm.jackcess.*;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.complex.Attachment;
import io.github.spannm.jackcess.impl.ByteUtil;
import io.github.spannm.jackcess.impl.CompoundOleUtil;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.*;
import java.nio.file.Files;

class OleBlobTest extends AbstractBaseTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testCreateBlob(FileFormat fileFormat) throws IOException {
        File sampleFile = new File(DIR_TEST_DATA, "sample-input.tab");
        String sampleFilePath = sampleFile.getAbsolutePath();
        String sampleFileName = sampleFile.getName();
        byte[] sampleFileBytes =  Files.readAllBytes(sampleFile.toPath());

        try (Database db = createDbMem(fileFormat)) {
            Table t = new TableBuilder("TestOle")
                .addColumn(new ColumnBuilder("id", DataType.LONG))
                .addColumn(new ColumnBuilder("ole", DataType.OLE))
                .toTable(db);

            try (OleBlob blob = t.newBlob().withSimplePackage(sampleFile).toBlob()) {
                t.addRow(1, blob);
            }

            try (OleBlob blob = t.newBlob().withLink(sampleFile).toBlob()) {
                t.addRow(2, blob);
            }

            try (OleBlob blob = t.newBlob()
                    .withPackagePrettyName("Text File")
                    .withPackageClassName("Text.File")
                    .withPackageTypeName("TextFile")
                    .withOtherBytes(sampleFileBytes)
                    .toBlob()) {
                t.addRow(3, blob);
            }

            for (Row row : t) {

                try (OleBlob blob = row.getBlob("ole")) {
                    OleBlob.Content content = blob.getContent();
                    assertSame(blob, content.getBlob());
                    assertSame(content, blob.getContent());

                    switch (row.getInt("id")) {
                        case 1:
                            assertEquals(OleBlob.ContentType.SIMPLE_PACKAGE, content.getType());
                            OleBlob.SimplePackageContent spc = (OleBlob.SimplePackageContent) content;
                            assertEquals(sampleFilePath, spc.getFilePath());
                            assertEquals(sampleFilePath, spc.getLocalFilePath());
                            assertEquals(sampleFileName, spc.getFileName());
                            assertEquals(OleBlob.Builder.PACKAGE_PRETTY_NAME, spc.getPrettyName());
                            assertEquals(OleBlob.Builder.PACKAGE_TYPE_NAME, spc.getTypeName());
                            assertEquals(OleBlob.Builder.PACKAGE_TYPE_NAME, spc.getClassName());
                            assertEquals(sampleFileBytes.length, spc.length());
                            assertArrayEquals(sampleFileBytes, readToByteArray(spc.getStream(), spc.length()));
                            break;

                        case 2:
                            OleBlob.LinkContent lc = (OleBlob.LinkContent) content;
                            assertEquals(OleBlob.ContentType.LINK, lc.getType());
                            assertEquals(sampleFilePath, lc.getLinkPath());
                            assertEquals(sampleFilePath, lc.getFilePath());
                            assertEquals(sampleFileName, lc.getFileName());
                            assertEquals(OleBlob.Builder.PACKAGE_PRETTY_NAME, lc.getPrettyName());
                            assertEquals(OleBlob.Builder.PACKAGE_TYPE_NAME, lc.getTypeName());
                            assertEquals(OleBlob.Builder.PACKAGE_TYPE_NAME, lc.getClassName());
                            break;

                        case 3:
                            OleBlob.OtherContent oc = (OleBlob.OtherContent) content;
                            assertEquals(OleBlob.ContentType.OTHER, oc.getType());
                            assertEquals("Text File", oc.getPrettyName());
                            assertEquals("Text.File", oc.getClassName());
                            assertEquals("TextFile", oc.getTypeName());
                            assertEquals(sampleFileBytes.length, oc.length());
                            assertArrayEquals(sampleFileBytes, readToByteArray(oc.getStream(), oc.length()));
                            break;
                        default:
                            throw new JackcessRuntimeException("Unexpected id " + row);
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(BLOB)
    void testReadBlob(TestDb testDb) throws IOException {
        try (Database db = testDb.open()) {
            Table t = db.getTable("Table1");

            for (Row row : t) {

                try (OleBlob oleBlob = row.getBlob("ole_data")) {
                    String name = row.getString("name");
                    OleBlob.Content content = oleBlob.getContent();
                    Attachment attach = null;
                    if (content.getType() != OleBlob.ContentType.LINK) {
                        attach = row.getForeignKey("attach_data").getAttachments().get(0);
                    }

                    switch (content.getType()) {
                        case LINK:
                            OleBlob.LinkContent lc = (OleBlob.LinkContent) content;
                            if ("test_link".equals(name)) {
                                assertEquals("Z:\\jackcess_test\\ole\\test_data.txt", lc.getLinkPath());
                            } else {
                                assertEquals("Z:\\jackcess_test\\ole\\test_datau2.txt", lc.getLinkPath());
                            }
                            break;

                        case SIMPLE_PACKAGE:
                            OleBlob.SimplePackageContent spc = (OleBlob.SimplePackageContent) content;
                            byte[] packageBytes = readToByteArray(spc.getStream(), spc.length());
                            assertArrayEquals(attach.getFileData(), packageBytes);
                            break;

                        case COMPOUND_STORAGE:
                            OleBlob.CompoundContent cc = (OleBlob.CompoundContent) content;
                            if (cc.hasContentsEntry()) {
                                OleBlob.CompoundContent.Entry entry = cc.getContentsEntry();
                                byte[] entryBytes = readToByteArray(entry.getStream(), entry.length());
                                assertArrayEquals(attach.getFileData(), entryBytes);
                            } else {

                                if ("test_word.doc".equals(name)) {
                                    checkCompoundEntries(cc,
                                        "/%02OlePres000", 466,
                                        "/WordDocument", 4096,
                                        "/%05SummaryInformation", 4096,
                                        "/%05DocumentSummaryInformation", 4096,
                                        "/%03AccessObjSiteData", 56,
                                        "/%02OlePres001", 1620,
                                        "/1Table", 6380,
                                        "/%01CompObj", 114,
                                        "/%01Ole", 20);
                                    checkCompoundStorage(cc, attach);
                                } else if ("test_excel.xls".equals(name)) {
                                    checkCompoundEntries(cc,
                                        "/%02OlePres000", 1326,
                                        "/%03AccessObjSiteData", 56,
                                        "/%05SummaryInformation", 200,
                                        "/%05DocumentSummaryInformation", 264,
                                        "/%02OlePres001", 4208,
                                        "/%01CompObj", 107,
                                        "/Workbook", 13040,
                                        "/%01Ole", 20);
                                    // the excel data seems to be modified when embedded as ole,
                                    // so we can't reallly test it against the attachment data
                                } else {
                                    throw new JackcessRuntimeException("Unexpected compound entry " + name);
                                }
                            }
                            break;

                        case OTHER:
                            OleBlob.OtherContent oc = (OleBlob.OtherContent) content;
                            byte[] otherBytes = readToByteArray(oc.getStream(), oc.length());
                            assertArrayEquals(attach.getFileData(), otherBytes);
                            break;

                        default:
                            throw new JackcessRuntimeException("Unexpected type " + content.getType());
                    }

                }
            }
        }
    }

    private static byte[] readToByteArray(InputStream in, long length) throws IOException {
        try (in) {
            DataInputStream din = new DataInputStream(in);
            byte[] bytes = new byte[(int) length];
            din.readFully(bytes);
            return bytes;
        }
    }

    private static void checkCompoundEntries(OleBlob.CompoundContent cc, Object... entryInfo) {
        int idx = 0;
        for (OleBlob.CompoundContent.Entry e : cc) {
            String entryName = (String) entryInfo[idx];
            int entryLen = (Integer) entryInfo[idx + 1];

            assertEquals(entryName, e.getName());
            assertEquals(entryLen, e.length());

            idx += 2;
        }
    }

    private static void checkCompoundStorage(OleBlob.CompoundContent cc, Attachment attach) throws IOException {
        File tempFile = TestUtil.createTempFile("attach", ".dat", false);

        try (FileOutputStream fout = new FileOutputStream(tempFile)) {
            fout.write(attach.getFileData());

            POIFSFileSystem attachFs = new POIFSFileSystem(tempFile, true);

            for (OleBlob.CompoundContent.Entry e : cc) {
                DocumentEntry attachE = null;
                try {
                    attachE = CompoundOleUtil.getDocumentEntry(e.getName(), attachFs.getRoot());
                } catch (FileNotFoundException _ex) {
                    // ignored, the ole data has extra entries
                    continue;
                }

                byte[] attachEBytes = readToByteArray(new DocumentInputStream(attachE), attachE.getSize());
                byte[] entryBytes = readToByteArray(e.getStream(), e.length());

                assertArrayEquals(attachEBytes, entryBytes);
            }

            ByteUtil.closeQuietly(attachFs);
        }
    }
}
