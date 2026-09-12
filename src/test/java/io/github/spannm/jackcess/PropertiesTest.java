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
package io.github.spannm.jackcess;

import static io.github.spannm.jackcess.test.Basename.COMMON1;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.*;

class PropertiesTest extends AbstractBaseTest {

    @Test
    void propertyMaps() {
        PropertyMaps maps = new PropertyMaps(10, null, null, null);
        assertThat(maps.isEmpty()).isTrue();
        assertThat(maps.getSize()).isEqualTo(0);
        assertThat(maps.iterator().hasNext()).isFalse();
        assertThat(maps.getObjectId()).isEqualTo(10);

        PropertyMapImpl defMap = maps.getDefault();
        assertThat(defMap.isEmpty()).isTrue();
        assertThat(defMap.getSize()).isEqualTo(0);
        assertThat(defMap.iterator().hasNext()).isFalse();

        PropertyMapImpl colMap = maps.get("testcol");
        assertThat(colMap.isEmpty()).isTrue();
        assertThat(colMap.getSize()).isEqualTo(0);
        assertThat(colMap.iterator().hasNext()).isFalse();

        assertThat(maps.isEmpty()).isFalse();
        assertThat(maps.getSize()).isEqualTo(2);

        assertThat(maps.get(PropertyMaps.DEFAULT_NAME)).isSameAs(defMap);
        assertThat(defMap.getName()).isEqualTo(PropertyMaps.DEFAULT_NAME);
        assertThat(maps.get("TESTCOL")).isSameAs(colMap);
        assertThat(colMap.getName()).isEqualTo("testcol");

        defMap.put("foo", DataType.TEXT, "bar", false);
        defMap.put("baz", DataType.LONG, 13, true);

        assertThat(defMap.isEmpty()).isFalse();
        assertThat(defMap.getSize()).isEqualTo(2);
        assertThat(defMap.get("foo").isDdl()).isFalse();
        assertThat(defMap.get("baz").isDdl()).isTrue();

        colMap.put("buzz", DataType.BOOLEAN, Boolean.TRUE, true);

        assertThat(colMap.isEmpty()).isFalse();
        assertThat(colMap.getSize()).isEqualTo(1);

        assertThat(defMap.getValue("foo")).isEqualTo("bar");
        assertThat(defMap.getValue("FOO")).isEqualTo("bar");
        assertThat(colMap.getValue("foo")).isNull();
        assertThat(defMap.get("baz").getValue()).isEqualTo(13);
        assertThat(colMap.getValue("Buzz")).isEqualTo(Boolean.TRUE);

        assertThat(defMap.getValue("foo", "blah")).isEqualTo("bar");
        assertThat(defMap.getValue("bogus", "blah")).isEqualTo("blah");

        List<PropertyMap.Property> props = new ArrayList<>();
        for (PropertyMap map : maps) {
            for (PropertyMap.Property prop : map) {
                props.add(prop);
            }
        }

        assertThat(props).isEqualTo(List.of(defMap.get("foo"), defMap.get("baz"),
                colMap.get("buzz")));
    }

