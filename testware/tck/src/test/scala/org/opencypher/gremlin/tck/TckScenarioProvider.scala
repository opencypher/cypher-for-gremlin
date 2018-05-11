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

import org.opencypher.tools.tck.api.events.TCKEvents
import org.opencypher.tools.tck.api.events.TCKEvents.Publish
import org.opencypher.tools.tck.api.{CypherTCK, Scenario, Step}
import purecsv.unsafe.CSVReader

object TckScenarioProvider {

  val skipStep: Publish[Step] = Publish[Step]()
  private val allScenarios = CypherTCK.allTckScenarios

  def filterUserSelected(scenarios: Seq[Scenario]): Seq[Scenario] = {
    val scenarioName = System.getProperty("scenario")
    val featureName = System.getProperty("feature")

    scenarios
      .filter(s => scenarioName == null || s.name == scenarioName)
      .filter(s => featureName == null || s.featureName == featureName)
  }

  def getRunnableScenarios: Seq[Scenario] = {
    val exclusions = readExclusions()
    val scenarios = allScenarios.filter(s => !exclusions.contains(ScenarioId(s.featureName, s.name)))
    filterUserSelected(scenarios)
  }

  def getIgnoredScenarios: Seq[Scenario] = {
    val exclusions = readExclusions()
    val scenarios = allScenarios.filter(s => exclusions.contains(ScenarioId(s.featureName, s.name)))
    assert(exclusions.lengthCompare(scenarios.size) == 0, "Ignored scenario count mismatch")
    filterUserSelected(scenarios)
  }

  def readExclusions(): Seq[ScenarioId] = {
    CSVReader[ScenarioId].readCSVFromFileName("src/test/resources/tck-wontfix.csv")
  }

  case class ScenarioId(featureName: String, name: String)

  def skipIgnoredScenarios: Unit = {
    getIgnoredScenarios.foreach(s => {
      TCKEvents.setScenario(s);
      s.steps.foreach(step => {
        skipStep.send(step);
      })
    })
  }
}
