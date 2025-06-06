package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.COMMON1;

import io.github.spannm.jackcess.Database.FileFormat;
import io.github.spannm.jackcess.impl.DatabaseImpl;
import io.github.spannm.jackcess.impl.PropertyMapImpl;
import io.github.spannm.jackcess.impl.PropertyMaps;
import io.github.spannm.jackcess.impl.TableImpl;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.TestUtil;
import io.github.spannm.jackcess.test.source.FileFormatSource;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import io.github.spannm.jackcess.test.source.TestDbSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.IOException;
import java.util.*;

class PropertiesTest extends AbstractBaseTest {

    @Test
    void testPropertyMaps() {
        PropertyMaps maps = new PropertyMaps(10, null, null, null);
        assertTrue(maps.isEmpty());
        assertEquals(0, maps.getSize());
        assertFalse(maps.iterator().hasNext());
        assertEquals(10, maps.getObjectId());

        PropertyMapImpl defMap = maps.getDefault();
        assertTrue(defMap.isEmpty());
        assertEquals(0, defMap.getSize());
        assertFalse(defMap.iterator().hasNext());

        PropertyMapImpl colMap = maps.get("testcol");
        assertTrue(colMap.isEmpty());
        assertEquals(0, colMap.getSize());
        assertFalse(colMap.iterator().hasNext());

        assertFalse(maps.isEmpty());
        assertEquals(2, maps.getSize());

        assertSame(defMap, maps.get(PropertyMaps.DEFAULT_NAME));
        assertEquals(PropertyMaps.DEFAULT_NAME, defMap.getName());
        assertSame(colMap, maps.get("TESTCOL"));
        assertEquals("testcol", colMap.getName());

        defMap.put("foo", DataType.TEXT, "bar", false);
        defMap.put("baz", DataType.LONG, 13, true);

        assertFalse(defMap.isEmpty());
        assertEquals(2, defMap.getSize());
        assertFalse(defMap.get("foo").isDdl());
        assertTrue(defMap.get("baz").isDdl());

        colMap.put("buzz", DataType.BOOLEAN, Boolean.TRUE, true);

        assertFalse(colMap.isEmpty());
        assertEquals(1, colMap.getSize());

        assertEquals("bar", defMap.getValue("foo"));
        assertEquals("bar", defMap.getValue("FOO"));
        assertNull(colMap.getValue("foo"));
        assertEquals(13, defMap.get("baz").getValue());
        assertEquals(Boolean.TRUE, colMap.getValue("Buzz"));

        assertEquals("bar", defMap.getValue("foo", "blah"));
        assertEquals("blah", defMap.getValue("bogus", "blah"));

        List<PropertyMap.Property> props = new ArrayList<>();
        for (PropertyMap map : maps) {
            for (PropertyMap.Property prop : map) {
                props.add(prop);
            }
        }

        assertEquals(List.of(defMap.get("foo"), defMap.get("baz"),
            colMap.get("buzz")), props);
    }

