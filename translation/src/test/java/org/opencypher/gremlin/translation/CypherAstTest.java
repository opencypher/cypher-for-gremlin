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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.opencypher.gremlin.extension.CypherBinding.binding;
import static org.opencypher.gremlin.extension.CypherBindingType.ANY;
import static org.opencypher.gremlin.extension.CypherBindingType.BOOLEAN;
import static org.opencypher.gremlin.extension.CypherBindingType.FLOAT;
import static org.opencypher.gremlin.extension.CypherBindingType.INTEGER;
import static org.opencypher.gremlin.extension.CypherBindingType.LIST;
import static org.opencypher.gremlin.extension.CypherBindingType.MAP;
import static org.opencypher.gremlin.extension.CypherBindingType.NODE;
import static org.opencypher.gremlin.extension.CypherBindingType.NUMBER;
import static org.opencypher.gremlin.extension.CypherBindingType.RELATIONSHIP;
import static org.opencypher.gremlin.extension.CypherBindingType.STRING;
import static org.opencypher.gremlin.extension.CypherProcedure.cypherProcedure;

import java.util.Map;
import org.junit.Test;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.traversal.ProcedureContext;
import org.opencypher.v9_0.util.symbols.AnyType;
import org.opencypher.v9_0.util.symbols.BooleanType;
import org.opencypher.v9_0.util.symbols.CypherType;
import org.opencypher.v9_0.util.symbols.FloatType;
import org.opencypher.v9_0.util.symbols.IntegerType;
import org.opencypher.v9_0.util.symbols.ListType;
import org.opencypher.v9_0.util.symbols.MapType;
import org.opencypher.v9_0.util.symbols.NodeType;
import org.opencypher.v9_0.util.symbols.NumberType;
import org.opencypher.v9_0.util.symbols.RelationshipType;
import org.opencypher.v9_0.util.symbols.StringType;

public class CypherAstTest {

    @Test
    public void duplicateNames() {
        String cypher = "MATCH (n:person)-[r:knows]->(friend:person)\n" +
            "WHERE n.name = 'marko'\n" +
            "RETURN n, friend.name AS friend";
        CypherAst ast = CypherAst.parse(cypher);
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("n")).isInstanceOf(NodeType.class);
        assertThat(variableTypes.get("friend")).isInstanceOf(AnyType.class);
    }

    @Test
    public void duplicateNameInAggregation() {
        String cypher = "MATCH (n) RETURN n.prop AS n, count(n) AS count";
        CypherAst ast = CypherAst.parse(cypher);
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
        CypherAst ast = CypherAst.parse(cypher);
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("a")).isInstanceOf(NodeType.class);
    }

    @Test
    public void variableInTypeTable() {
        String cypher = "MATCH (a)\n" +
            "RETURN a, count(a) + 3";
        CypherAst ast = CypherAst.parse(cypher);
        Map<String, CypherType> variableTypes = ast.getReturnTypes();

        assertThat(variableTypes.get("a")).isInstanceOf(NodeType.class);
        assertThat(variableTypes.get("count(a) + 3")).isInstanceOf(IntegerType.class);
    }

    @Test
    public void standaloneCallTypes() {
        ProcedureContext procedureContext = new ProcedureContext(singleton(
            cypherProcedure(
                "proc",
                emptyList(),
                asList(
                    binding("any", ANY),
                    binding("boolean", BOOLEAN),
                    binding("string", STRING),
                    binding("number", NUMBER),
                    binding("float", FLOAT),
                    binding("integer", INTEGER),
                    binding("map", MAP),
                    binding("list", LIST),
                    binding("node", NODE),
                    binding("relationship", RELATIONSHIP)
                ),
                arguments -> {
                    throw new UnsupportedOperationException();
                }
            )
        ));
        CypherAst ast = CypherAst.parse("CALL proc()", emptyMap(), procedureContext);
        Map<String, CypherType> returnTypes = ast.getReturnTypes();

        assertThat(returnTypes).hasSize(10);
        assertThat(returnTypes.get("any")).isInstanceOf(AnyType.class);
        assertThat(returnTypes.get("boolean")).isInstanceOf(BooleanType.class);
        assertThat(returnTypes.get("string")).isInstanceOf(StringType.class);
        assertThat(returnTypes.get("number")).isInstanceOf(NumberType.class);
        assertThat(returnTypes.get("float")).isInstanceOf(FloatType.class);
        assertThat(returnTypes.get("integer")).isInstanceOf(IntegerType.class);
        assertThat(returnTypes.get("map")).isInstanceOf(MapType.class);
        assertThat(returnTypes.get("list")).isInstanceOf(ListType.class);
        assertThat(returnTypes.get("node")).isInstanceOf(NodeType.class);
        assertThat(returnTypes.get("relationship")).isInstanceOf(RelationshipType.class);
    }

    @Test
    public void callYieldTypes() {
        ProcedureContext procedureContext = new ProcedureContext(singleton(
            cypherProcedure(
                "proc",
                emptyList(),
                asList(
                    binding("a", STRING),
                    binding("b", INTEGER),
                    binding("c", FLOAT)
                ),
                arguments -> {
                    throw new UnsupportedOperationException();
                }
            )
        ));
        CypherAst ast = CypherAst.parse(
            "CALL proc() " +
                "YIELD b, c " +
                "RETURN b, c",
            emptyMap(),
            procedureContext);
        Map<String, CypherType> returnTypes = ast.getReturnTypes();

        assertThat(returnTypes).hasSize(2);
        assertThat(returnTypes.get("b")).isInstanceOf(AnyType.class);
        assertThat(returnTypes.get("c")).isInstanceOf(AnyType.class);
    }

    @Test
    public void noCypherExtensions() {
        CypherAst ast = CypherAst.parse(
            "MATCH (n:N) " +
                "WITH n.p AS s " +
                "WHERE s STARTS WITH 'x' AND s ENDS WITH 'x' AND s CONTAINS 'x' " +
                "RETURN size(s), toString(s)"
        );
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();

        assertThatThrownBy(() -> ast.buildTranslation(translator))
            .hasMessageContaining("cypherContains, cypherEndsWith, cypherSize, cypherStarsWith, cypherToString");
    }
}
