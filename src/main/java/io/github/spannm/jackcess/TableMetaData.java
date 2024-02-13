/*
Copyright (c) 2016 James Ahlborn

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

/**
 * Basic metadata about a single database Table. This is the top-level information stored in a (local) database which
 * can be retrieved without attempting to load the Table itself.
 *
 * @author James Ahlborn
 */
public interface TableMetaData {
    enum Type {
        LOCAL,
        LINKED,
        LINKED_ODBC;
    }

    /**
     * The type of table
     */
    Type getType();

    /**
     * The name of the table (as it is stored in the database)
     */
    String getName();

    /**
     * {@code true} if this is a linked table, {@code false} otherwise.
     */
    boolean isLinked();

    /**
     * {@code true} if this is a system table, {@code false} otherwise.
     */
    boolean isSystem();

    /**
     * The name of this linked table in the linked database if this is a linked table, {@code null} otherwise.
     */
    String getLinkedTableName();

    /**
     * The name of this the linked database if this is a linked table, {@code
     * null} otherwise.
     */
    String getLinkedDbName();

    /**
     * The connection of this the linked database if this is a linked ODBC table, {@code null} otherwise.
     */
    String getConnectionName();

    /**
     * Opens this table from the given Database instance.
     */
    Table open(Database db) throws IOException;

    /**
     * Gets the local table definition from the given Database instance if available. Only useful for linked ODBC
     * tables.
     */
    TableDefinition getTableDefinition(Database db) throws IOException;
}
