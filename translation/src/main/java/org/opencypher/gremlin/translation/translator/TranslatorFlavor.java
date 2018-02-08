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
package org.opencypher.gremlin.translation.translator;

import org.opencypher.gremlin.translation.GremlinSteps;

import java.util.function.Function;

import static java.util.function.Function.identity;

/**
 * A flavor defines specialized behavior of a {@link Translator}.
 */
public final class TranslatorFlavor<T, P> {
    private final Function<GremlinSteps<T, P>, GremlinSteps<T, P>> translationBuilderDecorator;

    private TranslatorFlavor(Function<GremlinSteps<T, P>, GremlinSteps<T, P>> translationBuilderDecorator) {
        this.translationBuilderDecorator = translationBuilderDecorator;
    }

    GremlinSteps<T, P> decorateTranslationBuilder(GremlinSteps<T, P> gremlinSteps) {
        return translationBuilderDecorator.apply(gremlinSteps);
    }

    /**
     * Returns a translator flavor that is suitable
     * for Gremlin Server or a compatible graph database.
     * <p>
     * This is the default flavor.
     *
     * @param <T> translation target type
     * @param <P> predicate target type
     * @return translator flavor
     */
    public static <T, P> TranslatorFlavor<T, P> gremlinServer() {
        return new TranslatorFlavor<>(identity());
    }

    /**
     * Returns a translator flavor that is suitable
     * for Cosmos DB.
     *
     * @param <T> translation target type
     * @param <P> predicate target type
     * @return translator flavor
     */
    public static <T, P> TranslatorFlavor<T, P> cosmosdb() {
        return new TranslatorFlavor<>(
            translationBuilder ->
                new CustomFunctionsGremlinStepsDecorator<>(
                    new CosmosDbGremlinStepsDecorator<>(
                        translationBuilder
                    )
                )
        );
    }
}
