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

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.junit.ClassRule;
import org.junit.Test;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.opencypher.Config;
import org.opencypher.GremlinDatabase;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.driver.v1.Values.parameters;

public class GremlinNeo4jDriverTest {
    @ClassRule
    public static final GremlinServerExternalResource server = new GremlinServerExternalResource();

    @Test
    public void testSimple() {
        Driver adam = GremlinDatabase.driver("//localhost:" + server.getPort());

        try (Session session = adam.session()) {
            StatementResult result = session.run("MATCH (n:person) RETURN count(n) as count");
            int count = result.single().get("count").asInt();
            assertThat(count).isEqualTo(4);
        }
    }

    @Test
    public void testMultipleRows() {
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
    public void testWithParameter() {
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
    public void testTranslating() {
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
}
