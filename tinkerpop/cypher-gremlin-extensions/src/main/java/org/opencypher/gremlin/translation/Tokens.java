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
package org.opencypher.gremlin.translation;

public final class Tokens {
    private Tokens() {
    }

    public static final String START = "  cypher.start";
    public static final String NULL = "  cypher.null";
    public static final String UNUSED = "  cypher.unused";
    public static final String NONEXISTENT = "  cypher.nonexistent";
    public static final String PATH_EDGE = "  cypher.path.edge.";
    public static final String PATH_START = "  cypher.path.start.";
    public static final String MATCH_START = "  cypher.match.start.";
    public static final String MATCH_END = "  cypher.match.end.";

    public static final String PROJECTION_RELATIONSHIP = "  cypher.relationship";
    public static final String PROJECTION_ELEMENT = "  cypher.element";
    public static final String PROJECTION_ID = "  cypher.id";
    public static final String PROJECTION_INV = "  cypher.inv";
    public static final String PROJECTION_OUTV = "  cypher.outv";

    public static final String GENERATED = "  GENERATED";
    public static final String UNNAMED = "  UNNAMED";
    public static final String FRESHID = "  FRESHID";

    public static final String DELETE = "  cypher.delete";
    public static final String DETACH_DELETE = "  cypher.delete.detach";
    public static final String DELETE_ONCE = "  cypher.delete.once";
}
