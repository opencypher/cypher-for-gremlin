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
package org.opencypher.gremlin.snippets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;
import org.opencypher.gremlin.rules.GremlinServerExternalResource;
import org.opencypher.gremlin.translation.CypherAstWrapper;
import org.opencypher.gremlin.translation.TranslationFacade;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinParameters;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinPredicates;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;

public class Translation {

    @ClassRule
    public static final GremlinServerExternalResource gremlinServer = new GremlinServerExternalResource();

    @Test
    public void translate() throws Exception {
        // freshReadmeSnippet: translate
        String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
        TranslationFacade cfog = new TranslationFacade();
        String gremlin = cfog.toGremlinGroovy(cypher);
        // freshReadmeSnippet: translate

        assertThat(gremlin).isEqualTo("g.V()." +
            "as('p')." +
            "where(" +
            "__.and(" +
            "__.select('p')." +
            "hasLabel('Person'), __.select('p')." +
            "values('age')." +
            "is(gt(25))))." +
            "select('p')." +
            "map(" +
            "__.project('p.name')." +
            "by(__.choose(neq('  cypher.null'), __.coalesce(__.values('name'), __.constant('  cypher.null')), __.constant('  cypher.null'))))");
    }

    @Test
    public void translateVerbose() throws Exception {
        // freshReadmeSnippet: verbose
        String cypher = "MATCH (p:Person) WHERE p.age > 25 RETURN p.name";
        CypherAstWrapper ast = CypherAstWrapper.parse(cypher);
        Translator<String, GroovyPredicate> translator = Translator.builder().gremlinGroovy().build();
        String gremlin = ast.buildTranslation(translator);
        // freshReadmeSnippet: verbose

        assertThat(gremlin).isEqualTo("g.V()." +
            "as('p')." +
            "where(" +
            "__.and(" +
            "__.select('p')." +
            "hasLabel('Person'), __.select('p')." +
            "values('age')." +
            "is(gt(25))))." +
            "select('p')." +
            "map(" +
            "__.project('p.name')." +
            "by(__.choose(neq('  cypher.null'), __.coalesce(__.values('name'), __.constant('  cypher.null')), __.constant('  cypher.null'))))");
    }

    @Test
    public void custom() throws Exception {
        // freshReadmeSnippet: custom
        Translator.builder()
            .custom(
                new MyGremlinSteps(),
                new MyGremlinPredicates(),
                new MyGremlinParameters()
            )
            .build();
        // freshReadmeSnippet: custom
    }


    private class MyGremlinSteps extends GroovyGremlinSteps {
    }

    private class MyGremlinPredicates extends GroovyGremlinPredicates {
    }

    private class MyGremlinParameters extends GroovyGremlinParameters {
    }
}
