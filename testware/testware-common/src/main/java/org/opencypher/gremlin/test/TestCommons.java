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
package org.opencypher.gremlin.test;

import static org.opencypher.gremlin.translation.ReturnProperties.ID;
import static org.opencypher.gremlin.translation.ReturnProperties.INV;
import static org.opencypher.gremlin.translation.ReturnProperties.LABEL;
import static org.opencypher.gremlin.translation.ReturnProperties.OUTV;
import static org.opencypher.gremlin.translation.ReturnProperties.TYPE;

import java.util.HashMap;
import java.util.Map;
import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps;

public class TestCommons {
    public static Map<String, Object> MARKO = parameterMap(ID, 1, LABEL, "person", TYPE, "node", "age", 29L, "name", "marko");
    public static Map<String, Object> VADAS = parameterMap(ID, 2, LABEL, "person", TYPE, "node", "age", 27L, "name", "vadas");
    public static Map<String, Object> JOSH = parameterMap(ID, 4, LABEL, "person", TYPE, "node", "age", 32L, "name", "josh");
    public static Map<String, Object> PETER = parameterMap(ID, 6, LABEL, "person", TYPE, "node", "age", 35L, "name", "peter");

    public static Map<String, Object> LOP = parameterMap(ID, 3, LABEL, "software", TYPE, "node", "lang", "java", "name", "lop");
    public static Map<String, Object> RIPPLE = parameterMap(ID, 5, LABEL, "software", TYPE, "node", "lang", "java", "name", "ripple");

    public static Map<String, Object> MARKO_KNOWS_VADAS = parameterMap(ID, 7, LABEL, "knows", TYPE, "relationship", "weight", 0.5, OUTV, MARKO.get(ID), INV, VADAS.get(ID));
    public static Map<String, Object> MARKO_KNOWS_JOSH = parameterMap(ID, 8, LABEL, "knows", TYPE, "relationship", "weight", 1.0, OUTV, MARKO.get(ID), INV, JOSH.get(ID));
    public static Map<String, Object> MARKO_CREATED_LOP = parameterMap(ID, 9, LABEL, "created", TYPE, "relationship", "weight", 0.4, OUTV, MARKO.get(ID), INV, LOP.get(ID));

    public static Map<String, Object> JOSH_CREATED_RIPPLE = parameterMap(ID, 10, LABEL, "created", TYPE, "relationship", "weight", 1.0, OUTV, JOSH.get(ID), INV, RIPPLE.get(ID));
    public static Map<String, Object> JOSH_CREATED_LOP = parameterMap(ID, 11, LABEL, "created", TYPE, "relationship", "weight", 0.4, OUTV, JOSH.get(ID), INV, LOP.get(ID));
    public static Map<String, Object> PETER_CREATED_LOP = parameterMap(ID, 12, LABEL, "created", TYPE, "relationship", "weight", 0.2, OUTV, PETER.get(ID), INV, LOP.get(ID));

    public static String DROP_ALL = new GroovyGremlinSteps().V().drop().current();

    public static String CREATE_MODERN = new GroovyGremlinSteps()
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

    public static Map<String, Object> parameterMap(Object... parameters) {
        HashMap<String, Object> result = new HashMap<>();
        for (int i = 0; i < parameters.length; i += 2) {
            result.put(String.valueOf(parameters[i]), parameters[i + 1]);
        }
        return result;
    }
}
