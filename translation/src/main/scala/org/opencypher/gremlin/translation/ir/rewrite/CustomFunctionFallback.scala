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
package org.opencypher.gremlin.translation.ir.rewrite

import org.apache.tinkerpop.gremlin.structure.Column.values
import org.opencypher.gremlin.translation.Tokens.NULL
import org.opencypher.gremlin.translation.exception.CypherExceptions
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._
import org.opencypher.gremlin.traversal.CustomFunction.{cypherException, cypherPlus}

/**
  * Replaces Custom Functions with "The Best We Could Do" Gremlin native alternatives
  */
object CustomFunctionFallback extends GremlinRewriter {
  val asSeq: Seq[GremlinRewriter] = Seq(CustomFunctionFallback)

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {

    mapTraversals(replace({
      case Constant(typ) :: MapF(function) :: rest if function.getName == cypherException().getName =>
        val text = CypherExceptions.messageByName(typ)
        Path :: From(text) :: rest

      case SelectC(values) :: MapF(function) :: rest if function.getName == cypherPlus().getName =>
        SelectC(values) :: Local(Unfold :: ChooseP(Neq(NULL), Sum :: Nil, Constant(NULL) :: Nil) :: Nil) :: rest
    }))(steps)
  }
}
