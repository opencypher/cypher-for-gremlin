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
package org.opencypher.gremlin.translation.ir.model

sealed trait GremlinPredicate

case class Eq(value: Any) extends GremlinPredicate
case class Gt(value: Any) extends GremlinPredicate
case class Gte(value: Any) extends GremlinPredicate
case class Lt(value: Any) extends GremlinPredicate
case class Lte(value: Any) extends GremlinPredicate
case class Neq(value: Any) extends GremlinPredicate
case class Between(first: Any, second: Any) extends GremlinPredicate
case class Within(values: Any*) extends GremlinPredicate
case class Without(values: Any*) extends GremlinPredicate
case class StartsWith(value: Any) extends GremlinPredicate
case class EndsWith(value: Any) extends GremlinPredicate
case class Contains(value: Any) extends GremlinPredicate
case class IsNode() extends GremlinPredicate
case class IsString() extends GremlinPredicate
