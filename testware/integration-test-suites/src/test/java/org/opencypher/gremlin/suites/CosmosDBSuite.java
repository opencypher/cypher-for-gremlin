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
package org.opencypher.gremlin.suites;

import org.junit.experimental.categories.Categories;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.opencypher.gremlin.groups.SkipWithCosmosDB;
import org.opencypher.gremlin.groups.UsesCollectionsInProperties;
import org.opencypher.gremlin.groups.UsesExtensions;
import org.opencypher.gremlin.queries.CaseTest;
import org.opencypher.gremlin.queries.CastTest;
import org.opencypher.gremlin.queries.ComparisonTest;
import org.opencypher.gremlin.queries.ComplexExamplesTest;
import org.opencypher.gremlin.queries.ContainerIndexTest;
import org.opencypher.gremlin.queries.CreateTest;
import org.opencypher.gremlin.queries.DeleteTest;
import org.opencypher.gremlin.queries.ExplainTest;
import org.opencypher.gremlin.queries.ExpressionTest;
import org.opencypher.gremlin.queries.FunctionTest;
import org.opencypher.gremlin.queries.ListComprehensionTest;
import org.opencypher.gremlin.queries.ListSliceTest;
import org.opencypher.gremlin.queries.LiteralTest;
import org.opencypher.gremlin.queries.MatchTest;
import org.opencypher.gremlin.queries.MergeTest;
import org.opencypher.gremlin.queries.NativeTraversalTest;
import org.opencypher.gremlin.queries.NullTest;
import org.opencypher.gremlin.queries.OptionalMatchTest;
import org.opencypher.gremlin.queries.OrderByTest;
import org.opencypher.gremlin.queries.ParameterTest;
import org.opencypher.gremlin.queries.PercentileTest;
import org.opencypher.gremlin.queries.ProcedureTest;
import org.opencypher.gremlin.queries.RangeTest;
import org.opencypher.gremlin.queries.ReturnTest;
import org.opencypher.gremlin.queries.SetTest;
import org.opencypher.gremlin.queries.SpecificsTest;
import org.opencypher.gremlin.queries.UnionTest;
import org.opencypher.gremlin.queries.UnwindTest;
import org.opencypher.gremlin.queries.VariableLengthPathTest;
import org.opencypher.gremlin.queries.WhereTest;
import org.opencypher.gremlin.queries.WithTest;

@RunWith(Categories.class)
@Categories.ExcludeCategory({
    SkipWithCosmosDB.class,
    UsesExtensions.class,
    UsesCollectionsInProperties.class,
})
@Suite.SuiteClasses({CaseTest.class, NativeTraversalTest.class, CastTest.class, NullTest.class, ComparisonTest.class, OptionalMatchTest.class, ComplexExamplesTest.class, OrderByTest.class, ContainerIndexTest.class, ParameterTest.class, CreateTest.class, PercentileTest.class, DeleteTest.class, ProcedureTest.class, ExplainTest.class, RangeTest.class, ExpressionTest.class, ReturnTest.class, FunctionTest.class, SetTest.class, ListComprehensionTest.class, UnionTest.class, ListSliceTest.class, UnwindTest.class, LiteralTest.class, VariableLengthPathTest.class, MatchTest.class, WhereTest.class, MergeTest.class, WithTest.class, SpecificsTest.class})
public class CosmosDBSuite {
}
