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
import java.util.stream.Collectors;

public class TCKTestCase implements TestCase {
    private int line = 0;
    private String name;
    private String designation = "";
    private String uri = "";
    private List<TestStep> testSteps;
    private List<PickleTag> tags;

    public TCKTestCase(Pickle pickle) {
        name = pickle.getName();
        testSteps = pickle.getSteps()
            .stream()
            .map(TCKTestStep::new)
            .collect(Collectors.toList());
        tags = pickle.getTags();
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
        return designation;
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
