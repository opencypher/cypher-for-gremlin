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

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;

interface GremlinDriver extends Driver {

    default Session session(AccessMode mode) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    default Session session(String bookmark) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    default Session session(AccessMode mode, String bookmark) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    default Session session(Iterable<String> bookmarks) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }

    default Session session(AccessMode mode, Iterable<String> bookmarks) {
        throw new UnsupportedOperationException("Transactions are currently not supported");
    }
}
