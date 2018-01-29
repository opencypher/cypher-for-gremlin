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

import org.opencypher.gremlin.translation.string.StringPredicate;

import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * Cypher to Gremlin translation facade.
 * <p>
 * Basic usage example:
 * <pre>{@code
 * String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
 * TranslationFacade cfog = new TranslationFacade();
 * String gremlin = cfog.toGremlin(cypher);
 * }</pre>
 */
public class TranslationFacade {

    /**
     * Translate a Cypher query to Gremlin.
     *
     * @param cypher Cypher query
     * @return Gremlin query
     */
    public String toGremlin(String cypher) {
        return toGremlin(cypher, emptyMap());
    }

    /**
     * Translate a Cypher query to Gremlin.
     *
     * @param cypher Cypher query
     * @param parameters query parameters
     * @return Gremlin query
     */
    public String toGremlin(String cypher, Map<String, Object> parameters) {
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, parameters);
        Translator<String, StringPredicate> translator = TranslatorFactory.string();
        TranslationPlan<String> translationPlan = ast.buildTranslation(translator);
        return translationPlan.getTranslation();
    }
}