    @Test
    void testInferTypes() {
        PropertyMaps maps = new PropertyMaps(10, null, null, null);
        PropertyMap defMap = maps.getDefault();

        assertEquals(DataType.TEXT,
            defMap.put(PropertyMap.FORMAT_PROP, null).getType());
        assertEquals(DataType.BOOLEAN,
            defMap.put(PropertyMap.REQUIRED_PROP, null).getType());

        assertEquals(DataType.TEXT,
            defMap.put("strprop", "this is a string").getType());
        assertEquals(DataType.BOOLEAN,
            defMap.put("boolprop", true).getType());
        assertEquals(DataType.LONG,
            defMap.put("intprop", 37).getType());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void testReadProperties(TestDb testDb) throws IOException {
        try (Database db = testDb.open()) {
            TableImpl t = (TableImpl) db.getTable("Table1");
            assertEquals(t.getTableDefPageNumber(), t.getPropertyMaps().getObjectId());
            PropertyMap tProps = t.getProperties();
            assertEquals(PropertyMaps.DEFAULT_NAME, tProps.getName());
            int expectedNumProps = 3;
            if (db.getFileFormat() != FileFormat.V1997) {
                assertEquals("{5A29A676-1145-4D1A-AE47-9F5415CDF2F1}", tProps.getValue(PropertyMap.GUID_PROP));
                expectedNumProps += 2;
            }
            assertEquals(expectedNumProps, tProps.getSize());
            assertEquals((byte) 0, tProps.getValue("Orientation"));
            assertEquals(Boolean.FALSE, tProps.getValue("OrderByOn"));
            assertEquals((byte) 2, tProps.getValue("DefaultView"));

            PropertyMap colProps = t.getColumn("A").getProperties();
            assertEquals("A", colProps.getName());
            expectedNumProps = 9;
            if (db.getFileFormat() != FileFormat.V1997) {
                assertEquals("{E9EDD90C-CE55-4151-ABE1-A1ACE1007515}", colProps.getValue(PropertyMap.GUID_PROP));
                expectedNumProps++;
            }
            assertEquals(expectedNumProps, colProps.getSize());
            assertEquals((short) -1, colProps.getValue("ColumnWidth"));
            assertEquals((short) 0, colProps.getValue("ColumnOrder"));
            assertEquals(Boolean.FALSE, colProps.getValue("ColumnHidden"));
            assertEquals(Boolean.FALSE, colProps.getValue(PropertyMap.REQUIRED_PROP));
            assertEquals(Boolean.FALSE, colProps.getValue(PropertyMap.ALLOW_ZERO_LEN_PROP));
            assertEquals((short) 109, colProps.getValue("DisplayControl"));
            assertEquals(Boolean.TRUE, colProps.getValue("UnicodeCompression"));
            assertEquals((byte) 0, colProps.getValue("IMEMode"));
            assertEquals((byte) 3, colProps.getValue("IMESentenceMode"));

            PropertyMap dbProps = db.getDatabaseProperties();
            assertTrue(((String) dbProps.getValue(PropertyMap.ACCESS_VERSION_PROP)).matches("[0-9]{2}[.][0-9]{2}"));

            PropertyMap sumProps = db.getSummaryProperties();
            assertEquals(3, sumProps.getSize());
            assertEquals("test", sumProps.getValue(PropertyMap.TITLE_PROP));
            assertEquals("tmccune", sumProps.getValue(PropertyMap.AUTHOR_PROP));
            assertEquals("Health Market Science", sumProps.getValue(PropertyMap.COMPANY_PROP));

            PropertyMap userProps = db.getUserDefinedProperties();
            assertEquals(1, userProps.getSize());
            assertEquals(Boolean.TRUE, userProps.getValue("ReplicateProject"));
        }
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class)
    void testParseProperties(FileFormat ff) throws IOException {
        File[] dbFiles = Optional.ofNullable(new File(DIR_TEST_DATA, ff.name()).listFiles()).orElse(new File[0]);
        for (File f : dbFiles) {

            if (!f.isFile()) {
                continue;
            }

            try (Database db = TestUtil.openDb(ff, f)) {
                PropertyMap dbProps = db.getDatabaseProperties();
                assertFalse(dbProps.isEmpty());
                assertTrue(((String) dbProps.getValue(PropertyMap.ACCESS_VERSION_PROP)).matches("[0-9]{2}[.][0-9]{2}"));

                for (Row row : ((DatabaseImpl) db).getSystemCatalog()) {
                    int id = row.getInt("Id");
                    byte[] propBytes = row.getBytes("LvProp");
                    PropertyMaps propMaps = ((DatabaseImpl) db).getPropertiesForObject(id, null);
                    int byteLen = propBytes != null ? propBytes.length : 0;
                    if (byteLen == 0) {
                        assertTrue(propMaps.isEmpty());
                    } else if (propMaps.isEmpty()) {
                        assertTrue(byteLen < 80);
                    } else {
                        assertTrue(byteLen > 0);
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMMON1)
    void testWriteProperties(TestDb testDb) throws IOException {
        try (Database db = testDb.open()) {
            TableImpl t = (TableImpl) db.getTable("Table1");

            PropertyMap tProps = t.getProperties();

            PropertyMaps maps = ((PropertyMapImpl) tProps).getOwner();

            byte[] mapsBytes = maps.write();

            PropertyMaps maps2 = ((DatabaseImpl) db).readProperties(
                mapsBytes, maps.getObjectId(), null);

            Iterator<PropertyMapImpl> iter = maps.iterator();
            Iterator<PropertyMapImpl> iter2 = maps2.iterator();

            while (iter.hasNext() && iter2.hasNext()) {
                PropertyMapImpl propMap = iter.next();
                PropertyMapImpl propMap2 = iter2.next();

                checkProperties(propMap, propMap2);
            }

            assertFalse(iter.hasNext());
            assertFalse(iter2.hasNext());
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMMON1)
    void testModifyProperties(TestDb testDb) throws IOException {
        File dbFile;
        PropertyMap origCProps;
        PropertyMap origFProps;
        PropertyMap origDProps;

        try (Database db = testDb.openCopy()) {
            dbFile = db.getFile();
            Table t = db.getTable("Table1");

            // grab originals
            origCProps = t.getColumn("C").getProperties();
            origFProps = t.getColumn("F").getProperties();
            origDProps = t.getColumn("D").getProperties();
        }

        // modify but do not save
        try (Database db = DatabaseBuilder.open(dbFile)) {
            Table t = db.getTable("Table1");

            PropertyMap cProps = t.getColumn("C").getProperties();
            PropertyMap fProps = t.getColumn("F").getProperties();
            PropertyMap dProps = t.getColumn("D").getProperties();

            assertFalse((Boolean) cProps.getValue(PropertyMap.REQUIRED_PROP));
            assertEquals("0", fProps.getValue(PropertyMap.DEFAULT_VALUE_PROP));
            assertEquals((short) 109, dProps.getValue("DisplayControl"));

            cProps.put(PropertyMap.REQUIRED_PROP, DataType.BOOLEAN, true);
            fProps.get(PropertyMap.DEFAULT_VALUE_PROP).setValue("42");
            dProps.remove("DisplayControl");
        }

        // modify and save
        try (Database db = DatabaseBuilder.open(dbFile)) {
            Table t = db.getTable("Table1");

            PropertyMap cProps = t.getColumn("C").getProperties();
            PropertyMap fProps = t.getColumn("F").getProperties();
            PropertyMap dProps = t.getColumn("D").getProperties();

            assertFalse((Boolean) cProps.getValue(PropertyMap.REQUIRED_PROP));
            assertEquals("0", fProps.getValue(PropertyMap.DEFAULT_VALUE_PROP));
            assertEquals((short) 109, dProps.getValue("DisplayControl"));

            checkProperties(origCProps, cProps);
            checkProperties(origFProps, fProps);
            checkProperties(origDProps, dProps);

            cProps.put(PropertyMap.REQUIRED_PROP, DataType.BOOLEAN, true);
            cProps.save();
            fProps.get(PropertyMap.DEFAULT_VALUE_PROP).setValue("42");
            fProps.save();
            dProps.remove("DisplayControl");
            dProps.save();
        }

        // reload saved props
        try (Database db = DatabaseBuilder.open(dbFile)) {
            Table t = db.getTable("Table1");

            PropertyMap cProps = t.getColumn("C").getProperties();
            PropertyMap fProps = t.getColumn("F").getProperties();
            PropertyMap dProps = t.getColumn("D").getProperties();

            assertTrue((Boolean) cProps.getValue(PropertyMap.REQUIRED_PROP));
            assertEquals("42", fProps.getValue(PropertyMap.DEFAULT_VALUE_PROP));
            assertNull(dProps.getValue("DisplayControl"));

            cProps.put(PropertyMap.REQUIRED_PROP, DataType.BOOLEAN, false);
            fProps.get(PropertyMap.DEFAULT_VALUE_PROP).setValue("0");
            dProps.put("DisplayControl", DataType.INT, (short) 109);

            checkProperties(origCProps, cProps);
            checkProperties(origFProps, fProps);
            checkProperties(origDProps, dProps);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource(exclude = "GENERIC_JET4")
    void testCreateDbProperties(FileFormat fileFormat) throws IOException {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        File file = TestUtil.createTempFile(getShortTestMethodName(), Database.FILE_EXT_MDB, false);

        try (Database db1 = DatabaseBuilder.newDatabase(file)
            .withFileFormat(fileFormat)
            .putUserDefinedProperty("testing", "123")
            .create()) {
            Table t1 = DatabaseBuilder.newTable("Test")
                .putProperty("awesome_table", true)
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG)
                    .withAutoNumber(true)
                    .withProperty(PropertyMap.REQUIRED_PROP, true)
                    .withProperty(PropertyMap.GUID_PROP, u1))
                .addColumn(DatabaseBuilder.newColumn("data", DataType.TEXT)
                    .withProperty(PropertyMap.ALLOW_ZERO_LEN_PROP, false)
                    .withProperty(PropertyMap.GUID_PROP, u2))
                .toTable(db1);

            t1.addRow(Column.AUTO_NUMBER, "value");
        }

        try (Database db = DatabaseBuilder.open(file)) {
            assertEquals("123", db.getUserDefinedProperties().getValue("testing"));

            Table t = db.getTable("Test");

            assertEquals(Boolean.TRUE, t.getProperties().getValue("awesome_table"));

            Column c = t.getColumn("id");
            assertEquals(Boolean.TRUE, c.getProperties().getValue(PropertyMap.REQUIRED_PROP));
            assertEquals("{" + u1.toString().toUpperCase() + "}", c.getProperties().getValue(PropertyMap.GUID_PROP));

            c = t.getColumn("data");
            assertEquals(Boolean.FALSE, c.getProperties().getValue(PropertyMap.ALLOW_ZERO_LEN_PROP));
            assertEquals("{" + u2.toString().toUpperCase() + "}", c.getProperties().getValue(PropertyMap.GUID_PROP));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void testEnforceProperties(FileFormat fileFormat) throws IOException {
        try (Database db = createDbMem(fileFormat)) {
            Table t = DatabaseBuilder.newTable("testReq")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG)
                    .withAutoNumber(true)
                    .withProperty(PropertyMap.REQUIRED_PROP, true))
                .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT)
                    .withProperty(PropertyMap.REQUIRED_PROP, true))
                .toTable(db);

            t.addRow(Column.AUTO_NUMBER, "v1");

            assertThrows(InvalidValueException.class, () -> t.addRow(Column.AUTO_NUMBER, null));

            t.addRow(Column.AUTO_NUMBER, "");

            List<? extends Map<String, Object>> expectedRows =
                    TestUtil.createExpectedTable(TestUtil.createExpectedRow("id", 1, "value", "v1"),
                            TestUtil.createExpectedRow("id", 2, "value", ""));
            TestUtil.assertTable(expectedRows, t);

            Table t2 = DatabaseBuilder.newTable("testNz")
                    .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG).withAutoNumber(true)
                            .withProperty(PropertyMap.REQUIRED_PROP, true))
                    .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT)
                            .withProperty(PropertyMap.ALLOW_ZERO_LEN_PROP, false))
                    .toTable(db);

            t2.addRow(Column.AUTO_NUMBER, "v1");

            assertThrows(InvalidValueException.class, () -> t2.addRow(Column.AUTO_NUMBER, ""));

            t2.addRow(Column.AUTO_NUMBER, null);

            expectedRows = TestUtil.createExpectedTable(TestUtil.createExpectedRow("id", 1, "value", "v1"),
                    TestUtil.createExpectedRow("id", 2, "value", null));
            TestUtil.assertTable(expectedRows, t2);

            Table t3 = DatabaseBuilder.newTable("testReqNz")
                .addColumn(DatabaseBuilder.newColumn("id", DataType.LONG)
                    .withAutoNumber(true)
                    .withProperty(PropertyMap.REQUIRED_PROP, true))
                .addColumn(DatabaseBuilder.newColumn("value", DataType.TEXT))
                .toTable(db);

            Column col = t3.getColumn("value");
            PropertyMap props = col.getProperties();
            props.put(PropertyMap.REQUIRED_PROP, true);
            props.put(PropertyMap.ALLOW_ZERO_LEN_PROP, false);
            props.save();

            t3.addRow(Column.AUTO_NUMBER, "v1");

            assertThrows(InvalidValueException.class, () -> t3.addRow(Column.AUTO_NUMBER, ""));

            assertThrows(InvalidValueException.class, () -> t3.addRow(Column.AUTO_NUMBER, null));

            t3.addRow(Column.AUTO_NUMBER, "v2");

            expectedRows = TestUtil.createExpectedTable(
                TestUtil.createExpectedRow("id", 1, "value", "v1"),
                TestUtil.createExpectedRow("id", 2, "value", "v2"));
            TestUtil.assertTable(expectedRows, t3);
        }
    }

    @Test
    void testEnumValues() {
        PropertyMaps maps = new PropertyMaps(10, null, null, null);

        PropertyMapImpl colMap = maps.get("testcol");

        colMap.put(PropertyMap.DISPLAY_CONTROL_PROP,
            PropertyMap.DisplayControl.TEXT_BOX);

        assertEquals(PropertyMap.DisplayControl.TEXT_BOX.getValue(),
            colMap.getValue(PropertyMap.DISPLAY_CONTROL_PROP));
    }

    private static void checkProperties(PropertyMap propMap1,
        PropertyMap propMap2) {
        assertEquals(propMap1.getSize(), propMap2.getSize());
        for (PropertyMap.Property prop : propMap1) {
            PropertyMap.Property prop2 = propMap2.get(prop.getName());

            assertEquals(prop.getName(), prop2.getName());
            assertEquals(prop.getType(), prop2.getType());

            Object v1 = prop.getValue();
            Object v2 = prop2.getValue();

            if (v1 instanceof byte[]) {
                assertArrayEquals((byte[]) v1, (byte[]) v2);
            } else {
                assertEquals(v1, v2);
            }
        }
    }

}
