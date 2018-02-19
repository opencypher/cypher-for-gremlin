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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CustomPredicatesTest {
    @Test
    public void startsWith() throws Exception {
        assertThat(CustomPredicates.startsWith("a").test("abcd")).isTrue();
        assertThat(CustomPredicates.startsWith("x").test("abcd")).isFalse();
        assertThat(CustomPredicates.startsWith("x").test(null)).isFalse();
        assertThat(CustomPredicates.startsWith(null).test("abcd")).isFalse();
        assertThat(CustomPredicates.startsWith(null).test(null)).isFalse();
    }

    @Test
    public void endsWith() throws Exception {
        assertThat(CustomPredicates.endsWith("d").test("abcd")).isTrue();
        assertThat(CustomPredicates.endsWith("x").test("abcd")).isFalse();
        assertThat(CustomPredicates.endsWith("x").test(null)).isFalse();
        assertThat(CustomPredicates.endsWith(null).test("abcd")).isFalse();
        assertThat(CustomPredicates.endsWith(null).test(null)).isFalse();
    }

    @Test
    public void contains() throws Exception {
        assertThat(CustomPredicates.contains("bc").test("abcd")).isTrue();
        assertThat(CustomPredicates.contains("x").test("abcd")).isFalse();
        assertThat(CustomPredicates.contains("x").test(null)).isFalse();
        assertThat(CustomPredicates.contains(null).test("abcd")).isFalse();
        assertThat(CustomPredicates.contains(null).test(null)).isFalse();
    }

}
