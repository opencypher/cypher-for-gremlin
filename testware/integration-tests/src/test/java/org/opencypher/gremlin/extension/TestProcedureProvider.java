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
package org.opencypher.gremlin.extension;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.opencypher.gremlin.extension.CypherArgument.argument;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestProcedureProvider implements CypherProcedureProvider {
    @Override
    public void apply(CypherProcedureRegistrar registry) {
        registry.register(
            "test.getName",
            emptyList(),
            singletonList(argument("name", String.class)),
            arguments -> asList(
                singletonMap("name", "marko"),
                singletonMap("name", "vadas")
            )
        );

        registry.register(
            "test.inc",
            singletonList(argument("a", Long.class)),
            singletonList(argument("r", Long.class)),
            arguments -> {
                long a = (long) arguments.get("a");
                return singletonList(singletonMap("r", a + 1));
            }
        );

        registry.register(
            "test.incF",
            singletonList(argument("a", Double.class)),
            singletonList(argument("r", Double.class)),
            arguments -> {
                double a = (double) arguments.get("a");
                return singletonList(singletonMap("r", a + 1));
            }
        );

        registry.register(
            "test.multi",
            emptyList(),
            asList(argument("foo", String.class), argument("bar", String.class)),
            arguments -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("bar", "bar");
                row.put("foo", "foo");
                return singletonList(row);
            }
        );

        registry.register(
            "test.void",
            emptyList(),
            emptyList(),
            arguments -> emptyList()
        );
    }
}
