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
package org.opencypher.gremlin.translation.ir.rewrite

import java.util

import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._

/**
  * Property setters where value is a constant can be simplified
  * to avoid empty traversal checks and set or unset the property directly.
  */
object SimplifyPropertySetters extends GremlinRewriter {

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {
    mapTraversals(replace({
      case PropertyT(key, Constant(value) :: Nil) :: rest =>
        PropertyV(key, value) :: rest
      case PropertyTC(cardinality, key, Constant(value) :: Nil) :: rest =>
        PropertyVC(cardinality, key, value) :: rest
      case ChooseT3(_, PropertyV(key, value) :: Nil, drop) :: rest =>
        val empty = value match {
          case NULL                     => true
          case coll: util.Collection[_] => coll.isEmpty
          case _                        => false
        }
        if (empty) {
          drop ++ rest
        } else {
          PropertyV(key, value) :: rest
        }
      case step @ ChooseT3(_, prop @ PropertyT(_, Project(_*) :: valueTail) :: Nil, _) :: rest
          if valueTail.init.forall(_.isInstanceOf[By]) =>
        valueTail.last match {
          case _: By | _: SelectC => prop ++ rest
          case _                  => step
        }
    }))(steps)
  }
}
