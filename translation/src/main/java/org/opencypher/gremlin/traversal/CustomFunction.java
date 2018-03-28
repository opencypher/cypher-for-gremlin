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
package org.opencypher.gremlin.traversal;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalUtil;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.opencypher.gremlin.translation.ReturnProperties;
import org.opencypher.gremlin.translation.Tokens;
import org.opencypher.gremlin.translation.exception.TypeException;

@SuppressWarnings("unchecked")
public class CustomFunction implements Function<Traverser, Object> {
    private final String name;
    private final Object[] args;
    private final Function<Traverser, Object> implementation;

    private CustomFunction(String name, Function<Traverser, Object> reference, Object... args) {
        this.name = name;
        this.args = args;
        this.implementation = reference;
    }

    public String getName() {
        return name;
    }

    public Object[] getArgs() {
        return args;
    }

    @Override
    public Object apply(Traverser traverser) {
        return implementation.apply(traverser);
    }

    public static CustomFunction length() {
        return new CustomFunction(
            "length",
            traverser -> (((Path) traverser.get()).size() - 1) / 2
        );
    }

    public static CustomFunction convertToString() {
        return new CustomFunction(
            "convertToString",
            traverser -> Optional.ofNullable(traverser.get())
                .map(String::valueOf)
                .orElse(null)
        );
    }

    public static CustomFunction convertToBoolean() {
        return new CustomFunction(
            "convertToBoolean",
            traverser -> {
                Object arg = tokenToNull(traverser.get());
                boolean valid = arg == null ||
                    arg instanceof Boolean ||
                    arg instanceof String;
                if (!valid) {
                    String className = arg.getClass().getName();
                    throw new TypeException("Cannot convert " + className + " to boolean");
                }

                return Optional.ofNullable(arg)
                    .map(String::valueOf)
                    .map(v -> {
                        switch (v.toLowerCase()) {
                            case "true":
                                return true;
                            case "false":
                                return false;
                            default:
                                return Tokens.NULL;
                        }
                    })
                    .orElse(Tokens.NULL);
            });
    }

    public static CustomFunction convertToInteger() {
        return new CustomFunction(
            "convertToInteger",
            traverser -> {
                Object arg = tokenToNull(traverser.get());
                boolean valid = arg == null ||
                    arg instanceof Number ||
                    arg instanceof String;
                if (!valid) {
                    String className = arg.getClass().getName();
                    throw new TypeException("Cannot convert " + className + " to integer");
                }

                return nullToToken(
                    Optional.ofNullable(arg)
                        .map(String::valueOf)
                        .map(v -> {
                            try {
                                return Double.valueOf(v);
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        })
                        .map(Double::intValue)
                        .orElse(null));
            });
    }

    public static CustomFunction convertToFloat() {
        return new CustomFunction(
            "convertToFloat",
            traverser -> {
                Object arg = tokenToNull(traverser.get());
                boolean valid = arg == null ||
                    arg instanceof Number ||
                    arg instanceof String;
                if (!valid) {
                    String className = arg.getClass().getName();
                    throw new TypeException("Cannot convert " + className + " to float");
                }

                return nullToToken(
                    Optional.ofNullable(arg)
                        .map(String::valueOf)
                        .map(v -> {
                            try {
                                return Double.valueOf(v);
                            } catch (NumberFormatException e) {
                                return null;
                            }
                        })
                        .orElse(null));
            });
    }

    public static CustomFunction nodes() {
        return new CustomFunction(
            "nodes",
            traverser -> ((Path) traverser.get()).objects().stream()
                .filter(element -> element instanceof Vertex)
                .map(CustomFunction::finalizeElements)
                .collect(toList()));
    }

    public static CustomFunction relationships() {
        return new CustomFunction(
            "relationships",
                traverser -> ((Collection) ((Path) traverser.get()).objects()).stream()
                .flatMap(CustomFunction::flatten)
                .filter(element -> element instanceof Edge)
                .map(CustomFunction::finalizeElements)
                .collect(toList()));
    }

    public static CustomFunction listComprehension(final Object functionTraversal) {
        return new CustomFunction(
            "listComprehension",
            traverser -> {
                Object list = traverser.get();
                if (!(list instanceof Collection)) {
                    throw new IllegalArgumentException("Expected Iterable, got " + list.getClass());
                }

                if (!(functionTraversal instanceof GraphTraversal)) {
                    throw new IllegalArgumentException("Expected GraphTraversal, got " + list.getClass());
                }

                return ((Collection) list)
                    .stream()
                    .map(item -> {
                        GraphTraversal.Admin admin = GraphTraversal.class.cast(functionTraversal).asAdmin();
                        return TraversalUtil.apply(item, admin);
                    })
                    .collect(Collectors.toList());
            },
            functionTraversal);
    }

    public static CustomFunction pathComprehension() {
        return new CustomFunction(
            "pathComprehension",
            arg -> ((Collection) arg.get()).stream()
                .map(CustomFunction::pathToList)
                .map(path -> {
                    Optional<Edge> first = ((Collection) path)
                        .stream()
                        .filter(Edge.class::isInstance)
                        .map(Edge.class::cast)
                        .findFirst();

                    Edge edge = first.orElseThrow(() -> new RuntimeException("Invalid path, no edge found!"));

                    return Stream.of(
                        edge.outVertex(),
                        edge,
                        edge.inVertex())
                        .map(CustomFunction::finalizeElements)
                        .collect(toList());
                })
                .collect(Collectors.toList()));
    }

    public static CustomFunction containerIndex(Object index) {
        return new CustomFunction(
            "containerIndex",
            traverser -> {
                Object arg = traverser.get();
                if (arg instanceof Map) {
                    Map map = (Map) arg;
                    return map.get(index);
                }
                Collection coll = (Collection) arg;
                int idx = parseInt(String.valueOf(index));
                return coll.stream()
                    .skip(idx)
                    .findFirst()
                    .orElse(null);
            },
            index);
    }

    public static CustomFunction size() {
        return new CustomFunction(
            "size", traverser -> traverser.get() instanceof String ?
            (long) ((String) traverser.get()).length() :
            (long) ((Collection) traverser.get()).size());
    }

    private static Object flatten(Object element) {
        return element instanceof Collection ? ((Collection) element).stream() : Stream.of(element);
    }

    private static Object tokenToNull(Object maybeNull) {
        return Tokens.NULL.equals(maybeNull) ? null : maybeNull;
    }

    private static Object nullToToken(Object maybeNull) {
        return maybeNull == null ? Tokens.NULL : maybeNull;
    }

    public static Object pathToList(Object value) {
        return value instanceof Path ? new ArrayList<>(((Path) value).objects()) : value;
    }

    private static Object finalizeElements(Object o) {
            HashMap<Object, Object> result = new HashMap<>();

            if (Tokens.NULL.equals(o)) {
                return Tokens.NULL;
            }

            Element element = (Element) o;
            result.put(ReturnProperties.ID, element.id());
            result.put(ReturnProperties.LABEL, element.label());
            element.properties().forEachRemaining(e -> result.put(e.key(), e.value()));

            if (o instanceof Vertex) {
                result.put(ReturnProperties.TYPE, ReturnProperties.NODE_TYPE);
            } else {
                Edge edge = (Edge) o;

                result.put(ReturnProperties.TYPE, ReturnProperties.RELATIONSHIP_TYPE);
                result.put(ReturnProperties.INV, edge.inVertex().id());
                result.put(ReturnProperties.OUTV, edge.outVertex().id());
            }

            return result;
    }
}
