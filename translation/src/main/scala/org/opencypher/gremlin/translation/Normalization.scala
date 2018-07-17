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
package org.opencypher.gremlin.translation

import org.opencypher.v9_0.frontend.phases.{BaseContext, Condition, StatementRewriter}
import org.opencypher.v9_0.rewriting.rewriters._
import org.opencypher.v9_0.util.{Rewriter, inSequence}

object Normalization extends StatementRewriter {
  override def instance(context: BaseContext): Rewriter = inSequence(
    flattenBooleanOperators,
    simplifyPredicates,
    collapseMultipleInPredicates,
    nameUpdatingClauses
  )

  override def description: String = "normalize the AST"

  override def postConditions: Set[Condition] = Set.empty
}
