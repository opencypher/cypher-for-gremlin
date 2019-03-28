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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

/**
 * Creates a better Cucumber report from generated json file
 * https://github.com/damianszczepanik/cucumber-reporting
 */
public class OpenCypherTckReport {
    private static final String CUCUMBER_BASE = "build/reports/tests/cucumber/";
    private static final String JSON_REPORT = CUCUMBER_BASE + "cucumber.json";

    public static void main(String[] args) throws IOException {
        generate(JSON_REPORT, CUCUMBER_BASE);
    }

    private static void generate(String jsonFilePath, String outputDirectory) throws IOException {
        File reportOutputDirectory = new File(outputDirectory);
        List<String> jsonFiles = new ArrayList<>();
        jsonFiles.add(jsonFilePath);

        // optional configuration
        Configuration configuration = new Configuration(reportOutputDirectory, "cypher-for-gremlin");
        configuration.setRunWithJenkins(false);
        configuration.setBuildNumber(System.getProperty("projectVersion"));

        // additional metadata presented on main page
        configuration.addClassifications("Platform", "Linux");
        configuration.addClassifications("Backend", "Tinker Graph");
        configuration.addClassifications("Branch", "master");
        configuration.addClassifications("Build", System.getProperty("buildNumber"));

        new ReportBuilder(jsonFiles, configuration).generateReports();

        Path path = Paths.get(CUCUMBER_BASE + "/cucumber-html-reports/overview-features.html");

        String content = new String(Files.readAllBytes(path), UTF_8);
        Document document = Jsoup.parse(content);
        document.select("#charts").remove();
        document.select("#report-lead").remove();
        document.select("script[src*=Chart]").remove();
        document.select("script").stream().filter(n -> n.data().contains("new Chart")).forEach(Node::remove);
        Files.write(path, document.html().getBytes(UTF_8));

        System.out.println("\nCucumber HTML report saved to: " + path.toUri());
    }
}
