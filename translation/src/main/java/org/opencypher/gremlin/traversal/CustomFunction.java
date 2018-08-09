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

import java.util.Objects;
import java.util.function.Function;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;

public class CustomFunction {
    private final String name;
    private final Function<Traverser, Object> implementation;

    CustomFunction(String name, Function<Traverser, Object> implementation) {
        this.name = name;
        this.implementation = implementation;
    }

    public String getName() {
        return name;
    }

    public Function<Traverser, Object> getImplementation() {
        return implementation;
    }

    public static CustomFunction cypherToString() {
        return new CustomFunction(
            "cypherToString",
            CustomFunctions.cypherToString()
        );
    }

    public static CustomFunction cypherToBoolean() {
        return new CustomFunction(
            "cypherToBoolean",
            CustomFunctions.cypherToBoolean()
        );
    }

    public static CustomFunction cypherToInteger() {
        return new CustomFunction(
            "cypherToInteger",
            CustomFunctions.cypherToInteger()
        );
    }

    public static CustomFunction cypherToFloat() {
        return new CustomFunction(
            "cypherToFloat",
            CustomFunctions.cypherToFloat()
        );
    }

    public static CustomFunction cypherProperties() {
        return new CustomFunction(
            "cypherProperties",
            CustomFunctions.cypherProperties()
        );
    }

    public static CustomFunction cypherContainerIndex() {
        return new CustomFunction(
            "cypherContainerIndex",
            CustomFunctions.cypherContainerIndex()
        );
    }

    public static CustomFunction cypherListSlice() {
        return new CustomFunction(
            "cypherListSlice",
            CustomFunctions.cypherListSlice()
        );
    }

    public static CustomFunction cypherPathComprehension() {
        return new CustomFunction(
            "cypherPathComprehension",
            CustomFunctions.cypherPathComprehension()
        );
    }

    public static CustomFunction cypherPercentileCont() {
        return new CustomFunction(
            "cypherPercentileCont",
            CustomFunctions.cypherPercentileCont()
        );
    }

    public static CustomFunction cypherPercentileDisc() {
        return new CustomFunction(
            "cypherPercentileDisc",
            CustomFunctions.cypherPercentileDisc()
        );
    }

    public static CustomFunction cypherSize() {
        return new CustomFunction(
            "cypherSize",
            CustomFunctions.cypherSize()
        );
    }

    public static CustomFunction cypherPlus() {
        return new CustomFunction(
            "cypherPlus",
            CustomFunctions.cypherPlus()
        );
    }

    public static CustomFunction cypherException() {
        return new CustomFunction(
            "cypherException",
            CustomFunctions.cypherException()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomFunction)) return false;
        CustomFunction that = (CustomFunction) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
