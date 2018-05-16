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
package org.opencypher.gremlin.neo4j.driver;

import org.opencypher.gremlin.translation.translator.TranslatorFlavor;

/**
 * A configuration class to config CfoG properties.
 * <p>
 * To create a config:
 * <pre>
 * {@code
 *     Config config = Config.build()
 *         .withTranslation(TranslatorFlavor.gremlinServer())
 *         .toConfig();
 * }
 * </pre>
 */
public class Config {
    private TranslatorFlavor flavor;

    private Config(ConfigBuilder configBuilder) {
        flavor = configBuilder.flavor;
    }

    /**
     * @return A config with all default settings
     */
    public static Config defaultConfig() {
        return Config.build().toConfig();
    }

    /**
     * @return {@link TranslatorFlavor} if {@link #translationEnabled()}, null otherwise
     */
    public TranslatorFlavor flavor() {
        return flavor;
    }

    /**
     * @return true if Cypher Query is translated to Gremlin before sending it to Gremlin Server
     */
    public boolean translationEnabled() {
        return flavor != null;
    }

    /**
     * Return a {@link ConfigBuilder} instance.
     *
     * @return a {@link ConfigBuilder} instance
     */
    public static ConfigBuilder build() {
        return new ConfigBuilder();
    }

    /**
     * Builder for new {@link Config} instances.
     */
    public static class ConfigBuilder {
        private TranslatorFlavor flavor;

        private ConfigBuilder() {
        }

        /**
         * Translate Cypher query to Gremlin before sending it to Gremlin Server.
         *
         * @return a {@link ConfigBuilder} instance
         */
        public ConfigBuilder withTranslation() {
            this.flavor = TranslatorFlavor.gremlinServer();
            return this;
        }

        /**
         * Translate Cypher query to Gremlin before sending it to Gremlin Server.
         *
         * @param flavor {@link TranslatorFlavor} of translation
         * @return a {@link ConfigBuilder} instance
         */
        public ConfigBuilder withTranslation(TranslatorFlavor flavor) {
            this.flavor = flavor;
            return this;
        }

        /**
         * Create a config instance from this builder.
         *
         * @return a {@link Config} instance
         */
        public Config toConfig() {
            return new Config(this);
        }
    }
}
