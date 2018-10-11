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

import org.apache.tinkerpop.gremlin.process.traversal.Scope.local
import org.opencypher.gremlin.translation.Tokens._
import org.opencypher.gremlin.translation.exception.CypherExceptions
import org.opencypher.gremlin.translation.ir.TraversalHelper._
import org.opencypher.gremlin.translation.ir.model._
import org.opencypher.gremlin.traversal.CustomFunction.{cypherException, cypherPlus, cypherProperties, cypherSize}

/**
  * Replaces Custom Functions with "The Best We Could Do" Gremlin native alternatives
  */
object CustomFunctionFallback extends GremlinRewriter {
  def prepend(rewriters: Seq[GremlinRewriter]): Seq[GremlinRewriter] = {
    CustomFunctionFallback +: rewriters
  }

  override def apply(steps: Seq[GremlinStep]): Seq[GremlinStep] = {

    mapTraversals(replace({
      case Constant(typ) :: MapF(function) :: rest if function.getName == cypherException().getName =>
        val text = CypherExceptions.messageByName(typ)
        Path :: From(text) :: rest

      case SelectC(values) :: MapF(function) :: rest if function.getName == cypherPlus().getName =>
        SelectC(values) :: Local(Unfold :: ChooseP(Neq(NULL), Sum :: Nil, None) :: Nil) :: rest

      case MapF(function) :: rest if function.getName == cypherSize().getName =>
        CountS(local) :: rest

      case MapF(function) :: rest if function.getName == cypherProperties().getName =>
        Local(Properties() :: Group :: By(Key :: Nil, None) :: By(MapT(Value :: Nil) :: Nil, None) :: Nil) :: rest

      case Unfold :: Is(IsNode()) :: As(hint) :: Fold :: rest if hint.startsWith(REWRITER_HINT) =>
        val pathName = hint.replaceFirst("^" + REWRITER_HINT, "")

        Path :: From(MATCH_START + pathName) :: To(MATCH_END + pathName) :: By(Identity :: Nil) :: By(
          Constant(UNUSED) :: Nil) :: Local(Unfold :: Is(Neq(UNUSED)) :: Fold :: Nil) :: rest

      case Unfold :: Is(IsRelationship()) :: As(hint) :: Fold :: rest if hint.startsWith(REWRITER_HINT) =>
        val pathName = hint.replaceFirst("^" + REWRITER_HINT, "")

        Path :: From(MATCH_START + pathName) :: To(MATCH_END + pathName) :: By(Constant(UNUSED) :: Nil) :: By(
          Identity :: Nil) :: Local(Unfold :: Is(Neq(UNUSED)) :: Fold :: Nil) :: rest
    }))(steps)
  }
}
