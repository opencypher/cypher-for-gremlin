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
package org.opencypher.gremlin.tck.reports.model;

import cucumber.api.TestCase;
import cucumber.api.TestStep;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleTag;
import java.util.List;

public class TCKTestCase implements TestCase {
    private final int line;
    private final String name;
    private final String uri;
    private final List<TestStep> testSteps;
    private final List<PickleTag> tags;

    public TCKTestCase(Pickle pickle, List<TestStep> testSteps, String uri, int line) {
        this.name = pickle.getName();
        this.testSteps = testSteps;
        this.tags = pickle.getTags();
        this.uri = uri;
        this.line  = line;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getScenarioDesignation() {
        return "";
    }

    @Override
    public List<PickleTag> getTags() {
        return tags;
    }

    @Override
    public List<TestStep> getTestSteps() {
        return testSteps;
    }

    @Override
    public String getUri() {
        return uri;
    }
}
