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

import java.util.function.Function;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;

@SuppressWarnings("unchecked")
public class CustomFunction implements Function<Traverser, Object> {
    private final String name;
    private final Object[] args;
    private final Function<Traverser, Object> implementation;

    CustomFunction(String name, Object[] args, Function<Traverser, Object> implementation) {
        this.name = name;
        this.args = args;
        this.implementation = implementation;
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

    public static CustomFunction cypherToString() {
        return new CustomFunction(
            "cypherToString",
            new Object[]{},
            CustomFunctions.cypherToString()
        );
    }

    public static CustomFunction cypherToBoolean() {
        return new CustomFunction(
            "cypherToBoolean",
            new Object[]{},
            CustomFunctions.cypherToBoolean()
        );
    }

    public static CustomFunction cypherToInteger() {
        return new CustomFunction(
            "cypherToInteger",
            new Object[]{},
            CustomFunctions.cypherToInteger()
        );
    }

    public static CustomFunction cypherToFloat() {
        return new CustomFunction(
            "cypherToFloat",
            new Object[]{},
            CustomFunctions.cypherToFloat()
        );
    }

    public static CustomFunction cypherProperties() {
        return new CustomFunction(
            "cypherProperties",
            new Object[]{},
            CustomFunctions.cypherProperties()
        );
    }

    public static CustomFunction cypherContainerIndex() {
        return new CustomFunction(
            "cypherContainerIndex",
            new Object[]{},
            CustomFunctions.cypherContainerIndex()
        );
    }

    public static CustomFunction cypherListSlice() {
        return new CustomFunction(
            "cypherListSlice",
            new Object[]{},
            CustomFunctions.cypherListSlice()
        );
    }

    public static CustomFunction cypherPathComprehension() {
        return new CustomFunction(
            "cypherPathComprehension",
            new Object[]{},
            CustomFunctions.cypherPathComprehension()
        );
    }

    public static CustomFunction cypherPercentileCont(double percentile) {
        return new CustomFunction(
            "cypherPercentileCont",
            new Object[]{percentile},
            CustomFunctions.cypherPercentileCont(percentile)
        );
    }

    public static CustomFunction cypherPercentileDisc(double percentile) {
        return new CustomFunction(
            "cypherPercentileDisc",
            new Object[]{percentile},
            CustomFunctions.cypherPercentileDisc(percentile)
        );
    }

    public static CustomFunction cypherSize() {
        return new CustomFunction(
            "cypherSize",
            new Object[]{},
            CustomFunctions.cypherSize()
        );
    }

    public static CustomFunction cypherPlus() {
        return new CustomFunction(
            "cypherPlus",
            new Object[]{},
            CustomFunctions.cypherPlus()
        );
    }
}
