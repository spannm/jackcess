package io.github.spannm.jackcess.impl;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.JackcessException;
import io.github.spannm.jackcess.PropertyMap;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.Basename;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;

/**
 * @author Dan Rollo Date: Mar 5, 2010 Time: 12:44:21 PM
 */
@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
class JetFormatTest extends AbstractBaseTest {

    @Test
    void testGetFormatNull() {
        assertThrows(NullPointerException.class, () -> JetFormat.getFormat(null));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.TEST, readOnly = true)
    void testGetFormat(TestDb testDb) throws Exception {
        try (FileChannel channel = DatabaseImpl.openChannel(testDb.getFile().toPath(), false, false)) {

            JetFormat fmtActual = JetFormat.getFormat(channel);
            assertEquals(testDb.getExpectedJetFormat(), fmtActual, "Unexpected JetFormat for dbFile: " + testDb.getFile().getAbsolutePath());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.TEST, readOnly = true)
    void testReadOnlyFormat(TestDb testDb) {
        Exception failure = null;
        try (Database db = testDb.openCopy()) {
            if (testDb.getExpectedJetFormat().READ_ONLY) {
                PropertyMap props = db.getUserDefinedProperties();
                props.put("foo", "bar");
                props.save();
            }

        } catch (Exception e) {
            failure = e;
        }

        if (!testDb.getExpectedJetFormat().READ_ONLY) {
            assertNull(failure);
        } else {
            assertInstanceOf(NonWritableChannelException.class, failure);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(basename = Basename.TEST, readOnly = true)
    void testFileFormat1(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            assertEquals(testDb.getExpectedFileFormat(), db.getFileFormat());
        }
    }

    @Test
    void testFileFormat2() throws Exception {
        try (Database db = TestUtil.open(FileFormat.GENERIC_JET4, new File(DIR_TEST_DATA, "adox_jet4.mdb"))) {
            assertEquals(FileFormat.GENERIC_JET4, db.getFileFormat());
        }
    }

    @Test
    void testSqlTypes() throws Exception {

        JetFormat v2000 = JetFormat.VERSION_4;
        for (DataType dt : DataType.values()) {
            if (v2000.isSupportedDataType(dt)) {
                Integer sqlType = null;
                try {
                    sqlType = dt.getSQLType();
                } catch (JackcessException ignored) {}

                if (sqlType != null) {
                    assertEquals(dt, DataType.fromSQLType(sqlType));
                }
            }
        }

        assertEquals(DataType.LONG, DataType.fromSQLType(java.sql.Types.BIGINT));
        assertEquals(DataType.BIG_INT, DataType.fromSQLType(
            java.sql.Types.BIGINT, 0, FileFormat.V2016));
        assertEquals(java.sql.Types.BIGINT, DataType.BIG_INT.getSQLType());
        assertEquals(DataType.MEMO, DataType.fromSQLType(
            java.sql.Types.VARCHAR, 1000));
    }

}
