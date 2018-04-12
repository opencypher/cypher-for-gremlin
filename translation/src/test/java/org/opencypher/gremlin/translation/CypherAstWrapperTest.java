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

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.AnyType;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.CypherType;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.IntegerType;
import org.neo4j.cypher.internal.frontend.v3_3.symbols.NodeType;

public class CypherAstWrapperTest {

    @Test
    public void noAutoParameters() {
        String cypher = "MATCH (p:person) WHERE 27 <= p.age < 32 RETURN p.name AS name";
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher);
        Map<String, Object> extractedParameters = ast.getExtractedParameters();

        assertThat(extractedParameters)
            .isEmpty();
    }

    @Test
    public void explicitParameters() {
        String cypher = "MATCH (p:person) WHERE $low <= p.age < 32 RETURN p.name AS name";
        Map<String, ?> parameters = singletonMap("low", 29L);
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, parameters);
        Map<String, Object> extractedParameters = ast.getExtractedParameters();

        assertThat(extractedParameters)
            .containsExactly(
                entry("low", 29L)
            );
    }

    @Test
    public void duplicateNames() {
        String cypher = "MATCH (n:person)-[r:knows]->(friend:person)\n" +
                        "WHERE n.name = 'marko'\n" +
                        "RETURN n, friend.name AS friend";
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, new HashMap<>());
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("n")).isInstanceOf(NodeType.class);
        assertThat(variableTypes.get("friend")).isInstanceOf(AnyType.class);
    }

    @Test
    public void duplicateNameInAggregation() {
        String cypher = "MATCH (n) RETURN n.prop AS n, count(n) AS count";
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, new HashMap<>());
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
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, new HashMap<>());
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("a")).isInstanceOf(NodeType.class);
    }

    @Test
    public void variableInTypeTable() {
        String cypher = "MATCH (a)\n" +
            "RETURN a, count(a) + 3";
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher, new HashMap<>());
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("a")).isInstanceOf(NodeType.class);
        assertThat(variableTypes.get("count(a) + 3")).isInstanceOf(IntegerType.class);
    }
}