    @Test
    void inferTypes() {
        PropertyMaps maps = new PropertyMaps(10, null, null, null);
        PropertyMap defMap = maps.getDefault();

        assertThat(defMap.put(PropertyMap.FORMAT_PROP, null).getType()).isEqualTo(DataType.TEXT);
        assertThat(defMap.put(PropertyMap.REQUIRED_PROP, null).getType()).isEqualTo(DataType.BOOLEAN);

        assertThat(defMap.put("strprop", "this is a string").getType()).isEqualTo(DataType.TEXT);
        assertThat(defMap.put("boolprop", true).getType()).isEqualTo(DataType.BOOLEAN);
        assertThat(defMap.put("intprop", 37).getType()).isEqualTo(DataType.LONG);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void readProperties(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            TableImpl t = (TableImpl) db.getTable("Table1");
            assertThat(t.getPropertyMaps().getObjectId()).isEqualTo(t.getTableDefPageNumber());
            PropertyMap tProps = t.getProperties();
            assertThat(tProps.getName()).isEqualTo(PropertyMaps.DEFAULT_NAME);
            int expectedNumProps = 3;
            if (db.getFileFormat() != FileFormat.V1997) {
                assertThat(tProps.getValue(PropertyMap.GUID_PROP)).isEqualTo("{5A29A676-1145-4D1A-AE47-9F5415CDF2F1}");
                expectedNumProps += 2;
            }
            assertThat(tProps.getSize()).isEqualTo(expectedNumProps);
            assertThat(tProps.getValue("Orientation")).isEqualTo((byte) 0);
            assertThat(tProps.getValue("OrderByOn")).isEqualTo(Boolean.FALSE);
            assertThat(tProps.getValue("DefaultView")).isEqualTo((byte) 2);

            PropertyMap colProps = t.getColumn("A").getProperties();
            assertThat(colProps.getName()).isEqualTo("A");
            expectedNumProps = 9;
            if (db.getFileFormat() != FileFormat.V1997) {
                assertThat(colProps.getValue(PropertyMap.GUID_PROP)).isEqualTo("{E9EDD90C-CE55-4151-ABE1-A1ACE1007515}");
                expectedNumProps++;
            }
            assertThat(colProps.getSize()).isEqualTo(expectedNumProps);
            assertThat(colProps.getValue("ColumnWidth")).isEqualTo((short) -1);
            assertThat(colProps.getValue("ColumnOrder")).isEqualTo((short) 0);
            assertThat(colProps.getValue("ColumnHidden")).isEqualTo(Boolean.FALSE);
            assertThat(colProps.getValue(PropertyMap.REQUIRED_PROP)).isEqualTo(Boolean.FALSE);
            assertThat(colProps.getValue(PropertyMap.ALLOW_ZERO_LEN_PROP)).isEqualTo(Boolean.FALSE);
            assertThat(colProps.getValue("DisplayControl")).isEqualTo((short) 109);
            assertThat(colProps.getValue("UnicodeCompression")).isEqualTo(Boolean.TRUE);
            assertThat(colProps.getValue("IMEMode")).isEqualTo((byte) 0);
            assertThat(colProps.getValue("IMESentenceMode")).isEqualTo((byte) 3);

            PropertyMap dbProps = db.getDatabaseProperties();
            assertThat(((String) dbProps.getValue(PropertyMap.ACCESS_VERSION_PROP)).matches("[0-9]{2}[.][0-9]{2}")).isTrue();

            PropertyMap sumProps = db.getSummaryProperties();
            assertThat(sumProps.getSize()).isEqualTo(3);
            assertThat(sumProps.getValue(PropertyMap.TITLE_PROP)).isEqualTo("test");
            assertThat(sumProps.getValue(PropertyMap.AUTHOR_PROP)).isEqualTo("tmccune");
            assertThat(sumProps.getValue(PropertyMap.COMPANY_PROP)).isEqualTo("Health Market Science");

            PropertyMap userProps = db.getUserDefinedProperties();
            assertThat(userProps.getSize()).isEqualTo(1);
            assertThat(userProps.getValue("ReplicateProject")).isEqualTo(Boolean.TRUE);
        }
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class)
    void parseProperties(FileFormat ff) throws Exception {
        File[] dbFiles = Optional.ofNullable(new File(DIR_TEST_DATA, ff.name()).listFiles()).orElse(new File[0]);
        for (File f : dbFiles) {

            if (!f.isFile()) {
                continue;
            }

            try (Database db = TestUtil.openDb(ff, f)) {
                PropertyMap dbProps = db.getDatabaseProperties();
                assertThat(dbProps.isEmpty()).isFalse();
                assertThat(((String) dbProps.getValue(PropertyMap.ACCESS_VERSION_PROP)).matches("[0-9]{2}[.][0-9]{2}")).isTrue();

                for (Row row : ((DatabaseImpl) db).getSystemCatalog()) {
                    int id = row.getInt("Id");
                    byte[] propBytes = row.getBytes("LvProp");
                    PropertyMaps propMaps = ((DatabaseImpl) db).getPropertiesForObject(id, null);
                    int byteLen = propBytes != null ? propBytes.length : 0;
                    if (byteLen == 0) {
                        assertThat(propMaps.isEmpty()).isTrue();
                    } else if (propMaps.isEmpty()) {
                        assertThat(byteLen < 80).isTrue();
                    } else {
                        assertThat(byteLen > 0).isTrue();
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMMON1)
    void writeProperties(TestDb testDb) throws Exception {
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

            assertThat(iter.hasNext()).isFalse();
            assertThat(iter2.hasNext()).isFalse();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbSource(COMMON1)
    void modifyProperties(TestDb testDb) throws Exception {
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

            assertThat((Boolean) cProps.getValue(PropertyMap.REQUIRED_PROP)).isFalse();
            assertThat(fProps.getValue(PropertyMap.DEFAULT_VALUE_PROP)).isEqualTo("0");
            assertThat(dProps.getValue("DisplayControl")).isEqualTo((short) 109);

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

            assertThat((Boolean) cProps.getValue(PropertyMap.REQUIRED_PROP)).isFalse();
            assertThat(fProps.getValue(PropertyMap.DEFAULT_VALUE_PROP)).isEqualTo("0");
            assertThat(dProps.getValue("DisplayControl")).isEqualTo((short) 109);

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

            assertThat((Boolean) cProps.getValue(PropertyMap.REQUIRED_PROP)).isTrue();
            assertThat(fProps.getValue(PropertyMap.DEFAULT_VALUE_PROP)).isEqualTo("42");
            assertThat(dProps.getValue("DisplayControl")).isNull();

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
    void createDbProperties(FileFormat fileFormat) throws Exception {
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
            assertThat(db.getUserDefinedProperties().getValue("testing")).isEqualTo("123");

            Table t = db.getTable("Test");

            assertThat(t.getProperties().getValue("awesome_table")).isEqualTo(Boolean.TRUE);

            Column c = t.getColumn("id");
            assertThat(c.getProperties().getValue(PropertyMap.REQUIRED_PROP)).isEqualTo(Boolean.TRUE);
            assertThat(c.getProperties().getValue(PropertyMap.GUID_PROP)).isEqualTo("{" + u1.toString().toUpperCase() + "}");

            c = t.getColumn("data");
            assertThat(c.getProperties().getValue(PropertyMap.ALLOW_ZERO_LEN_PROP)).isEqualTo(Boolean.FALSE);
            assertThat(c.getProperties().getValue(PropertyMap.GUID_PROP)).isEqualTo("{" + u2.toString().toUpperCase() + "}");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @FileFormatSource
    void enforceProperties(FileFormat fileFormat) throws Exception {
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
    void enumValues() {
        PropertyMaps maps = new PropertyMaps(10, null, null, null);

        PropertyMapImpl colMap = maps.get("testcol");

        colMap.put(PropertyMap.DISPLAY_CONTROL_PROP,
            PropertyMap.DisplayControl.TEXT_BOX);

        assertThat(colMap.getValue(PropertyMap.DISPLAY_CONTROL_PROP)).isEqualTo(PropertyMap.DisplayControl.TEXT_BOX.getValue());
    }

    private static void checkProperties(PropertyMap propMap1,
        PropertyMap propMap2) {
        assertThat(propMap2.getSize()).isEqualTo(propMap1.getSize());
        for (PropertyMap.Property prop : propMap1) {
            PropertyMap.Property prop2 = propMap2.get(prop.getName());

            assertThat(prop2.getName()).isEqualTo(prop.getName());
            assertThat(prop2.getType()).isEqualTo(prop.getType());

            Object v1 = prop.getValue();
            Object v2 = prop2.getValue();

            if (v1 instanceof byte[]) {
                assertThat((byte[]) v2).containsExactly((byte[]) v1);
            } else {
                assertThat(v2).isEqualTo(v1);
            }
        }
    }

}
