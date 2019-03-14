/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.gremlin.tck.reports;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SystemOutReaderTest {
    @Test
    public void testTee() throws Exception {
        SystemOutReader output = new SystemOutReader();
        System.out.println("test1");
        System.out.println("test2");
        String out1 = output.clear();
        System.out.println("test3");
        String out2 = output.clear();
        output.close();
        System.out.println("test4");

        assertEquals(out1, "test1\ntest2\n");
        assertEquals(out2, "test3\n");
    }
}
