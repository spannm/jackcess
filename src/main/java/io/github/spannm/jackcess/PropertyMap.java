/*
Copyright (c) 2013 James Ahlborn

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

package io.github.spannm.jackcess;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Map of properties for a database object.
 *
 * @author James Ahlborn
 */
public interface PropertyMap extends Iterable<PropertyMap.Property> {
    String ACCESS_VERSION_PROP    = "AccessVersion";
    String TITLE_PROP             = "Title";
    String AUTHOR_PROP            = "Author";
    String COMPANY_PROP           = "Company";

    String DEFAULT_VALUE_PROP     = "DefaultValue";
    String REQUIRED_PROP          = "Required";
    String ALLOW_ZERO_LEN_PROP    = "AllowZeroLength";
    String DECIMAL_PLACES_PROP    = "DecimalPlaces";
    String FORMAT_PROP            = "Format";
    String INPUT_MASK_PROP        = "InputMask";
    String CAPTION_PROP           = "Caption";
    String VALIDATION_RULE_PROP   = "ValidationRule";
    String VALIDATION_TEXT_PROP   = "ValidationText";
    String GUID_PROP              = "GUID";
    String DESCRIPTION_PROP       = "Description";
    String RESULT_TYPE_PROP       = "ResultType";
    String EXPRESSION_PROP        = "Expression";
    String ALLOW_MULTI_VALUE_PROP = "AllowMultipleValues";
    String ROW_SOURCE_TYPE_PROP   = "RowSourceType";
    String ROW_SOURCE_PROP        = "RowSource";
    String DISPLAY_CONTROL_PROP   = "DisplayControl";
    String TEXT_FORMAT_PROP       = "TextFormat";
    String IME_MODE_PROP          = "IMEMode";
    String IME_SENTENCE_MODE_PROP = "IMESentenceMode";

    String getName();

    int getSize();

    boolean isEmpty();

    /**
     * @return the property with the given name, if any
     */
    Property get(String name);

    /**
     * @return the value of the property with the given name, if any
     */
    Object getValue(String name);

    /**
     * @return the value of the property with the given name, if any, otherwise the given defaultValue
     */
    Object getValue(String name, Object defaultValue);

    /**
     * Creates a new (or updates an existing) property in the map. Attempts to determine the type of the property based
     * on the name and value (the property names listed above have their types builtin, otherwise the type of the value
     * is used).
     * <p>
     * Note, this change will not be persisted until the {@link #save} method has been called.
     *
     * @return the newly created (or updated) property
     * @throws IllegalArgumentException if the type of the property could not be determined automatically
     */
    Property put(String name, Object value);

    /**
     * Creates a new (or updates an existing) property in the map.
     * <p>
     * Note, this change will not be persisted until the {@link #save} method has been called.
     *
     * @return the newly created (or updated) property
     */
    Property put(String name, DataType type, Object value);

    /**
     * Creates a new (or updates an existing) property in the map.
     * <p>
     * Note, this change will not be persisted until the {@link #save} method has been called.
     *
     * @return the newly created (or updated) property
     */
    Property put(String name, DataType type, Object value, boolean isDdl);

    /**
     * Puts all the given properties into this map.
     *
     * @param props the properties to put into this map ({@code null} is tolerated and ignored).
     */
    void putAll(Iterable<? extends Property> props);

    /**
     * Removes the property with the given name
     *
     * @return the removed property, or {@code null} if none found
     */
    Property remove(String name);

    /**
     * Saves the current state of this map.
     */
    void save() throws IOException;

    /**
     * @return a Stream using the default Iterator.
     */
    default Stream<PropertyMap.Property> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Info about a property defined in a PropertyMap.
     */
    interface Property {
        String getName();

        DataType getType();

        /**
         * Whether or not this property is a DDL object. If {@code true}, users can't change or delete the property in
         * access without the dbSecWriteDef permission. Additionally, certain properties must be flagged correctly or
         * the access engine may not recognize them correctly.
         */
        boolean isDdl();

        Object getValue();

        /**
         * Sets the new value for this property.
         * <p>
         * Note, this change will not be persisted until the {@link PropertyMap#save} method has been called.
         */
        void setValue(Object newValue);
    }

    /**
     * Interface for enums which can be used as property values.
     */
    interface EnumValue {
        /**
         * @return the property value which should be stored in the db
         */
        Object getValue();
    }

    /**
     * Enum value constants for the DisplayControl property
     */
    enum DisplayControl implements EnumValue {
        BOUND_OBJECT_FRAME(108),
        CHECK_BOX(106),
        COMBO_BOX(111),
        COMMAND_BUTTON(104),
        CUSTOM_CONTROL(119),
        EMPTY_CELL(127),
        IMAGE(103),
        LABEL(100),
        LINE(102),
        LIST_BOX(110),
        NAVIGATION_BUTTON(130),
        NAVIGATION_CONTROL(129),
        OBJECT_FRAME(114),
        OPTION_BUTTON(105),
        OPTION_GROUP(107),
        PAGE(124),
        PAGE_BREAK(118),
        RECTANGLE(101),
        SUB_FORM(112),
        TAB_CTL(123),
        TEXT_BOX(109),
        TOGGLE_BUTTON(122),
        WEB_BROWSER(128);

        private final Short _value;

        DisplayControl(int value) {
            _value = (short) value;
        }

        @Override
        public Short getValue() {
            return _value;
        }

        @Override
        public String toString() {
            return name() + "[" + _value + "]";
        }
    }

    /**
     * Enum value constants for the TextFormat property
     */
    enum TextFormat implements EnumValue {
        HTMLRICHTEXT(1),
        PLAIN(0);

        private final Byte _value;

        TextFormat(int value) {
            _value = (byte) value;
        }

        @Override
        public Byte getValue() {
            return _value;
        }

        @Override
        public String toString() {
            return name() + "[" + _value + "]";
        }
    }

    /**
     * Enum value constants for the IMEMode property
     */
    enum IMEMode implements EnumValue {
        NOCONTROL(0),
        ON(1),
        OFF(2),
        DISABLE(3),
        HIRAGANA(4),
        KATAKANA(5),
        KATAKANAHALF(6),
        ALPHAFULL(7),
        ALPHA(8),
        HANGULFULL(9),
        HANGUL(10);

        private final Byte _value;

        IMEMode(int value) {
            _value = (byte) value;
        }

        @Override
        public Byte getValue() {
            return _value;
        }

        @Override
        public String toString() {
            return name() + "[" + _value + "]";
        }
    }

    /**
     * Enum value constants for the IMESentenceMode property
     */
    enum IMESentenceMode implements EnumValue {
        NORMAL(0),
        PLURAL(1),
        SPEAKING(2),
        NONE(3);

        private final Byte _value;

        IMESentenceMode(int value) {
            _value = (byte) value;
        }

        @Override
        public Byte getValue() {
            return _value;
        }

        @Override
        public String toString() {
            return name() + "[" + _value + "]";
        }
    }

}
