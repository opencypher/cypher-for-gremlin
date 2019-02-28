/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.gremlin.translation.exception;

public enum CypherExceptions {
    DELETE_CONNECTED_NODE("Cannot delete node, because it still has relationships. To delete this node, you must first delete its relationships."),
    INVALID_RANGE("Invalid range argument");

    private String message;

    CypherExceptions(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static String messageByName(Object name) {
        return valueOf(String.valueOf(name)).getMessage();
    }
}
