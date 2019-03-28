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

import cucumber.api.PickleStepTestStep;
import gherkin.pickles.PickleStep;
import java.util.ArrayList;
import java.util.List;

public class TCKTestStep implements PickleStepTestStep {
    private PickleStep step;
    private String uri;
    private int line;

    public TCKTestStep(PickleStep step, String uri, int line) {
        this.step = step;
        this.uri = uri;
        this.line = line;
    }

    @Override
    public PickleStep getPickleStep() {
        return step;
    }

    @Override
    public String getStepLocation() {
        return uri + ":" + getStepLine();
    }

    @Override
    public int getStepLine() {
        return line;
    }

    @Override
    public String getStepText() {
        return step.getText();
    }

    @Override
    public List<cucumber.api.Argument> getDefinitionArgument() {
        return new ArrayList<>();
    }

    @Override
    public List<gherkin.pickles.Argument> getStepArgument() {
        return step.getArgument();
    }

    @Override
    public String getPattern() {
        return  "";
    }

    @Override
    public String getCodeLocation() {
        return "";
    }
}
