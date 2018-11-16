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

import com.google.common.base.Charsets;
import com.google.common.collect.Streams;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.assertj.core.groups.Tuple;
import org.opencypher.gremlin.client.CypherGremlinClient;

public class TestCommons {
    public static String DELETE_ALL = "MATCH (n) DETACH DELETE n;";

    public static class ModernGraph {
        private ModernGraph() {
        }

        public Map<String, Object> MARKO;
        public Map<String, Object> VADAS;
        public Map<String, Object> JOSH;
        public Map<String, Object> PETER;

        public Map<String, Object> LOP;
        public Map<String, Object> RIPPLE;

        public Map<String, Object> MARKO_KNOWS_VADAS;
        public Map<String, Object> MARKO_KNOWS_JOSH;
        public Map<String, Object> MARKO_CREATED_LOP;

        public Map<String, Object> JOSH_CREATED_RIPPLE;
        public Map<String, Object> JOSH_CREATED_LOP;
        public Map<String, Object> PETER_CREATED_LOP;
    }

    @SuppressWarnings("unchecked")
    public static ModernGraph modernGraph(CypherGremlinClient client) throws IOException {
        String createModern = Resources.toString(Resources.getResource("modern.cyp"), Charsets.UTF_8).trim();

        client.submit(DELETE_ALL).all();
        Map<String, Object> r = client.submit(createModern).all().get(0);

        ModernGraph g = new ModernGraph();

        g.MARKO = (Map) r.get("marko");
        g.VADAS = (Map) r.get("vadas");
        g.JOSH = (Map) r.get("josh");
        g.PETER = (Map) r.get("peter");
        g.LOP = (Map) r.get("lop");
        g.RIPPLE = (Map) r.get("ripple");
        g.MARKO_KNOWS_VADAS = (Map) r.get("marko_knows_vadas");
        g.MARKO_KNOWS_JOSH = (Map) r.get("marko_knows_josh");
        g.MARKO_CREATED_LOP = (Map) r.get("marko_created_lop");
        g.JOSH_CREATED_RIPPLE = (Map) r.get("josh_created_ripple");
        g.JOSH_CREATED_LOP = (Map) r.get("josh_created_lop");
        g.PETER_CREATED_LOP = (Map) r.get("peter_created_lop");

        return g;
    }

    public static void snGraph(CypherGremlinClient client) throws IOException {
        String createModern = Resources.toString(Resources.getResource("snMini.cyp"), Charsets.UTF_8).trim();
        client.submit(DELETE_ALL).all();
        client.submit(createModern).all();
    }

    public static Map<String, Object> parameterMap(Object... parameters) {
        HashMap<String, Object> result = new HashMap<>();
        for (int i = 0; i < parameters.length; i += 2) {
            result.put(String.valueOf(parameters[i]), parameters[i + 1]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> ignoreOrderInCollections() {
        return (t1, t2) -> {
            if (t1 instanceof Tuple && t2 instanceof Tuple) {
                return (int) Streams.zip(((Tuple) t1).toList().stream(), ((Tuple) t2).toList().stream(),
                    (o1, o2) ->
                        areEqualCollections(o1, o2) || Objects.equals(o1, o2))
                    .filter(isEqual -> !isEqual)
                    .count();
            } else {
                return (areEqualCollections(t1, t2) || Objects.equals(t1, t2)) ? 0 : 1;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static boolean areEqualCollections(Object o1, Object o2) {
        if (o1 != null && (o2 != null) &&
            (o1 instanceof Collection) && (o2 instanceof Collection)) {
            Collection list1 = Collection.class.cast(o1);
            Collection list2 = Collection.class.cast(o2);
            return list1.size() == list2.size() && list1.containsAll(list2);
        }
        return false;
    }
}
