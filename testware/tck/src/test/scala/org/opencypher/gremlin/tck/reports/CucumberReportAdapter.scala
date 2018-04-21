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
package org.opencypher.gremlin.tck.reports

import java.nio.file.Paths
import java.{lang, util}

import cucumber.api._
import cucumber.api.event.{
  EventListener,
  TestCaseStarted,
  TestRunFinished,
  TestRunStarted,
  TestSourceRead,
  TestStepFinished,
  TestStepStarted,
  WriteEvent
}
import cucumber.api.formatter.Formatter
import cucumber.runner.{EventBus, PickleTestStep, TimeService}
import cucumber.runtime.formatter.PluginFactory
import cucumber.runtime.{Argument, DefinitionMatch, Env, Match, RuntimeOptions}
import gherkin.events.PickleEvent
import gherkin.pickles.PickleStep
import org.junit.jupiter.api.extension.{AfterAllCallback, BeforeAllCallback, ExtensionContext}
import org.opencypher.gremlin.tck.reports.tools.{CucumberEventFactory, SystemOutReader}
import org.opencypher.tools.tck.api.events.TCKEvents
import org.opencypher.tools.tck.api.events.TCKEvents.StepResult
import org.opencypher.tools.tck.api.{Measure, SideEffects, Step}

import scala.collection.JavaConversions._

/** Generates Cucumber test report for TCK scenarios.
  *
  * By default cucumber.json report file is created in a working directory.
  * To configure report format and location use -Dcucumber.options="--plugin PLUGIN[:PATH_OR_URL]"
  *
  * Usage:
  * Annotate test class with [[org.junit.jupiter.api.extension.ExtendWith]]
  * <pre>{@code
  *
  * @literal @ExtendWith(Array(classOf[CucumberReportAdapter]))
  * class TckTest {
  *
  * @literal @TestFactory
  * def testStandardTCK(): util.Collection[DynamicTest] = { }
  * }
  *
  * }</pre>
  */
class CucumberReportAdapter() extends BeforeAllCallback with AfterAllCallback {

  private val DefaultReportFilePath = "cucumber.json"
  private val DefaultReportFormat = "json"

  private val featureUri = scala.collection.mutable.Map[String, String]()
  private val stepTimestamp = scala.collection.mutable.Map[String, Long]()
  private val stepUri = ""

  private val bus = new EventBus(TimeService.SYSTEM)
  private val output = new SystemOutReader

  override def beforeAll(context: ExtensionContext): Unit = {
    init()
  }

  override def afterAll(context: ExtensionContext): Unit = {
    close()
  }

  private def init(): Unit = {

    configureCucumberPlugins()
    bus.send(new TestRunStarted(bus.getTime))

    TCKEvents.feature.subscribe(feature => {
      val uri = shortenUri(feature.uri)
      featureUri += (feature.name -> uri)
      bus.send(new TestSourceRead(bus.getTime, uri, feature.source))
    })

    TCKEvents.scenario.subscribe(scenario => {
      val pickle = scenario.source
      val steps: util.List[TestStep] = mapTestSteps(pickle.getSteps)
      val testCase = CucumberEventFactory.testCase(steps, new PickleEvent(featureUri(scenario.featureName), pickle))
      bus.send(new TestCaseStarted(bus.getTime, testCase))
    })

    TCKEvents.stepStarted.subscribe(event => {
      val startedAt = bus.getTime
      stepTimestamp += (event.correlationId -> startedAt)
      val step = event.step
      if (shouldReport(step)) {
        logStep(step)
        val cucumberEvent = new TestStepStarted(startedAt, new PickleTestStep(stepUri, step.source, definitionMatch))
        bus.send(cucumberEvent)
      }
    })

    TCKEvents.stepFinished.subscribe(event => {
      val step = event.step
      if (shouldReport(step)) {
        logStep(step, finished = true)
        val timeNow = bus.getTime
        logOutput(timeNow)
        val duration = timeNow - stepTimestamp(event.correlationId)
        val status = getStatus(event.result)
        val testStep = new PickleTestStep(stepUri, step.source, definitionMatch)
        val cucumberEvent =
          new TestStepFinished(timeNow, testStep, new Result(status, duration, event.result.left.getOrElse(null)))

        bus.send(cucumberEvent)
      } else {
        output.clear()
      }
    })

    def shortenUri(uri: String) = {
      val path = Paths.get(uri)
      path.getParent.getFileName.toString.concat("/").concat(path.getFileName.toString)
    }
  }

  private def getStatus(result: StepResult): Result.Type = {
    val status = if (result.isRight) {
      Result.Type.PASSED
    } else {
      Result.Type.FAILED
    }
    status
  }

  private def logStep(step: Step, finished: Boolean = false): Unit = {
    step match {
      case Measure(_)                       => println("TCK: Side effect measurement ".concat(if (finished) "finished" else "started"))
      case SideEffects(diff, _) if finished => println(diff)
      case _                                =>
    }
  }

  private def logOutput(timeNow: lang.Long): Unit = {
    val log = output.readOutput()
    if (!log.isEmpty) {
      bus.send(new WriteEvent(timeNow, log))
    }
  }

  private def shouldReport(s: Step): Boolean = s match {
    case _: Measure => false
    case _          => true
  }

  private def mapTestSteps(steps: util.List[PickleStep]): util.List[TestStep] = {
    for (step <- steps) yield new PickleTestStep(stepUri, step, definitionMatch).asInstanceOf[TestStep]
  }

  private def definitionMatch = {
    new DefinitionMatch {
      override def getMatch: Match = null

      override def getCodeLocation: String = null

      override def runStep(language: String, scenario: Scenario): Unit = throw new NotImplementedError

      override def getArguments: util.List[Argument] = util.Arrays.asList()

      override def getPattern: String = null

      override def dryRunStep(language: String, scenario: Scenario): Unit = throw new NotImplementedError
    }
  }

  private def configureCucumberPlugins(): Unit = {
    val plugins = new RuntimeOptions(seqAsJavaList(List.empty[String])).getPlugins

    if (Env.INSTANCE.get("cucumber.options") == null) {
      val formatter = new PluginFactory().create(s"$DefaultReportFormat:$DefaultReportFilePath").asInstanceOf[Formatter]
      plugins += formatter
    }

    for (plugin <- plugins) {
      plugin match {
        case listener: EventListener =>
          listener.setEventPublisher(bus)
        case _ =>
      }
    }
  }

  private def close(): Unit = {
    bus.send(new TestRunFinished(bus.getTime))
    output.close()
  }
}
