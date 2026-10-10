/*
 * Copyright (c) 2026 James Ahlborn
 * Copyright (c) 2026 Markus Spann
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
package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.test.Basename.COMMON1;

import io.github.spannm.jackcess.Database;
import io.github.spannm.jackcess.PropertyMap;
import io.github.spannm.jackcess.test.AbstractBaseTest;
import io.github.spannm.jackcess.test.TestDb;
import io.github.spannm.jackcess.test.source.TestDbReadOnlySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * Tests that a property map carries the whole flag byte, which Access uses as a bit field. See
 * {@link PropertyMapImpl#DDL_FLAG} and {@link PropertyMapImpl#SKIP_HANDLER_FLAG}.
 */
@SuppressWarnings({"checkstyle:MethodName", "PMD.LinguisticNaming"})
final class PropertyMapsTest extends AbstractBaseTest {

    /** the one property in the test databases which carries more than the ddl bit */
    private static final String FLAGGED_TABLE = "Table4";
    private static final String FLAGGED_PROP  = "ColIsGuid";
    private static final byte   FLAGGED_FLAGS = (byte) (PropertyMapImpl.DDL_FLAG | PropertyMapImpl.SKIP_HANDLER_FLAG);

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void read_flaggedProperty_keepsWholeFlagByte(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            PropertyMap.Property prop = db.getTable(FLAGGED_TABLE).getProperties().get(FLAGGED_PROP);

            assertThat(getFlags(prop)).isEqualTo(FLAGGED_FLAGS);
            // the ddl bit is set too, so the boolean view stays true
            assertThat(prop.isDdl()).isTrue();
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @TestDbReadOnlySource(COMMON1)
    void write_flaggedProperty_keepsWholeFlagByte(TestDb testDb) throws Exception {
        try (Database db = testDb.open()) {
            PropertyMaps maps = ((PropertyMapImpl) db.getTable(FLAGGED_TABLE).getProperties()).getOwner();

            PropertyMaps maps2 = ((DatabaseImpl) db).readProperties(maps.write(), maps.getObjectId(), null);

            assertThat(getFlags(maps2.getDefault().get(FLAGGED_PROP))).isEqualTo(FLAGGED_FLAGS);
        }
    }

    @Test
    void put_newProperty_getsDdlBitOnly() {
        PropertyMapImpl map = new PropertyMaps(10, null, null, null).getDefault();

        assertThat(getFlags(map.put("plain", "value"))).isZero();
        assertThat(getFlags(map.put("ddl", null, "value", true))).isEqualTo(PropertyMapImpl.DDL_FLAG);
    }

    /**
     * Returns the flag byte of the given property.
     *
     * @param prop property to inspect
     * @return the flag byte as stored in the file
     */
    private static byte getFlags(PropertyMap.Property prop) {
        return ((PropertyMapImpl.PropertyImpl) prop).getFlags();
    }
}
