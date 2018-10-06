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
package org.opencypher.gremlin.translation.ir.builder

import org.opencypher.gremlin.translation.GremlinPredicates
import org.opencypher.gremlin.translation.ir.model._

class IRGremlinPredicates extends GremlinPredicates[GremlinPredicate] {
  override def isEq(value: scala.Any): GremlinPredicate = Eq(value)

  override def gt(value: scala.Any): GremlinPredicate = Gt(value)

  override def gte(value: scala.Any): GremlinPredicate = Gte(value)

  override def lt(value: scala.Any): GremlinPredicate = Lt(value)

  override def lte(value: scala.Any): GremlinPredicate = Lte(value)

  override def neq(value: scala.Any): GremlinPredicate = Neq(value)

  override def between(first: scala.Any, second: scala.Any): GremlinPredicate = Between(first, second)

  override def within(values: AnyRef*): GremlinPredicate = Within(values: _*)

  override def without(values: AnyRef*): GremlinPredicate = Without(values: _*)

  override def startsWith(value: scala.Any): GremlinPredicate = StartsWith(value)

  override def endsWith(value: scala.Any): GremlinPredicate = EndsWith(value)

  override def contains(value: scala.Any): GremlinPredicate = Contains(value)

  override def isNode: GremlinPredicate = IsNode()

  override def isString: GremlinPredicate = IsString()
}
