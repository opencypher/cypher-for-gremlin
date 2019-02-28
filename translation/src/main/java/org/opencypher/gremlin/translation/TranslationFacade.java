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

import static java.util.Collections.emptyMap;

import java.util.Map;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;

/**
 * Cypher to Gremlin translation facade.
 * <p>
 * Basic usage example:
 * <pre>{@code
 * String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
 * TranslationFacade cfog = new TranslationFacade();
 * String gremlin = cfog.toGremlinGroovy(cypher);
 * }</pre>
 */
public class TranslationFacade {

    /**
     * Translates a Cypher query to Gremlin Groovy.
     *
     * @param cypher Cypher query
     * @return Gremlin Groovy query
     */
    public String toGremlinGroovy(String cypher) {
        return toGremlinGroovy(cypher, emptyMap());
    }

    /**
     * Translates a Cypher query to Gremlin Groovy.
     *
     * @param cypher     Cypher query
     * @param parameters query parameters
     * @return Gremlin Groovy query
     */
    public String toGremlinGroovy(String cypher, Map<String, Object> parameters) {
        CypherAst ast = CypherAst.parse(cypher, parameters);
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();
        return ast.buildTranslation(translator);
    }
}
