package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.Basename.COMMON1;

import io.github.spannm.jackcess.DataType;
import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.JackcessException;
import io.github.spannm.jackcess.PropertyMap;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.NonWritableChannelException;

@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
class JetFormatTest extends AbstractBaseTest {

    @Test
    void testGetFormatNull() {
        assertThrows(NullPointerException.class, () -> JetFormat.getFormat(null));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testGetFormat(TestDb testDb) throws IOException {
        try (FileChannel channel = DatabaseImpl.openChannel(testDb.getFile().toPath(), false, false)) {

            JetFormat fmtActual = JetFormat.getFormat(channel);
            assertEquals(testDb.getExpectedJetFormat(), fmtActual, "Unexpected JetFormat for dbFile: " + testDb.getFile().getAbsolutePath());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testReadOnlyFormat(TestDb testDb) {
        Exception failure = null;
        try (Database db = testDb.openCopy()) {
            if (testDb.getExpectedJetFormat().READ_ONLY) {
                PropertyMap props = db.getUserDefinedProperties();
                props.put("foo", "bar");
                props.save();
            }

        } catch (Exception _ex) {
            failure = _ex;
        }

        if (!testDb.getExpectedJetFormat().READ_ONLY) {
            assertNull(failure);
        } else {
            assertInstanceOf(NonWritableChannelException.class, failure);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testFileFormat1(TestDb testDb) throws IOException {
        try (Database db = testDb.open()) {
            assertEquals(testDb.getExpectedFileFormat(), db.getFileFormat());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource(include = "GENERIC_JET4")
    void testFileFormat2(FileFormat ff) throws IOException {
        try (Database db = TestUtil.openDb(ff, new File(DIR_TEST_DATA, "adox_jet4.mdb"))) {
            assertEquals(ff, db.getFileFormat());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(DataType.class)
    void testJet4SqlTypes(DataType dt) throws IOException {
        if (JetFormat.VERSION_4.isSupportedDataType(dt)) {
            Integer sqlType = null;
            try {
                sqlType = dt.getSQLType();
            } catch (JackcessException ignored) {}

            if (sqlType != null) {
                assertEquals(dt, DataType.fromSQLType(sqlType));
            }
        }
    }

    @Test
    void testSqlTypes() throws IOException {
        assertEquals(DataType.LONG, DataType.fromSQLType(java.sql.Types.BIGINT));
        assertEquals(DataType.BIG_INT, DataType.fromSQLType(java.sql.Types.BIGINT, 0, FileFormat.V2016));
        assertEquals(java.sql.Types.BIGINT, DataType.BIG_INT.getSQLType());
        assertEquals(DataType.MEMO, DataType.fromSQLType(java.sql.Types.VARCHAR, 1000));
    }

}
