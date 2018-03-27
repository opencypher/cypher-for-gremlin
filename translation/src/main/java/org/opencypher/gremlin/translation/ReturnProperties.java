/*
 * Copyright (c) 2018 "Neo4j, Inc." [https://neo4j.com]
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ReturnProperties {
    private ReturnProperties() {
    }

    public static final String NODE_TYPE = "node";
    public static final String RELATIONSHIP_TYPE = "relationship";

    public static final String ID = "_id";
    public static final String LABEL = "_label";
    public static final String TYPE = "_type";
    public static final String INV = "_inV";
    public static final String OUTV = "_outV";

    public static final List<String> ALL_PROPERTIES = Arrays.asList(ID, LABEL, TYPE, INV, OUTV);

    public static boolean isNode(Object value) {
        return ((value instanceof Map) && NODE_TYPE.equals(((Map) value).get(TYPE)));
    }

    public static boolean isRelationship(Object value) {
        return ((value instanceof Map) && RELATIONSHIP_TYPE.equals(((Map) value).get(TYPE)));
    }

    public static boolean isPath(Object value) {
        if (!(value instanceof List)) {
            return false;
        }

        List list = (List) value;

        if (list.isEmpty() || !isNode(list.get(0))) {
            return false;
        }

        for (Object e : list) {
            if (!isNode(e) && !isRelationship(e)) {
                return false;
            }
        }

        return true;
    }
}
