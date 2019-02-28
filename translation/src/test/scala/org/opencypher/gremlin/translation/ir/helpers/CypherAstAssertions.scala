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

import org.assertj.core.api.Assertions
import org.opencypher.gremlin.translation.CypherAst

object CypherAstAssertions {
  def assertThat(actual: CypherAst) = new CypherAstAssert(actual)

  def assertThat[T](actual: T): org.assertj.core.api.AbstractObjectAssert[_, T] = Assertions.assertThat(actual)
}
