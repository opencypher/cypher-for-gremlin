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

/**
 * Translator features are additional behaviors that can be allowed in translation.
 * These need to be enabled individually when creating a {@link Translator}.
 */
public enum TranslatorFeature {
    /**
     * Support for custom functions and predicates
     * provided by the CfoG Gremlin Server plugin.
     */
    CYPHER_EXTENSIONS,

    /**
     * Support for specifying multiple labels for a vertex
     * and matching by multiple labels.
     */
    MULTIPLE_LABELS
}
