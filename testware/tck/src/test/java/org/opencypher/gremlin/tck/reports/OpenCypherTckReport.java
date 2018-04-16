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
package org.opencypher.gremlin.tck.reports;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;

/**
 * Creates a better Cucumber report from generated json file
 * https://github.com/damianszczepanik/cucumber-reporting
 */
public class OpenCypherTckReport {

    private static final String CUCUMBER_BASE = "build/reports/tests/cucumber/";
    private static final String JSON_REPORT = CUCUMBER_BASE + "cucumber.json";

    public static void main(String[] args) {
        generate(JSON_REPORT, CUCUMBER_BASE);
    }

    private static void generate(String jsonFilePath, String outputDirectory) {
        File reportOutputDirectory = new File(outputDirectory);
        List<String> jsonFiles = new ArrayList<>();
        jsonFiles.add(jsonFilePath);

        // optional configuration
        Configuration configuration = new Configuration(reportOutputDirectory, "cypher-for-gremlin");
        configuration.setParallelTesting(false);
        configuration.setRunWithJenkins(false);
        configuration.setBuildNumber(System.getProperty("projectVersion"));

        // additional metadata presented on main page
        configuration.addClassifications("Platform", "Linux");
        configuration.addClassifications("Branch", "master");

        String backend = "Tinker Graph";

        configuration.addClassifications("Backend", backend);

        new ReportBuilder(jsonFiles, configuration).generateReports();

        File result = new File(CUCUMBER_BASE + "/cucumber-html-reports/overview-features.html");
        System.out.println("\n" +
            "================================================================================\n" +
            "| Cucumber HTML report generated successfully:\n" +
            "| " + result.getAbsolutePath() + "\n" +
            "================================================================================");
    }
}
