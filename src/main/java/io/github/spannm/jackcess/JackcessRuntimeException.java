/*
 * Copyright (C) 2024- Markus Spann
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

/**
 * Unspecific {@code Jackcess} run-time exception.
 */
public class JackcessRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JackcessRuntimeException(String _message) {
        this(_message, null);
    }

    public JackcessRuntimeException(Throwable _cause) {
        this(null, _cause);
    }

    public JackcessRuntimeException(String _message, Throwable _cause) {
        super(_message, _cause);
    }

}
