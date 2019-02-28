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
package org.opencypher.gremlin.tck.regression;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import org.opencypher.gremlin.tck.regression.TckResultsComparator.Diff;

public class TckRegressionReport {
    public static void generate(Diff diff) throws IOException, TemplateException {
        File resourcesDir = new File("src/test/resources/");
        File outputDir = new File("build/reports/tests/");
        File report = new File(outputDir, "regression.html");

        Configuration cfg = new Configuration(VERSION_2_3_23);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(RETHROW_HANDLER);
        cfg.setTemplateLoader(new FileTemplateLoader(resourcesDir));

        Map<String, Object> input = new HashMap<>();
        input.put("diff", diff);

        try (Writer fileWriter = new FileWriter(report)) {
            copy(resourcesDir, outputDir, "success.png");
            Template template = cfg.getTemplate("Regression.ftl");
            template.process(input, fileWriter);
            System.out.println("\nRegression report saved to " + report.toURI());
        }
    }

    private static void copy(File resourcesDir, File outputDir, String filename) throws IOException {
        File src = new File(resourcesDir, filename);
        File dst = new File(outputDir, filename);
        if (!dst.exists()) {
            Files.copy(src.toPath(), dst.toPath());
        }
    }

}
