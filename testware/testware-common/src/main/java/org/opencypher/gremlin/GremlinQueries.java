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
package org.opencypher.gremlin;

import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps;

public interface GremlinQueries {
    String DROP_ALL = new GroovyGremlinSteps().V().drop().current();

    String CREATE_MODERN = new GroovyGremlinSteps()
        .addV("person").property("name", "marko").property("age", 29).as("marko")
        .addV("person").property("name", "vadas").property("age", 27).as("vadas")
        .addV("software").property("name", "lop").property("lang", "java").as("lop")
        .addV("person").property("name", "josh").property("age", 32).as("josh")
        .addV("software").property("name", "ripple").property("lang", "java").as("ripple")
        .addV("person").property("name", "peter").property("age", 35).as("peter")
        .addE("knows").from("marko").to("vadas").property("weight", 0.5d)
        .addE("knows").from("marko").to("josh").property("weight", 1.0d)
        .addE("created").from("marko").to("lop").property("weight", 0.4d)
        .addE("created").from("josh").to("ripple").property("weight", 1.0d)
        .addE("created").from("josh").to("lop").property("weight", 0.4d)
        .addE("created").from("peter").to("lop").property("weight", 0.2d)
        .current();
}
