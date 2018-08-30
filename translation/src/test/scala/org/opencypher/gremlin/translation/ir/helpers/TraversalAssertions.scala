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

import org.assertj.core.api.Fail.fail
import org.opencypher.gremlin.translation.ir.TranslationWriter
import org.opencypher.gremlin.translation.ir.helpers.TraversalMatcher.containsSteps
import org.opencypher.gremlin.translation.ir.model.GremlinStep
import org.opencypher.gremlin.translation.translator.Translator

object TraversalAssertions {

  def traversalContains(description: String, actual: Seq[GremlinStep], expected: Seq[GremlinStep]): Unit = {
    if (!containsSteps(actual, expected))
      fail(
        s"$description does not contain expected steps!\nSteps expected: <%s>\n  Actual: <%s>",
        print(expected),
        print(actual)
      )
  }

  def traversalNotContains(description: String, actual: Seq[GremlinStep], expected: Seq[GremlinStep]): Unit = {
    if (containsSteps(actual, expected))
      fail(
        s"$description expected not to contain steps!\nSteps not expected: <%s>\n  Actual: <%s>",
        print(expected),
        print(actual)
      )
  }

  private def print(traversal: Seq[GremlinStep]): String = {
    val translator = Translator
      .builder()
      .gremlinGroovy()
      .enableCypherExtensions()
      .enableMultipleLabels()
      .build()
    TranslationWriter
      .write(traversal, translator, Map.empty[String, Any])
      .replace("'", "\"")
  }
}
