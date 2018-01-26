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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Translation plan is a composition of all the parts
 * necessary to run a translated Cypher query on a TinkerPop graph.
 *
 * @param <T> translation type
 * @see Translator
 */
public class TranslationPlan<T> {
    protected final T translation;
    private final Set<StatementOption> statementOptions;

    public TranslationPlan(T translation,
                           Set<StatementOption> statementOptions) {
        this.translation = translation;
        this.statementOptions = statementOptions;
    }

    /**
     * Checks if the query has a particular Cypher pre-parser option turned on.
     *
     * @param option supported option
     * @return true, if this option is included in the plan, false otherwise
     */
    public boolean hasOption(StatementOption option) {
        return statementOptions.contains(option);
    }

    /**
     * Returns full Gremlin translation
     *
     * @return translation in type {@code T}
     */
    public T getTranslation() {
        return translation;
    }

    /**
     * Returns a result row containing translation plan explanation for this query.
     *
     * @return explanation row
     */
    public Map<String, Object> explain() {
        Map<String, Object> explanation = new LinkedHashMap<>();
        explanation.put("translation", translation.toString());
        explanation.put("options", statementOptions.toString());

        return explanation;
    }
}
