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

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;

/**
 * Parsed Cypher AST wrapper that can transform it in a suitable format
 * for executing a Gremlin traversal.
 */
public class CypherAstWrapper {
    private final CypherAst ast;

    private CypherAstWrapper(CypherAst ast) {
        this.ast = ast;
    }

    /**
     * Constructs a new Cypher AST from the provided query.
     *
     * @param queryText Cypher query
     * @return Cypher AST wrapper
     */
    public static CypherAstWrapper parse(String queryText) {
        return parse(queryText, emptyMap());
    }

    /**
     * Constructs a new Cypher AST from the provided query.
     *
     * @param queryText    Cypher query
     * @param passedParams Cypher query parameters
     * @return Cypher AST wrapper
     */
    public static CypherAstWrapper parse(String queryText, Map<String, Object> passedParams) {
        CypherAst ast = CypherAst.parse(queryText, passedParams);
        return new CypherAstWrapper(ast);
    }

    /**
     * Create a translation to Gremlin.
     *
     * @param translator {@link Translator} implementation (e.g. Gremlin traversal or string)
     * @param <T>        translation target type
     * @param <P>        predicate target type
     * @return to-Gremlin translation
     */
    public <T, P> T buildTranslation(Translator<T, P> translator) {
        return ast.buildTranslation(translator);
    }

    /**
     * Get declared options for this query.
     *
     * @return set of statement options
     */
    public Set<StatementOption> getOptions() {
        return ast.getOptions();
    }

    /**
     * Pretty-prints the Cypher AST.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return ast.toString();
    }
}
