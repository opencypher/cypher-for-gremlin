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

import org.junit.jupiter.api.{DynamicTest, TestFactory}
import org.opencypher.gremlin.rules.TinkerGraphServerEmbedded
import org.opencypher.gremlin.tck.GremlinCypherValueConverter.{toCypherValueRecords, toExecutionFailed, toGremlinParams}
import org.opencypher.gremlin.tck.GremlinQueries._
import org.opencypher.tools.tck.api._
import org.opencypher.tools.tck.values.CypherValue
import java.util

import scala.collection.JavaConverters._

object TinkerGraphServerEmbeddedGraph extends Graph {
  val TIME_OUT_SECONDS = 10

  val tinkerGraphServerEmbedded = new TinkerGraphServerEmbedded
  tinkerGraphServerEmbedded.before()
  tinkerGraphServerEmbedded.client().submitGremlin(dropQuery)

  override def cypher(query: String, params: Map[String, CypherValue], queryType: QueryType): Result = {
    queryType match {
      case SideEffectQuery if cypherToGremlinQueries.isDefinedAt(query) =>
        val results = tinkerGraphServerEmbedded.client.submitGremlin(cypherToGremlinQueries(query))
        toCypherValueRecords(query, results)

      case ExecQuery | InitQuery | SideEffectQuery =>
        val paramsJava: util.Map[String, Object] = toGremlinParams(params)
        try {
          val results = tinkerGraphServerEmbedded.client.submitCypher(query, paramsJava, null, TIME_OUT_SECONDS)
          toCypherValueRecords(query, results)
        } catch {
          case e: Exception => toExecutionFailed(e)
        }
    }
  }

  override def close(): Unit = {
    tinkerGraphServerEmbedded.client.submitGremlin(dropQuery)
  }
}

class TckTest {
  @TestFactory
  def testTck(): util.Collection[DynamicTest] = {
    val scenarios = CypherTCK.allTckScenarios
    runScenarios(scenarios)
  }

  private def runScenarios(scenarios: Seq[Scenario]) = {
    def createTestGraph() = TinkerGraphServerEmbeddedGraph

    val dynamicTests = scenarios.map { scenario =>
      val name = scenario.toString()
      val executable = scenario(createTestGraph())
      DynamicTest.dynamicTest(name, executable)
    }
    dynamicTests.asJavaCollection
  }
}
