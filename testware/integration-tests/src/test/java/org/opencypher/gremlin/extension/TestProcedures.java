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
package org.opencypher.gremlin.extension;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.opencypher.gremlin.extension.CypherBinding.binding;
import static org.opencypher.gremlin.extension.CypherBindingType.FLOAT;
import static org.opencypher.gremlin.extension.CypherBindingType.INTEGER;
import static org.opencypher.gremlin.extension.CypherBindingType.STRING;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class TestProcedures implements Supplier<CypherProcedureDefinition> {

    private final CypherProcedureDefinition procedures = new CypherProcedureDefinition();

    public TestProcedures() {
        procedures.define(
            "test.getName",
            emptyList(),
            singletonList(binding("name", STRING)),
            arguments -> asList(
                singletonMap("name", "marko"),
                singletonMap("name", "vadas")
            )
        );

        procedures.define(
            "test.inc",
            singletonList(binding("a", INTEGER)),
            singletonList(binding("r", INTEGER)),
            arguments -> {
                long a = (long) arguments.get("a");
                return singletonList(singletonMap("r", a + 1));
            }
        );

        procedures.define(
            "test.incF",
            singletonList(binding("a", FLOAT)),
            singletonList(binding("r", FLOAT)),
            arguments -> {
                double a = (double) arguments.get("a");
                return singletonList(singletonMap("r", a + 1));
            }
        );

        procedures.define(
            "test.multi",
            emptyList(),
            asList(binding("foo", STRING), binding("bar", STRING)),
            arguments -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("bar", "bar");
                row.put("foo", "foo");
                return singletonList(row);
            }
        );

        procedures.define(
            "test.void",
            emptyList(),
            emptyList(),
            arguments -> emptyList()
        );
    }

    @Override
    public CypherProcedureDefinition get() {
        return procedures;
    }
}
