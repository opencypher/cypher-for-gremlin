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

import java.util
import java.util.concurrent.TimeUnit.SECONDS

import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.{DynamicTest, TestFactory}
import org.opencypher.gremlin.extension.{CypherProcedure, CypherProcedureProvider, CypherProcedureRegistrar}
import org.opencypher.gremlin.rules.GremlinServerExternalResource
import org.opencypher.gremlin.tck.GremlinQueries._
import org.opencypher.gremlin.tck.TckGremlinCypherValueConverter._
import org.opencypher.gremlin.tck.reports.CucumberReportAdapter
import org.opencypher.gremlin.traversal.ProcedureRegistry
import org.opencypher.tools.tck.api._
import org.opencypher.tools.tck.values.CypherValue

import scala.collection.JavaConverters._

object TinkerGraphServerEmbeddedGraph extends Graph with ProcedureSupport {
  val TIME_OUT_SECONDS = 10

  val tinkerGraphServerEmbedded = new GremlinServerExternalResource
  tinkerGraphServerEmbedded.before()
  tinkerGraphServerEmbedded.gremlinClient().submit(dropQuery).all().join()

  override def cypher(query: String, params: Map[String, CypherValue], queryType: QueryType): Result = {
    queryType match {
      case SideEffectQuery if cypherToGremlinQueries.isDefinedAt(query) =>
        val resultSet = tinkerGraphServerEmbedded.gremlinClient().submit(cypherToGremlinQueries(query))
        toCypherValueRecords(query, ResultTransformer.resultSetAsMaps(resultSet))

      case ExecQuery | InitQuery | SideEffectQuery =>
        try {
          val paramsJava: util.Map[String, Object] = toGremlinParams(params)
          val results = tinkerGraphServerEmbedded
            .cypherGremlinClient()
            .submitAsync(query, paramsJava)
            .get(TIME_OUT_SECONDS, SECONDS)
            .all()
          toCypherValueRecords(query, results)
        } catch {
          case e: Exception => toExecutionFailed(e)
        }
    }
  }

  private val signaturePattern = """^([\w\.]+)\(([^)]*)\) :: (?:\(([^)]*)\)|VOID)""".r("name", "arguments", "results")
  private val argumentPattern = """(\w+) :: ([\w\?]+)""".r("name", "type")

  override def registerProcedure(signature: String, values: CypherValueRecords): Unit = {
    val signatureMatch = signaturePattern.findFirstMatchIn(signature).get
    val name = signatureMatch.group("name")
    val argumentNames = argumentPattern.findAllMatchIn(signatureMatch.group("arguments")).map(_.group("name")).toList
    val resultNames = Option(signatureMatch.group("results"))
      .map(argumentPattern.findAllMatchIn(_).map(_.group("name")).toList)
      .getOrElse(Nil)

    ProcedureRegistry.clear()
    ProcedureRegistry.register(new CypherProcedureProvider {
      override def apply(registry: CypherProcedureRegistrar): Unit = {
        registry.register(
          name,
          new CypherProcedure {
            override def call(arguments: AnyRef*): util.List[util.Map[String, AnyRef]] = {
              val results = values.rows.filter { row =>
                val resultArguments = argumentNames
                  .map(row(_))
                  .map(fromCypherValue)
                arguments == resultArguments
              }.map { result =>
                val resultMap = resultNames
                  .map(key => (key, fromCypherValue(result(key)).asInstanceOf[AnyRef]))
                  .toMap
                new util.HashMap[String, AnyRef](resultMap.asJava)
              }
              new util.ArrayList(results.asJava)
            }
          }
        )
      }
    })
  }

  override def close(): Unit = {
    tinkerGraphServerEmbedded.gremlinClient().submit(dropQuery).all().join()
  }
}

@ExtendWith(Array(classOf[CucumberReportAdapter]))
class TckTest {
  @TestFactory
  def testTck(): util.Collection[DynamicTest] = {
    val scenarioName = System.getProperty("scenario")
    val featureName = System.getProperty("feature")

    val scenarios = CypherTCK.allTckScenarios
      .filter(s => Set(null, "", s.name).contains(scenarioName))
      .filter(s => Set(null, "", s.featureName).contains(featureName))

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
