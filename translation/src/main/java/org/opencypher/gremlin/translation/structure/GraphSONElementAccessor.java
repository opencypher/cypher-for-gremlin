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
package org.opencypher.gremlin.translation.structure;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GraphSONElementAccessor implements ElementAccessor {

    private final Map<String, Object> element;

    public GraphSONElementAccessor(Map<String, Object> element) {
        this.element = element;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> keys() {
        return Optional.ofNullable(element.get("properties"))
            .map(properties -> ((Map<String, Object>) properties).keySet())
            .map(HashSet::new)
            .orElse(new HashSet<>());
    }

    @Override
    public String label() {
        return (String) element.get("label");
    }

    @Override
    public Object property(String name) {
        @SuppressWarnings("unchecked")
        Map<String, List<Map>> properties = (Map<String, List<Map>>) element.get("properties");

        // FIXME: Handle odd case of map literal accessed as GraphSON
        if (properties == null) {
            return element.get(name);
        }

        return Optional.ofNullable(properties.get(name))
            .map(multipleValuesProperty -> multipleValuesProperty.get(0))
            .filter(multipleValuesProperty -> !multipleValuesProperty.isEmpty())
            .map(property -> property.get("value"))
            .orElse(null);
    }

}
