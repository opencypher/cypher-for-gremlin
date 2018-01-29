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
package org.opencypher.gremlin.server.jsr223;

import org.apache.tinkerpop.gremlin.jsr223.Customizer;
import org.apache.tinkerpop.gremlin.jsr223.DefaultImportCustomizer;
import org.apache.tinkerpop.gremlin.jsr223.GremlinPlugin;
import org.apache.tinkerpop.gremlin.jsr223.ImportCustomizer;
import org.opencypher.gremlin.traversal.CustomFunctions;
import org.opencypher.gremlin.traversal.CustomPredicates;

import java.util.Optional;

public class CypherPlugin implements GremlinPlugin {

    private static final ImportCustomizer imports = DefaultImportCustomizer.build()
        .addMethodImports(CustomPredicates.class.getDeclaredMethods())
        .addMethodImports(CustomFunctions.class.getDeclaredMethods())
        .create();

    @Override
    public String getName() {
        return "cypher.extra";
    }

    public static GremlinPlugin instance() {
        return new CypherPlugin();
    }

    @Override
    public boolean requireRestart() {
        return true;
    }

    @Override
    public Optional<Customizer[]> getCustomizers(String scriptEngineName) {
        return Optional.of(new Customizer[]{imports});
    }
}
