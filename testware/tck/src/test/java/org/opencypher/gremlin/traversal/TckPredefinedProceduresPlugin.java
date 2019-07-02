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
package org.opencypher.gremlin.traversal;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_LONG_FOR_INTS;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.tinkerpop.gremlin.jsr223.AbstractGremlinPlugin;
import org.apache.tinkerpop.gremlin.jsr223.DefaultImportCustomizer;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.slf4j.Logger;

public class TckPredefinedProceduresPlugin extends AbstractGremlinPlugin {
    private static final Logger logger = getLogger(TckPredefinedProceduresPlugin.class);

    private static ObjectReader reader = new ObjectMapper().reader().with(USE_LONG_FOR_INTS).forType(Object.class);

    public TckPredefinedProceduresPlugin() {
        super("opencypher.gremlin.tck",
            DefaultImportCustomizer.build()
                .addMethodImports(TckPredefinedProceduresPlugin.class.getDeclaredMethods())
                .create());
    }

    public static TckPredefinedProceduresPlugin create() {
        return new TckPredefinedProceduresPlugin();
    }

    public static TckPredefinedProceduresPlugin build() {
        return new TckPredefinedProceduresPlugin();
    }

    public static Function<Traverser, Object> registerTckProcedure() {
        return traverser -> {
            Object o = traverser.get();
            try {
                List arguments = (List) o;
                String signature = String.valueOf(arguments.get(0));
                String rowsJson = String.valueOf(arguments.get(1));
                String headerJson = String.valueOf(arguments.get(2));

                List<String> header = reader.readValue(headerJson);
                List<Map<String, Object>> rows = reader.readValue(rowsJson);

                PredefinedProcedureRegistry.register(signature, header, rows);

                logger.info(format("Registred TCK procedure `%s` `%s` `%s`",
                    signature, header, rows));

                return true;
            } catch (Exception e) {
                throw new RuntimeException("Unable to register procedure: " + o, e);
            }
        };
    }

    public static Function<Traverser, Object> clearTckProcedures() {
        return traverser -> {
            PredefinedProcedureRegistry.clear();
            logger.info("Cleared TCK procedures");

            return true;
        };
    }
}
