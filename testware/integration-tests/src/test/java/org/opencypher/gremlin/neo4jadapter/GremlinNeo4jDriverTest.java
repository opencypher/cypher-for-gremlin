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
package org.opencypher.gremlin.neo4jadapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.neo4j.driver.v1.Values.parameters;

import java.util.List;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;
import org.opencypher.Config;
import org.opencypher.GremlinDatabase;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

public class GremlinNeo4jDriverTest {
    @ClassRule
    public static final GremlinServerExternalResource server = new GremlinServerExternalResource();

    @Test
    public void simple() {
        Driver adam = GremlinDatabase.driver("//localhost:" + server.getPort());

        try (Session session = adam.session()) {
            StatementResult result = session.run("MATCH (n:person) RETURN count(n) as count");
            int count = result.single().get("count").asInt();
            assertThat(count).isEqualTo(4);
        }
    }

    @Test
    public void multipleRows() {
        Cluster cluster = Cluster.build()
            .addContactPoints("localhost")
            .port(server.getPort())
            .create();

        Driver driver = GremlinDatabase.driver(cluster);

        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (p:person) RETURN p.name, p.age");
            List<String> rows = result.list(r -> r.get("p.name").asString() + r.get("p.age").asInt());
            assertThat(rows).containsExactly("marko29", "vadas27", "josh32", "peter35");
        }
    }

    @Test
    public void withParameter() {
        Driver driver = GremlinDatabase.driver("//localhost:" + server.getPort());

        try (Session session = driver.session()) {
            StatementResult result = session.run("CREATE (a:Greeting) " +
                    "SET a.message = $message " +
                    "RETURN a.message",
                parameters("message", "Hello"));

            String message = result.single().get(0).asString();

            assertThat(message).isEqualTo("Hello");
        }
    }

    @Test
    public void returnNodeAndRelationship() {
        Driver driver = GremlinDatabase.driver("//localhost:" + server.getPort());

        try (Session session = driver.session()) {
            StatementResult result = session.run("CREATE (n1:Person {name: 'Marko'})-[r:knows {since:1999}]->(n2:Person)" +
                    "RETURN n1,r,n2",
                parameters("message", "Hello"));

            Record record = result.single();

            Node n1 = record.get("n1").asNode();
            Relationship r = record.get("r").asRelationship();
            Node n2 = record.get("n2").asNode();

            assertThat(n1.hasLabel("Person")).isTrue();
            assertThat(n1.get("name").asString()).isEqualTo("Marko");

            assertThat(r.hasType("knows")).isTrue();
            assertThat(r.startNodeId()).isEqualTo(n1.id());
            assertThat(r.endNodeId()).isEqualTo(n2.id());
            assertThat(r.get("since").asLong()).isEqualTo(1999L);

            assertThat(n2.hasLabel("Person")).isTrue();
        }
    }

    @Test
    public void returnPath() {
        Driver driver = GremlinDatabase.driver("//localhost:" + server.getPort());

        try (Session session = driver.session()) {
            StatementResult setup = session.run("CREATE (n1:Person {name: 'Anders'})-[r:knows]->(n2:Person)" +
                            "RETURN n1,r,n2");
            Record createdNodes = setup.single();
            Node n1 = createdNodes.get("n1").asNode();
            Node n2 = createdNodes.get("n2").asNode();
            Relationship r = createdNodes.get("r").asRelationship();

            StatementResult result = session.run("MATCH p =(b1 { name: 'Anders' })-->()" +
                    "RETURN p");
            Path path = result.single().get("p").asPath();

            assertThat(path.contains(n1)).isTrue();
            assertThat(path.contains(n2)).isTrue();
            assertThat(path.contains(r)).isTrue();
            assertThat(path.relationships()).hasSize(1);
            assertThat(path.nodes()).hasSize(2);
        }
    }


    @Test
    public void translating() {
        Config config = Config.build()
            .withTranslation(TranslatorFlavor.gremlinServer())
            .toConfig();

        Driver driver = GremlinDatabase.driver("//localhost:" + server.getPort(), config);

        try (Session session = driver.session()) {
            StatementResult result = session.run("MATCH (n:person) RETURN count(n) as count");
            int count = result.single().get("count").asInt();
            assertThat(count).isEqualTo(4);
        }
    }

    @Test
    public void invalidSyntax() {
        Driver driver = GremlinDatabase.driver("//localhost:" + server.getPort());

        try (Session session = driver.session()) {
            StatementResult result = session.run("INVALID");
            Throwable throwable = catchThrowable(result::list);

            assertThat(throwable)
                .hasMessageContaining("Invalid input");
        }
    }

    @Test
    public void invalidSyntaxInTranslation() {
        Config config = Config.build()
            .withTranslation(TranslatorFlavor.gremlinServer())
            .toConfig();

        Driver driver = GremlinDatabase.driver("//localhost:" + server.getPort(), config);

        try (Session session = driver.session()) {
            StatementResult result = session.run("INVALID");
            Throwable throwable = catchThrowable(result::list);

            assertThat(throwable)
                .hasMessageContaining("Invalid input");
        }
    }
}
