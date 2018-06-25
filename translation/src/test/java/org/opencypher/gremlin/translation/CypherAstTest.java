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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.v9_0.util.symbols.AnyType;
import org.opencypher.v9_0.util.symbols.CypherType;
import org.opencypher.v9_0.util.symbols.IntegerType;
import org.opencypher.v9_0.util.symbols.NodeType;

public class CypherAstTest {

    @Test
    public void duplicateNames() {
        String cypher = "MATCH (n:person)-[r:knows]->(friend:person)\n" +
            "WHERE n.name = 'marko'\n" +
            "RETURN n, friend.name AS friend";
        CypherAst ast = CypherAst.parse(cypher, new HashMap<>());
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("n")).isInstanceOf(NodeType.class);
        assertThat(variableTypes.get("friend")).isInstanceOf(AnyType.class);
    }

    @Test
    public void duplicateNameInAggregation() {
        String cypher = "MATCH (n) RETURN n.prop AS n, count(n) AS count";
        CypherAst ast = CypherAst.parse(cypher, new HashMap<>());
        Map<String, CypherType> extractedParameters = ast.getReturnTypes();

        assertThat(extractedParameters.get("n")).isInstanceOf(AnyType.class);
    }

    @Test
    public void returnTypesInUnion() {
        String cypher = "MATCH (a:A)\n" +
            "RETURN a AS a\n" +
            "UNION\n" +
            "MATCH (b:B)\n" +
            "RETURN b AS a";
        CypherAst ast = CypherAst.parse(cypher, new HashMap<>());
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("a")).isInstanceOf(NodeType.class);
    }

    @Test
    public void variableInTypeTable() {
        String cypher = "MATCH (a)\n" +
            "RETURN a, count(a) + 3";
        CypherAst ast = CypherAst.parse(cypher, new HashMap<>());
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("a")).isInstanceOf(NodeType.class);
        assertThat(variableTypes.get("count(a) + 3")).isInstanceOf(IntegerType.class);
    }

    @Test
    public void noCypherExtensions() {
        CypherAst ast = CypherAst.parse(
            "MATCH (n:N) " +
                "WITH n.p AS s " +
                "WHERE s STARTS WITH 'x' AND s ENDS WITH 'x' AND s CONTAINS 'x' " +
                "RETURN length(s), toString(s)"
        );
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();

        assertThatThrownBy(() -> ast.buildTranslation(translator))
            .hasMessageContaining("contains, convertToString, endsWith, length, starsWith");
    }
}
