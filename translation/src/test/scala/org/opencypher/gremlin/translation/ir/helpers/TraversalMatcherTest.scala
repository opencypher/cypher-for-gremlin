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
package org.opencypher.gremlin.translation.ir.helpers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.opencypher.gremlin.translation.ir.helpers.TraversalMatcher.containsSteps
import org.opencypher.gremlin.translation.ir.model.{As, InV, OutE, Vertex}

class TraversalMatcherTest {

  @Test
  def contains(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil

    assertThat(containsSteps(seq, OutE("rel") :: As("r") :: InV :: Nil)).isTrue
    assertThat(containsSteps(seq, As("not exists") :: Nil)).isFalse
  }
}
