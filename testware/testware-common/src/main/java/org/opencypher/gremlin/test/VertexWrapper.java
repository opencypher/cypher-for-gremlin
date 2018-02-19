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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.detached.DetachedVertex;

public final class VertexWrapper extends DetachedVertex {

    public VertexWrapper(Vertex vertex) {
        super(vertex, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VertexWrapper that = (VertexWrapper) o;
        return Objects.equals(this.toString(), that.toString());
    }

    @Override
    public String toString() {
        return toStringHelper("")
            .add("id", id)
            .add("label", label)
            .add("properties", properties)
            .toString();
    }
}
