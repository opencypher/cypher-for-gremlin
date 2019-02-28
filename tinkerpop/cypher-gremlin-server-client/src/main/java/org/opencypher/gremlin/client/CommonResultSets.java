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
package org.opencypher.gremlin.client;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.opencypher.gremlin.translation.CypherAst;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.translator.Translator;

final class CommonResultSets {
    private CommonResultSets() {
    }

    static CypherResultSet explain(CypherAst ast) {
        Map<String, Object> explanation = new LinkedHashMap<>();
        Translator<String, GroovyPredicate> translator = Translator.builder()
            .gremlinGroovy()
            .inlineParameters()
            .build();
        explanation.put("translation", ast.buildTranslation(translator));
        explanation.put("options", ast.getOptions().toString());
        Iterator<Result> iterator = singletonIterator(() -> new Result(explanation));
        return new CypherResultSet(iterator);
    }

    static CypherResultSet exceptional(Throwable throwable) {
        return new CypherResultSet(singletonIterator(() -> {
            throw new RuntimeException(throwable);
        }));
    }

    private static <R> Iterator<R> singletonIterator(Supplier<R> supplier) {
        return new Iterator<R>() {
            private boolean done;

            @Override
            public boolean hasNext() {
                return !done;
            }

            @Override
            public R next() {
                if (done) {
                    throw new NoSuchElementException();
                }
                done = true;
                return supplier.get();
            }
        };
    }
}
