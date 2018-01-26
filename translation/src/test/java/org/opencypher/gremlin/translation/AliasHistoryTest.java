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
package org.opencypher.gremlin.translation;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AliasHistoryTest {

    private AliasHistory aliasHistory = new AliasHistory();

    @Test
    public void next() throws Exception {
        assertThat(aliasHistory.next("n")).isEqualTo("n");
        assertThat(aliasHistory.next("n")).isEqualTo("n_1");
        assertThat(aliasHistory.next("n")).isEqualTo("n_2");

        assertThat(aliasHistory.next("n_0")).isEqualTo("n_0");
        assertThat(aliasHistory.next("n_0")).isEqualTo("n_0_1");
        assertThat(aliasHistory.next("n_0")).isEqualTo("n_0_2");
    }

    @Test
    public void current() throws Exception {
        assertThat(aliasHistory.current("n")).isEqualTo("n");
        aliasHistory.next("n");
        assertThat(aliasHistory.current("n")).isEqualTo("n");
        aliasHistory.next("n");
        assertThat(aliasHistory.current("n")).isEqualTo("n_1");
        aliasHistory.next("n");
        assertThat(aliasHistory.current("n")).isEqualTo("n_2");

        assertThat(aliasHistory.current("m_0")).isEqualTo("m_0");
        aliasHistory.next("m_0");
        assertThat(aliasHistory.current("m_0")).isEqualTo("m_0");
        aliasHistory.next("m_0");
        assertThat(aliasHistory.current("m_0")).isEqualTo("m_0_1");
        aliasHistory.next("m_0");
        assertThat(aliasHistory.current("m_0")).isEqualTo("m_0_2");
    }

}
