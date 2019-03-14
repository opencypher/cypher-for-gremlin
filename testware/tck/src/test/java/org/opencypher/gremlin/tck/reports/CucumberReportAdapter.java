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
package org.opencypher.gremlin.tck.reports;

import cucumber.api.Plugin;
import cucumber.api.Result;
import cucumber.api.TestCase;
import cucumber.api.TestStep;
import cucumber.api.event.EventListener;
import cucumber.api.event.TestCaseStarted;
import cucumber.api.event.TestRunFinished;
import cucumber.api.event.TestRunStarted;
import cucumber.api.event.TestSourceRead;
import cucumber.api.event.TestStepFinished;
import cucumber.api.event.TestStepStarted;
import cucumber.api.event.WriteEvent;
import cucumber.runner.EventBus;
import cucumber.runner.PickleTestStep;
import cucumber.runner.TimeService;
import cucumber.runtime.Env;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.UndefinedStepDefinitionMatch;
import cucumber.runtime.formatter.PluginFactory;
import gherkin.events.PickleEvent;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleStep;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opencypher.tools.tck.api.CypherValueRecords;
import org.opencypher.tools.tck.api.ExecutionFailed;
import org.opencypher.tools.tck.api.Measure;
import org.opencypher.tools.tck.api.Scenario;
import org.opencypher.tools.tck.api.SideEffects;
import org.opencypher.tools.tck.api.Step;
import org.opencypher.tools.tck.api.events.TCKEvents;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;
import scala.util.Either;

/**
 * Generates Cucumber test report for TCK scenarios.
 * <p>
 * By default cucumber.json report file is created in a working directory.
 * To configure report format and location use -Dcucumber.options="--plugin PLUGIN[:PATH_OR_URL]"
 * <p>
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
public class CucumberReportAdapter implements BeforeAllCallback, AfterAllCallback {

    private final String DEFAULT_REPORT_FILE_PATH = "cucumber.json";
    private final String DEFAULT_REPORT_FORMAT = "json";

    private Map<String, String> featureUri = new HashMap<>();
    private Map<String, Long> stepTimestamp = new HashMap<>();

    private EventBus bus = new EventBus(TimeService.SYSTEM);
    private SystemOutReader output = new SystemOutReader();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        configureCucumberPlugins();

        TCKEvents.feature().subscribe(adapt(featureEvent()));
        TCKEvents.scenario().subscribe(adapt(scenarioEvent()));
        TCKEvents.stepStarted().subscribe(adapt(stepStartedEvent()));
        TCKEvents.stepFinished().subscribe(adapt(stepFinishedEvent()));

        bus.send(new TestRunStarted(bus.getTime()));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        bus.send(new TestRunFinished(bus.getTime()));
        output.close();
    }

    private Consumer<TCKEvents.StepFinished> stepFinishedEvent() {
        return event -> {
            Step step = event.step();
            if (shouldReport(step)) {
                Long timeNow = bus.getTime();
                logOutput(step, timeNow);
                Long duration = timeNow - stepTimestamp.get(event.correlationId());
                Result.Type status = getStatus(event.result());
                PickleTestStep testStep = getTestStep(step.source());
                Result result = new Result(status, duration, errorOrNull(event.result()));
                TestStepFinished cucumberEvent = new TestStepFinished(timeNow, testStep, result);
                bus.send(cucumberEvent);
            } else {
                output.clear();
            }
        };
    }

    private Consumer<TCKEvents.StepStarted> stepStartedEvent() {
        return event -> {
            Long startedAt = bus.getTime();
            stepTimestamp.put(event.correlationId(), startedAt);
            Step step = event.step();
            if (shouldReport(step)) {
                TestStepStarted cucumberEvent = new TestStepStarted(startedAt, getTestStep(step.source()));
                bus.send(cucumberEvent);
            }
        };
    }

    @SuppressWarnings("deprecation")
    private Consumer<Scenario> scenarioEvent() {
        return scenario -> {
            Pickle pickle = scenario.source();
            List<TestStep> steps = pickle.getSteps()
                .stream()
                .map(this::getTestStep)
                .map(TestStep.class::cast)
                .collect(Collectors.toList());
            TestCase testCase = new TestCase(steps, new PickleEvent(featureUri.get(scenario.featureName()), pickle), false);
            bus.send(new TestCaseStarted(bus.getTime(), testCase));
        };
    }

    private Consumer<TCKEvents.FeatureRead> featureEvent() {
        return feature -> {
            String uri = shortenUri(feature.uri());
            featureUri.put(feature.name(), uri);
            bus.send(new TestSourceRead(bus.getTime(), uri, feature.source()));
        };
    }

    private PickleTestStep getTestStep(PickleStep source) {
        UndefinedStepDefinitionMatch definition = new UndefinedStepDefinitionMatch(source);
        return new PickleTestStep("n/a", source, definition);
    }

    private String shortenUri(String uri) {
        Path path = Paths.get(uri);
        return path.getParent().getFileName() + "/" + path.getFileName();
    }

    private Result.Type getStatus(Either<Throwable, Either<ExecutionFailed, CypherValueRecords>> result) {
        return result.isRight() ? Result.Type.PASSED : Result.Type.FAILED;
    }

    private Throwable errorOrNull(Either<Throwable, Either<ExecutionFailed, CypherValueRecords>> result) {
        return result.isLeft() ? result.left().get() : null;
    }

    private void logOutput(Step step, Long timeNow) {
        String log = output.clear();

        if (step instanceof SideEffects) {
            SideEffects sideEffects = SideEffects.class.cast(step);
            log = log + sideEffects.expected();
        }

        if (!log.isEmpty()) {
            bus.send(new WriteEvent(timeNow, log));
        }
    }

    private boolean shouldReport(Step step) {
        return !(step instanceof Measure);
    }

    private void configureCucumberPlugins() {
        List<Plugin> plugins = new RuntimeOptions(new ArrayList<>()).getPlugins();

        if (Env.INSTANCE.get("cucumber.options") == null) {
            Plugin formatter = new PluginFactory().create(DEFAULT_REPORT_FORMAT + ":" + DEFAULT_REPORT_FILE_PATH);
            plugins.add(formatter);
        }

        plugins
            .stream()
            .filter(p -> p instanceof EventListener)
            .map(EventListener.class::cast)
            .forEach(plugin -> plugin.setEventPublisher(bus));
    }

    private <T> AbstractFunction1<T, BoxedUnit> adapt(Consumer<T> procedure) {
        return new AbstractFunction1<T, BoxedUnit>() {
            @Override
            public BoxedUnit apply(T v1) {
                procedure.accept(v1);
                return BoxedUnit.UNIT;
            }
        };
    }
}
