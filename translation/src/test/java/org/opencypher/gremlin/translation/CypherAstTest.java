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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.assertj.core.data.MapEntry;
import org.junit.Test;
import org.opencypher.gremlin.extension.CypherProcedureDefinition;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;
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

    private static void assertReturnTypes(CypherAst ast, List<MapEntry<String, Class<?>>> expected) {
        Map<String, CypherType> returnTypes = ast.getReturnTypes();
        List<String> columns = new ArrayList<>(returnTypes.keySet());
        List<String> expectedColumns = expected.stream()
            .map(MapEntry::getKey)
            .collect(toList());

        assertThat(columns).containsExactlyElementsOf(expectedColumns);

        for (MapEntry<String, Class<?>> entry : expected) {
            String column = entry.getKey();
            assertThat(returnTypes.get(column))
                .as("Type of %s", column)
                .isInstanceOf(entry.getValue());
        }
    }

    @Test
    public void duplicateNames() {
        String cypher = "MATCH (n:person)-[r:knows]->(friend:person)\n" +
            "WHERE n.name = 'marko'\n" +
            "RETURN n, friend.name AS friend";
        CypherAst ast = CypherAst.parse(cypher);

        assertReturnTypes(ast, asList(
            entry("n", NodeType.class),
            entry("friend", AnyType.class)
        ));
    }

    @Test
    public void duplicateNameInAggregation() {
        String cypher = "MATCH (n) RETURN n.prop AS n, count(n) AS count";
        CypherAst ast = CypherAst.parse(cypher);

        assertReturnTypes(ast, asList(
            entry("n", AnyType.class),
            entry("count", IntegerType.class)
        ));
    }

    @Test
    public void returnTypesInUnion() {
        String cypher = "MATCH (a:A)\n" +
            "RETURN a AS a\n" +
            "UNION\n" +
            "MATCH (b:B)\n" +
            "RETURN b AS a";
        CypherAst ast = CypherAst.parse(cypher);

        assertReturnTypes(ast, singletonList(
            entry("a", NodeType.class)
        ));
    }

    @Test
    public void variableInTypeTable() {
        String cypher = "MATCH (a)\n" +
            "RETURN a, count(a) + 3";
        CypherAst ast = CypherAst.parse(cypher);

        assertReturnTypes(ast, asList(
            entry("a", NodeType.class),
            entry("count(a) + 3", IntegerType.class)
        ));
    }

    @Test
    public void standaloneCallTypes() {
        CypherProcedureDefinition procedures = new CypherProcedureDefinition();
        procedures.define(
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
        );
        CypherAst ast = CypherAst.parse("CALL proc()", emptyMap(), procedures.getSignatures());

        assertReturnTypes(ast, asList(
            entry("any", AnyType.class),
            entry("boolean", BooleanType.class),
            entry("string", StringType.class),
            entry("number", NumberType.class),
            entry("float", FloatType.class),
            entry("integer", IntegerType.class),
            entry("map", MapType.class),
            entry("list", ListType.class),
            entry("node", NodeType.class),
            entry("relationship", RelationshipType.class)
        ));
    }

    @Test
    public void callYieldTypes() {
        CypherProcedureDefinition procedures = new CypherProcedureDefinition();
        procedures.define(
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
        );
        CypherAst ast = CypherAst.parse(
            "CALL proc() " +
                "YIELD b, c " +
                "RETURN b, c",
            emptyMap(),
            procedures.getSignatures());

        assertReturnTypes(ast, asList(
            entry("b", AnyType.class),
            entry("c", AnyType.class)
        ));
    }

    @Test
    public void noCypherExtensions() {
        CypherAst ast = CypherAst.parse(
            "MATCH (n:N) " +
                "WITH n.p AS s " +
                "RETURN toString(s)"
        );
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();

        assertThatThrownBy(() -> ast.buildTranslation(translator))
            .hasMessageContaining("cypherToString");
    }
}
