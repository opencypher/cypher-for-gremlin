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
package org.opencypher.gremlin.tck.regression;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Collections2.filter;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.ListUtils.subtract;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import freemarker.template.TemplateException;

public class TckResultsComparator {
    public static void main(String[] args) throws IOException, TemplateException {
        Features before = getFeatures("build/test-results/junit-platform/TEST-junit-jupiter.xml");
        Features now = getFeatures("build/test-results/junit-platform/TEST-junit-jupiter-before.xml");
        Diff diff = now.compare(before);
        TckRegressionReport.generate(diff);

        if (!diff.newlyFailedScenarios.isEmpty()) {
            System.exit(1);
        }
    }

    private static Features getFeatures(String reportPath) throws IOException {
        File reportFile = new File(reportPath);
        checkState(reportFile.exists(), format("File %s doesn't exist, but is required for TCK reports comparison.", reportPath));
        ObjectMapper xmlMapper = new XmlMapper();
        xmlMapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        xmlMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        Feature value = xmlMapper.readValue(reportFile, Feature.class);
        return new Features(new Feature[]{value});
    }

    public static class Features {

        List<Scenario> scenarios = new ArrayList<>();

        List<Scenario> passingScenarios = new ArrayList<>();

        List<Scenario> failedScenarios = new ArrayList<>();

        public Features(Feature[] features) {
            for (Feature feature : features) {
                List<Scenario> featureScenarios = feature.testcase.stream().filter(s -> s.name != null).collect(toList());
                scenarios.addAll(featureScenarios);
                passingScenarios.addAll(filter(featureScenarios, Scenario::isPassed));
                failedScenarios.addAll(filter(featureScenarios, Scenario::isFailed));
            }
        }

        @SuppressWarnings("unchecked")
        public Diff compare(Features before) {
            String errorMessage = format("Can't compare: scenarios size changed from %s to %s", before.scenarios.size(), this.scenarios.size());
            checkState(this.scenarios.size() == before.scenarios.size(), errorMessage);

            List<Scenario> newlyPassingScenarios = subtract(this.passingScenarios, before.passingScenarios);
            List<Scenario> newlyFailedScenarios = subtract(this.failedScenarios, before.failedScenarios);
            int totalPassingScenarios = passingScenarios.size();
            int totalScenarios = scenarios.size();
            return new Diff(newlyPassingScenarios, newlyFailedScenarios,
                totalPassingScenarios, totalScenarios, scenarios);
        }

    }

    public static class Diff {

        List<Scenario> newlyPassingScenarios;

        List<Scenario> newlyFailedScenarios;

        int totalPassingScenarios;

        int totalScenarios;

        List<Scenario> allScenarios;

        String passingPercentage;

        public Diff(List<Scenario> newlyPassingScenarios, List<Scenario> newlyFailedScenarios,
                    int totalPassingScenarios, int totalScenarios, List<Scenario> allScenarios) {
            this.newlyPassingScenarios = newlyPassingScenarios;
            this.newlyFailedScenarios = newlyFailedScenarios;
            this.totalPassingScenarios = totalPassingScenarios;
            this.totalScenarios = totalScenarios;
            this.allScenarios = allScenarios;
            this.passingPercentage = new DecimalFormat("#.##").format((0.0 + totalPassingScenarios) / totalScenarios * 100) + "%";
        }

        public List<Scenario> getNewlyPassingScenarios() {
            return newlyPassingScenarios;
        }

        public List<Scenario> getNewlyFailedScenarios() {
            return newlyFailedScenarios;
        }

        public int getTotalPassingScenarios() {
            return totalPassingScenarios;
        }

        public int getTotalScenarios() {
            return totalScenarios;
        }

        public String getPassingPercentage() {
            return passingPercentage;
        }

        public List<Scenario> getAllScenarios() {
            return allScenarios;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JacksonXmlRootElement(localName = "testsuite")
    static class Feature {
        private String name;

        @JacksonXmlElementWrapper(localName = "testcase", useWrapping = false)
        private List<Scenario> testcase;

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Error {
        String message;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Scenario {
        private static Pattern pattern = Pattern.compile("Feature \"(.*?)\": Scenario \"(.*?)\"");

        private String name;

        private String featureName;

        private Error error;

        @JsonCreator
        public Scenario(@JsonProperty("name") String name, @JsonProperty("error") Error error) {
            Matcher matcher = pattern.matcher(name);
            matcher.find();
            this.name = matcher.group(2);
            this.featureName = matcher.group(1);
            this.error = error;
        }

        public boolean isFailed() {
            return error != null;
        }

        public boolean isPassed() {
            return !isFailed();
        }

        public String getStatus() {
            return isPassed() ? "Passed" : "Failed";
        }

        public String getName() {
            Matcher matcher = pattern.matcher(name);
            matcher.find();
            return name;
        }

        public String getFeatureName() {
            return featureName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Scenario scenario = (Scenario) o;
            return name != null ? name.equals(scenario.name) : scenario.name == null;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}
