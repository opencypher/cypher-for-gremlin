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
package org.opencypher.gremlin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.opencypher.gremlin.ClientServerCommunication.CYPHER_OP_PROCESSOR_NAME;
import static org.opencypher.gremlin.ClientServerCommunication.buildRequest;

import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.junit.Test;

public class ClientServerCommunicationTest {

    @Test
    public void createRequestTest() throws Exception {
        RequestMessage request = buildRequest("cypher").create();

        assertThat(request.getOp()).isEqualTo(Tokens.OPS_EVAL);
        assertThat(request.getProcessor()).isEqualTo(CYPHER_OP_PROCESSOR_NAME);
        assertThat(request.getArgs()).containsOnly(
            entry(Tokens.ARGS_GREMLIN, "cypher")
        );
    }
}
