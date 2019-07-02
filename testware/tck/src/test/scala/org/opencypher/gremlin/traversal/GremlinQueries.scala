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
package org.opencypher.gremlin.traversal

import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps
import org.opencypher.tools.tck.constants.TCKQueries

object GremlinQueries {
  private def __ = new GroovyGremlinSteps()

  val dropQuery: String = new GroovyGremlinSteps().V().drop().toString

  val getNodeProperties =
    __.V()
      .as("V")
      .properties()
      .project("nodeId", "key", "value")
      .by(
        __.start()
          .select("V")
          .id())
      .by(__.start().key())
      .by(__.start().value())
      .toString

  val getRelProperties =
    __.V()
      .outE()
      .as("E")
      .properties()
      .project("relId", "key", "value")
      .by(
        __.start()
          .select("E")
          .id())
      .by(__.start().key())
      .by(__.start().value())
      .toString

  val cypherToGremlinQueries: Map[String, String] = Map(
    TCKQueries.NODE_PROPS_QUERY -> getNodeProperties,
    TCKQueries.REL_PROPS_QUERY -> getRelProperties
  )

  def registerProcedure(signature: String, rowsJson: String, headerJson: String) =
    __.inject(rowsJson)
      .inject(headerJson)
      .inject(signature)
      .fold()
      .map(new CustomFunction("registerTckProcedure", null))
      .toString

  val clearProcedures =
    __.inject("start")
      .map(new CustomFunction("clearTckProcedures", null))
      .toString

}
