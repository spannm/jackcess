/*
Copyright (c) 2005 Health Market Science, Inc.

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

package io.github.spannm.jackcess.impl;

/**
 * Codes for page types
 *
 * @author Tim McCune
 */
public interface PageTypes {

    /** invalid page type */
    byte INVALID    = (byte) 0x00;
    /** Data page */
    byte DATA       = (byte) 0x01;
    /** Table definition page */
    byte TABLE_DEF  = (byte) 0x02;
    /** intermediate index page pointing to other index pages */
    byte INDEX_NODE = (byte) 0x03;
    /** leaf index page containing actual entries */
    byte INDEX_LEAF = (byte) 0x04;
    /** Table usage map page */
    byte USAGE_MAP  = (byte) 0x05;

}
