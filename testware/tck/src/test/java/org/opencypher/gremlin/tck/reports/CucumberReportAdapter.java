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

import cucumber.api.PickleStepTestStep;
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
import cucumber.runner.CanonicalOrderEventPublisher;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.formatter.PluginFactory;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleLocation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opencypher.gremlin.tck.reports.model.TCKTestCase;
import org.opencypher.gremlin.tck.reports.model.TCKTestStep;
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
    private final String DEFAULT_FORMATTER_PLUGIN = "json:cucumber.json";

    private final CanonicalOrderEventPublisher bus = new CanonicalOrderEventPublisher();
    private final SystemOutReader output = new SystemOutReader();

    private final Map<String, String> featureNameToUri = new HashMap<>();
    private final Map<String, Long> stepTimestamp = new HashMap<>();

    private TestCase currentTestCase;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        RuntimeOptions options = new RuntimeOptions("");
        String cucumberOptions = options.getPluginFormatterNames().stream().findFirst().orElse(DEFAULT_FORMATTER_PLUGIN);

        PluginFactory pluginFactory = new PluginFactory();
        EventListener json = (EventListener) pluginFactory.create(cucumberOptions);
        json.setEventPublisher(bus);

        TCKEvents.feature().subscribe(adapt(featureReadEvent()));
        TCKEvents.scenario().subscribe(adapt(scenarioStartedEvent()));
        TCKEvents.stepStarted().subscribe(adapt(stepStartedEvent()));
        TCKEvents.stepFinished().subscribe(adapt(stepFinishedEvent()));

        bus.handle(new TestRunStarted(time()));
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        bus.handle(new TestRunFinished(time()));
        output.close();
    }

    private Consumer<TCKEvents.FeatureRead> featureReadEvent() {
        return feature -> {
            featureNameToUri.put(feature.name(), feature.uri());
            bus.handle(new TestSourceRead(time(), feature.uri(), feature.source()));
        };
    }

    private Consumer<Scenario> scenarioStartedEvent() {
        return scenario -> {
            String featureUri = featureNameToUri.get(scenario.featureName());
            Pickle pickle = scenario.source();
            List<TestStep> steps = pickle.getSteps()
                .stream()
                .map(step -> new TCKTestStep(step, featureUri, outlineLocation(step.getLocations())))
                .collect(Collectors.toList());
            int line = outlineLocation(pickle.getLocations());
            currentTestCase = new TCKTestCase(pickle, steps, featureUri, line);
            bus.handle(new TestCaseStarted(time(), currentTestCase));
        };
    }

    private Consumer<TCKEvents.StepStarted> stepStartedEvent() {
        return event -> {
            Long startedAt = time();
            stepTimestamp.put(event.correlationId(), startedAt);
            Step step = event.step();
            if (shouldReport(step)) {
                int line = outlineLocation(step.source().getLocations());
                TCKTestStep testStep = new TCKTestStep(step.source(), currentTestCase.getUri(), line);
                TestStepStarted cucumberEvent = new TestStepStarted(startedAt, currentTestCase, testStep);
                bus.handle(cucumberEvent);
            }
        };
    }

    private Consumer<TCKEvents.StepFinished> stepFinishedEvent() {
        return event -> {
            Step step = event.step();
            if (shouldReport(step)) {
                Long timeNow = time();
                logOutput(step, timeNow);
                Long duration = timeNow - stepTimestamp.get(event.correlationId());
                Result.Type status = getStatus(event.result());
                int line = outlineLocation(step.source().getLocations());
                PickleStepTestStep testStep = new TCKTestStep(step.source(), currentTestCase.getUri(), line);
                Result result = new Result(status, duration, errorOrNull(event.result()));
                TestStepFinished cucumberEvent = new TestStepFinished(timeNow, currentTestCase, testStep, result);
                bus.handle(cucumberEvent);
            } else {
                output.clear();
            }
        };
    }

    private void logOutput(Step step, Long timeNow) {
        String log = output.clear();

        if (step instanceof SideEffects) {
            SideEffects sideEffects = (SideEffects) step;
            log = log + sideEffects.expected();
        }

        if (!log.isEmpty()) {
            bus.handle(new WriteEvent(timeNow, currentTestCase, log));
        }
    }

    private boolean shouldReport(Step step) {
        return !(step instanceof Measure);
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

    private Result.Type getStatus(Either<Throwable, Either<ExecutionFailed, CypherValueRecords>> result) {
        return result.isRight() ? Result.Type.PASSED : Result.Type.FAILED;
    }

    private Throwable errorOrNull(Either<Throwable, Either<ExecutionFailed, CypherValueRecords>> result) {
        return result.isLeft() ? result.left().get() : null;
    }

    private Long time() {
        return System.currentTimeMillis();
    }

    private int outlineLocation(List<PickleLocation> locations) {
        return locations.get(locations.size() - 1).getLine();
    }
}
