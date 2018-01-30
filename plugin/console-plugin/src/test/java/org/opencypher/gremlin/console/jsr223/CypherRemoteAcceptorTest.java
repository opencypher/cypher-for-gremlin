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
package org.opencypher.gremlin.console.jsr223;

import org.junit.Test;
import org.opencypher.gremlin.translation.Flavor;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.opencypher.gremlin.console.jsr223.CypherRemoteAcceptor.configureQueryHandler;

public class CypherRemoteAcceptorTest {

    @Test
    public void configureQueryHandler_shouldReturnSimpleHandler() {
        QueryHandler queryHandler = configureQueryHandler(singletonList(""));

        assertThat(queryHandler).isInstanceOf(SimpleQueryHandler.class);
    }

    @Test
    public void configureQueryHandler_shouldReturnTranslatingHandler() {
        QueryHandler queryHandler = configureQueryHandler(asList("translate", "cosmosdb"));

        assertThat(queryHandler).isInstanceOf(TranslatingQueryHandler.class);
        assertThat(((TranslatingQueryHandler) queryHandler).flavor).isEqualTo(Flavor.COSMOSDB);
    }

    @Test
    public void configureQueryHandler_shouldReturnTranslatingHandlerWithDefaultFlavor() {
        QueryHandler queryHandler = configureQueryHandler(singletonList("translate"));

        assertThat(queryHandler).isInstanceOf(TranslatingQueryHandler.class);
        assertThat(((TranslatingQueryHandler) queryHandler).flavor).isEqualTo(Flavor.GREMLIN);
    }
}
