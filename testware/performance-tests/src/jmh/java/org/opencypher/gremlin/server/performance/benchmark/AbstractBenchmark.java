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
package org.opencypher.gremlin.server.performance.benchmark;

import org.opencypher.gremlin.server.performance.infra.CypherClient;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;

import static org.opencypher.gremlin.server.performance.IOUtils.readFile;

@State(Scope.Thread)
public abstract class AbstractBenchmark {

    private CypherClient client;

    protected abstract CypherClient configureClient(Blackhole blackhole);

    @Setup
    public void setup(Blackhole blackhole) {
        client = configureClient(blackhole);
        client.run("MATCH (n) DETACH DELETE n");
        client.run(readFile("movies.cyp"));
    }

    @TearDown
    public void tearDown() {
        client.close();
    }

    @Benchmark
    public void allNodes() {
        client.run(
            "MATCH (n) " +
                "RETURN n"
        );
    }

    @Benchmark
    public void allPaths() {
        client.run(
            "MATCH (n)-[r]->() " +
                "RETURN n, r"
        );
    }

    @Benchmark
    public void relationshipTypes() {
        client.run(
            "MATCH ()-[r]->() " +
                "RETURN DISTINCT type(r)"
        );
    }

    @Benchmark
    public void limit() {
        client.run(
            "MATCH (n) " +
                "RETURN n LIMIT 10"
        );
    }

    @Benchmark
    public void orderBy() {
        client.run(
            "MATCH (n:Person) " +
                "RETURN n.name " +
                "ORDER BY n.born DESC LIMIT 10"
        );
    }


    @Benchmark
    public void byLabel() {
        client.run(
            "MATCH (m:Movie) " +
                "RETURN m"
        );
    }

    @Benchmark
    public void byProperty() {
        client.run(
            "MATCH (tom {name: \"Tom Hanks\"}) " +
                "RETURN tom"
        );
    }

    @Benchmark
    public void wherePropertyBetween() {
        client.run(
            "MATCH (nineties:Movie) " +
                "WHERE nineties.released > 1990 AND nineties.released < 2000 " +
                "RETURN nineties.title"
        );
    }

    @Benchmark
    public void byPath() {
        client.run(
            "MATCH (cloudAtlas {title: \"Cloud Atlas\"})<-[:DIRECTED]-(directors) " +
                "RETURN directors.name"
        );
    }

    @Benchmark
    public void byLongPath() {
        client.run(
            "MATCH (tom:Person {name:\"Tom Hanks\"})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors) " +
                "RETURN coActors.name"
        );
    }

    @Benchmark
    public void byUndirectedPath() {
        client.run(
            "MATCH (people:Person)-[relatedTo]-(:Movie {title: \"Cloud Atlas\"}) " +
                "RETURN people.name"
        );
    }

    @Benchmark
    public void byMultiplePaths() {
        client.run(
            "MATCH (tom:Person {name:\"Tom Hanks\"})-[:ACTED_IN]->(m)<-[:ACTED_IN]-(coActors), " +
                "(coActors)-[:ACTED_IN]->(m2)<-[:ACTED_IN]-(cruise:Person {name:\"Tom Cruise\"}) " +
                "RETURN DISTINCT coActors.name"
        );
    }

    @Benchmark
    public void byVariablePath() {
        client.run(
            "MATCH (bacon:Person {name:\"Kevin Bacon\"})-[*1..4]-(hollywood) " +
                "RETURN DISTINCT hollywood"
        );
    }

    @Benchmark
    public void countWithPivot() {
        client.run(
            "MATCH (m:Movie) " +
                "RETURN m.released AS Released, count(m) AS Count"
        );
    }

    @Benchmark
    public void groupByProperty() {
        client.run(
            "MATCH (m:Person) " +
                "RETURN m.born AS Year, collect(m) AS Born"
        );
    }
}
