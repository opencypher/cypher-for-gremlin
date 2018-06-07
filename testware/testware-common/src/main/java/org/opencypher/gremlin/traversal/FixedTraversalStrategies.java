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
package org.opencypher.gremlin.traversal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.ConnectiveStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.finalization.ProfileStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.AdjacentToIncidentStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.CountStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.FilterRankingStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.IncidentToAdjacentStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.InlineFilterStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.LazyBarrierStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.MatchPredicateStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.PathRetractionStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.RepeatUnrollStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.verification.StandardVerificationStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversalStrategies;
import org.apache.tinkerpop.gremlin.tinkergraph.process.traversal.strategy.optimization.TinkerGraphCountStrategy;
import org.apache.tinkerpop.gremlin.tinkergraph.process.traversal.strategy.optimization.TinkerGraphStepStrategy;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

/** Applies strategies in the same order as provided in addStrategies().
 */
public class FixedTraversalStrategies extends DefaultTraversalStrategies {

    @SuppressWarnings("unchecked")
    public static void tinkerGraph() {
        TraversalStrategies strategies = TraversalStrategies.GlobalCache.getStrategies(TinkerGraph.class);
        List<TraversalStrategy<?>> original = new ArrayList<>(strategies.toList());
        List<TraversalStrategy> reordered = Arrays.asList(
            ConnectiveStrategy.instance(),
            IncidentToAdjacentStrategy.instance(),
            MatchPredicateStrategy.instance(),
            FilterRankingStrategy.instance(),
            InlineFilterStrategy.instance(),
            CountStrategy.instance(),
            RepeatUnrollStrategy.instance(),
            PathRetractionStrategy.instance(),
            AdjacentToIncidentStrategy.instance(),
            LazyBarrierStrategy.instance(),
            TinkerGraphCountStrategy.instance(),
            TinkerGraphStepStrategy.instance(),
            ProfileStrategy.instance(),
            StandardVerificationStrategy.instance());

        assert original.size() == reordered.size() && original.containsAll(reordered);

        FixedTraversalStrategies fixed = new FixedTraversalStrategies();
        fixed.addStrategies(
            reordered.toArray(new TraversalStrategy[reordered.size()]));
        TraversalStrategies.GlobalCache.registerStrategies(TinkerGraph.class, fixed);
    }

    @Override
    public TraversalStrategies addStrategies(TraversalStrategy<?>... strategies) {
        final List<TraversalStrategy<?>> concurrent = new ArrayList<>(this.traversalStrategies);
        for (final TraversalStrategy<?> addStrategy : strategies) {
            for (final TraversalStrategy<?> currentStrategy : concurrent) {
                if (addStrategy.getClass().equals(currentStrategy.getClass()))
                    this.traversalStrategies.remove(currentStrategy);
            }
        }
        Collections.addAll(this.traversalStrategies, strategies);
        return this;
    }
}
