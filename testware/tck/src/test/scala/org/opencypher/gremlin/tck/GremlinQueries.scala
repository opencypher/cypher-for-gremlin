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
package org.opencypher.gremlin.tck

import org.opencypher.gremlin.translation.groovy.GroovyGremlinSteps

object GremlinQueries {
  val dropQuery: String = new GroovyGremlinSteps().V().drop().toString

  // TODO Remove after https://github.com/opencypher/openCypher/pull/295 is released
  private val nodePropsQuery =
    """MATCH (n)
      |UNWIND keys(n) AS key
      |WITH properties(n) AS properties, key, n
      |RETURN id(n) AS nodeId, key, properties[key] AS value""".stripMargin

  // TODO Remove after https://github.com/opencypher/openCypher/pull/295 is released
  private val relPropsQuery =
    """MATCH ()-[r]->()
      |UNWIND keys(r) AS key
      |WITH properties(r) AS properties, key, r
      |RETURN id(r) AS relId, key, properties[key] AS value""".stripMargin

  private val getNodeProperties = {
    val b = new GroovyGremlinSteps()
    b.V()
      .as("V")
      .properties()
      .project("nodeId", "key", "value")
      .by(
        b.start()
          .select("V")
          .id())
      .by(b.start().key())
      .by(b.start().value())
  }

  private val getRelProperties = {
    val b = new GroovyGremlinSteps()
    b.V()
      .outE()
      .as("E")
      .properties()
      .project("relId", "key", "value")
      .by(
        b.start()
          .select("E")
          .id())
      .by(b.start().key())
      .by(b.start().value())
  }

  val cypherToGremlinQueries = Map(
    nodePropsQuery -> getNodeProperties.toString,
    relPropsQuery -> getRelProperties.toString
  )

}
