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
package org.opencypher.gremlin.translation.ir.helpers

import org.assertj.core.api.AbstractAssert
import org.assertj.core.util.Preconditions
import org.opencypher.gremlin.translation.ir.builder.{IRGremlinPredicates, IRGremlinSteps}
import org.opencypher.gremlin.translation.ir.helpers.TraversalAssertions._
import org.opencypher.gremlin.translation.ir.model.{GremlinPredicate, GremlinStep}
import org.opencypher.gremlin.translation.ir.rewrite.{GremlinRewriter, IdentityRewriter}
import org.opencypher.gremlin.translation.translator.TranslatorFlavor
import org.opencypher.gremlin.translation.{CypherAst, GremlinSteps}

import scala.collection.Seq

object CypherAstAssert {
  def __ = new IRGremlinSteps
  val P = new IRGremlinPredicates
}

class CypherAstAssert(
    actual: CypherAst,
    val flavor: TranslatorFlavor = TranslatorFlavor.empty,
    val rewriter: GremlinRewriter = IdentityRewriter
) extends AbstractAssert[CypherAstAssert, CypherAst](actual, classOf[CypherAstAssert]) {

  def withFlavor(flavor: TranslatorFlavor): CypherAstAssert = {
    new CypherAstAssert(actual, flavor, rewriter)
  }

  def rewritingWith(rewriter: GremlinRewriter): CypherAstAssert = {
    new CypherAstAssert(actual, flavor, rewriter)
  }

  def normalizedTo(expected: String): CypherAstAssert = {
    val actualString = actual.toString
    val expectedString = CypherAst.parse(expected).toString
    if (actualString != expectedString) {
      failWithMessage("AST mismatch!\nExpected: <%s>\n  Actual: <%s>", expectedString, actualString)
    }
    this
  }

  final def adds(traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): CypherAstAssert = {
    traversalNotContains("Traversal before rewrite", actualTraversal, traversal.current)
    traversalContains("Traversal after rewrite", rewriteTraversal, traversal.current)
    this
  }

  final def removes(traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): CypherAstAssert = {
    traversalContains("Traversal before rewrite", actualTraversal, traversal.current)
    traversalNotContains("Traversal after rewrite", rewriteTraversal, traversal.current)
    this
  }

  final def keeps(traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): CypherAstAssert = {
    traversalContains("Traversal before rewrite", actualTraversal, traversal.current)
    traversalContains("Traversal after rewrite", rewriteTraversal, traversal.current)
    this
  }

  final def contains(traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): CypherAstAssert = {
    traversalContains("Actual traversal", actualTraversal, traversal.current)
    this
  }

  final def doesNotContain(traversal: GremlinSteps[Seq[GremlinStep], GremlinPredicate]): CypherAstAssert = {
    traversalNotContains("Actual traversal", actualTraversal, traversal.current)
    this
  }

  private def actualTraversal = {
    actual.translate(flavor)
  }

  private def rewriteTraversal = {
    Preconditions.checkNotNull(rewriter, "Rewriter not set! Use `CypherAstAssert.rewritingWith`")
    actual.translate(flavor.extend(Seq(rewriter), Seq()))
  }
}
