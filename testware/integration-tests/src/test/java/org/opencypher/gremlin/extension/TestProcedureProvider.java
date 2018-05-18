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
    }
}
