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
package org.opencypher.gremlin.translation.ir

import fastparse.JavaWhitespace._
import fastparse._
import org.apache.tinkerpop.gremlin.process.traversal.{Order, Scope}
import org.apache.tinkerpop.gremlin.structure.Column
import org.apache.tinkerpop.gremlin.structure.VertexProperty.Cardinality
import org.opencypher.gremlin.translation.ir.model._

object GremlinParser {
  def parse(gremlin: String): Seq[GremlinStep] = {
    fastparse.parse(gremlin, Parser.query(_)) match {
      case Parsed.Success(result, _) => result
      case f: Parsed.Failure         => throw new RuntimeException(f.msg)
    }
  }
}

private object Parser {
  def fail[_: P](e: Any) = throw new IllegalArgumentException("Unable to parse " + e)

  def string[_: P]: P[String] = P("'" ~ CharsWhile(_ != '\'', min = 0).! ~ "'")
  def `null`[_: P]: P[Any] = P(StringIn("null")).map(_ => null)
  def bool[_: P]: P[Boolean] = P(StringIn("true", "false").!).map(_.toBoolean)
  def number[_: P]: P[Long] = P(CharIn("0-9").!).map(_.toLong)
  def any[_: P]: P[Any] = P(string | bool | `null` | number)

  def ord[_: P]: P[Order] = P(StringIn("incr", "decr", "shuffle").!).map(Order.valueOf)
  def column[_: P]: P[Column] = P(StringIn("keys", "values").!).map(Column.valueOf)
  def scope[_: P]: P[Scope] = P(StringIn("global", "local").!).map(Scope.valueOf)
  def cardinality[_: P]: P[Cardinality] = P(StringIn("single", "list", "set").!).map(Cardinality.valueOf)

  def isEq[_: P]: P[GremlinPredicate] = P("eq(" ~/ any ~ ")").map(Eq)
  def gt[_: P]: P[GremlinPredicate] = P("gt(" ~/ number ~ ")").map(Gt)
  def gte[_: P]: P[GremlinPredicate] = P("gte(" ~/ number ~ ")").map(Gte)
  def lt[_: P]: P[GremlinPredicate] = P("lt(" ~/ number ~ ")").map(Lt)
  def lte[_: P]: P[GremlinPredicate] = P("lte(" ~/ number ~ ")").map(Lte)
  def neq[_: P]: P[GremlinPredicate] = P("neq(" ~/ any ~ ")").map(Neq)
  def between[_: P]: P[GremlinPredicate] =
    P("between(" ~/ number ~ "," ~ number ~ ")").map(n => Between(n._1, n._2))
  def within[_: P]: P[GremlinPredicate] = P("within(" ~/ any.rep(sep = ",") ~ ")").map(n => Within(n: _*))
  def without[_: P]: P[GremlinPredicate] = P("without(" ~/ any.rep(sep = ",") ~ ")").map(n => Without(n: _*))
  def predicate[_: P]: P[GremlinPredicate] = P(isEq | gt | gte | lt | lte | neq | between | within | without)

  def V[_: P]: P[GremlinStep] = P("V()").map(_ => Vertex)
  def E[_: P]: P[GremlinStep] = P("E()").map(_ => Edge)
  def addE[_: P]: P[GremlinStep] = P("addE(" ~ string ~ ")").map(AddE)
  def addV[_: P]: P[GremlinStep] = P("addV(" ~ string.? ~ ")").map {
    case Some(s) => AddV(s)
    case None    => AddV
  }
  def aggregate[_: P]: P[GremlinStep] = P("aggregate(" ~ string ~ ")").map(Aggregate)
  def and[_: P]: P[GremlinStep] = P("and(" ~/ traversal.rep(sep = ",") ~ ")").map(n => And(n: _*))
  def as[_: P]: P[GremlinStep] = P("as(" ~ string ~ ")").map(As)
  def barrier[_: P]: P[GremlinStep] = P("barrier()").map(_ => Barrier)
  def bothE[_: P]: P[GremlinStep] = P("bothE(" ~ string.rep(sep = ",") ~ ")").map(n => BothE(n: _*))
  def by[_: P]: P[GremlinStep] = P("by(" ~/ traversal ~ ",".? ~ ord.? ~ ")").map(n => By(n._1, n._2))
  def cap[_: P]: P[GremlinStep] = P("cap(" ~/ string ~ ")").map(Cap)
  def choose[_: P]: P[GremlinStep] =
    P("choose(" ~/ traversal.? ~ predicate.? ~ ",".? ~ traversal.rep(min = 0, max = 2, sep = ",") ~ ")").map {
      case (Some(t), None, opts) if opts.size == 0 => ChooseT1(t)
      case (Some(t), None, opts) if opts.size == 1 => ChooseT2(t, opts.head)
      case (Some(t), None, opts) if opts.size == 2 => ChooseT3(t, opts.head, opts.last)
      case (None, Some(p), opts) if opts.size == 1 => ChooseP2(p, opts.head)
      case (None, Some(p), opts) if opts.size == 2 => ChooseP3(p, opts.head, opts.last)
      case c                                       => fail(c)
    }
  def coalesce[_: P]: P[GremlinStep] =
    P("coalesce(" ~/ traversal.rep(sep = ",") ~ ")").map(n => Coalesce(n: _*))
  def constant[_: P]: P[GremlinStep] = P("constant(" ~ any ~ ")").map(Constant)
  def count[_: P]: P[GremlinStep] = P("count(" ~ scope.? ~ ")").map {
    case Some(scope) => CountS(scope)
    case None        => Count
  }
  def dedup[_: P]: P[GremlinStep] = P("dedup(" ~ string.rep(sep = ",") ~ ")").map(n => Dedup(n: _*))
  def drop[_: P]: P[GremlinStep] = P("drop()").map(_ => Drop)
  def emit[_: P]: P[GremlinStep] = P("emit(" ~ traversal.? ~ ")").map {
    case Some(t) => EmitT(t)
    case None    => Emit
  }
  def flatMap[_: P]: P[GremlinStep] = P("flatMap(" ~/ traversal ~ ")").map(FlatMapT)
  def fold[_: P]: P[GremlinStep] = P("fold()").map(_ => Fold)
  def from[_: P]: P[GremlinStep] = P("from(" ~ string ~ ")").map(From)
  def group[_: P]: P[GremlinStep] = P("group()").map(_ => Group)
  def has[_: P]: P[GremlinStep] = P("has(" ~ string ~ ",".? ~ predicate.? ~ ")").map {
    case (s, Some(p)) => HasP(s, p)
    case (s, None)    => Has(s)
  }
  def hasKey[_: P]: P[GremlinStep] = P("hasKey(" ~ string.rep(sep = ",") ~ ")").map(n => HasKey(n: _*))
  def hasLabel[_: P]: P[GremlinStep] = P("hasLabel(" ~ string.rep(sep = ",") ~ ")").map(n => HasLabel(n: _*))
  def hasNot[_: P]: P[GremlinStep] = P("hasNot(" ~ string ~ ")").map(HasNot)
  def id[_: P]: P[GremlinStep] = P("id()").map(_ => Id)
  def identity[_: P]: P[GremlinStep] = P("identity()").map(_ => Identity)
  def inE[_: P]: P[GremlinStep] = P("inE(" ~ string.rep(sep = ",") ~ ")").map(n => InE(n: _*))
  def inV[_: P]: P[GremlinStep] = P("inV()").map(_ => InV)
  def inject[_: P]: P[GremlinStep] = P("inject(" ~ any.rep(sep = ",") ~ ")").map(n => Inject(n: _*))
  def is[_: P]: P[GremlinStep] = P("is(" ~/ predicate ~ ")").map(Is)
  def key[_: P]: P[GremlinStep] = P("key()").map(_ => Key)
  def label[_: P]: P[GremlinStep] = P("label()").map(_ => Label)
  def limit[_: P]: P[GremlinStep] = P("limit(" ~ scope.? ~ ",".? ~ number ~ ")").map {
    case (Some(scope), n) => LimitS(scope, n)
    case (None, n)        => Limit(n)
  }
  def local[_: P]: P[GremlinStep] = P("local(" ~/ traversal ~ ")").map(Local)
  def loops[_: P]: P[GremlinStep] = P("loops()").map(_ => Loops)
  def map[_: P]: P[GremlinStep] = P("map(" ~/ traversal ~ ")").map(MapT)
  def math[_: P]: P[GremlinStep] = P("math(" ~ string ~ ")").map(Math)
  def max[_: P]: P[GremlinStep] = P("max()").map(_ => Max)
  def mean[_: P]: P[GremlinStep] = P("mean()").map(_ => Mean)
  def min[_: P]: P[GremlinStep] = P("min()").map(_ => Min)
  def not[_: P]: P[GremlinStep] = P("not(" ~/ traversal ~ ")").map(Not)
  def option[_: P]: P[GremlinStep] =
    P("option(" ~ any ~ "," ~ traversal ~ ")").map(n => OptionT(n._1.asInstanceOf[Object], n._2))
  def optional[_: P]: P[GremlinStep] = P("optional(" ~/ traversal ~ ")").map(model.Optional)
  def or[_: P]: P[GremlinStep] = P("or(" ~/ traversal.rep(sep = ",") ~ ")").map(n => Or(n: _*))
  def order[_: P]: P[GremlinStep] = P("order()").map(_ => model.Order)
  def otherV[_: P]: P[GremlinStep] = P("otherV()").map(_ => OtherV)
  def outE[_: P]: P[GremlinStep] = P("outE(" ~ string.rep(sep = ",") ~ ")").map(n => OutE(n: _*))
  def outV[_: P]: P[GremlinStep] = P("outV()").map(_ => OutV)
  def path[_: P]: P[GremlinStep] = P("path()").map(_ => Path)
  def properties[_: P]: P[GremlinStep] =
    P("properties(" ~ string.rep(sep = ",") ~ ")").map(n => Properties(n: _*))
  def property[_: P]: P[GremlinStep] =
    P("property(" ~ cardinality.? ~ ",".? ~ string ~ "," ~ any.? ~ traversal.? ~ ")").map {
      case (None, k, Some(v), None)    => PropertyV(k, v)
      case (Some(c), k, Some(v), None) => PropertyVC(c, k, v)
      case (None, k, None, Some(t))    => PropertyT(k, t)
      case (Some(c), k, None, Some(t)) => PropertyTC(c, k, t)
      case e                           => fail(e)
    }
  def project[_: P]: P[GremlinStep] = P("project(" ~ string.rep(sep = ",") ~ ")").map(n => Project(n: _*))
  def range[_: P]: P[GremlinStep] =
    P("range(" ~ scope ~ "," ~ number ~ "," ~ number ~ ")").map(n => Range(n._1, n._2, n._3))
  def repeat[_: P]: P[GremlinStep] = P("repeat(" ~/ traversal ~ ")").map(Repeat)
  def select[_: P]: P[GremlinStep] = P("select(" ~ column.? ~ string.rep(sep = ",", min = 1).? ~ ")").map {
    case (Some(column: Column), None) => SelectC(column)
    case (None, Some(keys))           => SelectK(keys: _*)
    case e                            => fail(e)
  }
  def sideEffect[_: P]: P[GremlinStep] = P("sideEffect(" ~/ traversal ~ ")").map(SideEffect)
  def simplePath[_: P]: P[GremlinStep] = P("simplePath()").map(_ => SimplePath)
  def skip[_: P]: P[GremlinStep] = P("skip(" ~ number ~ ")").map(Skip)
  def sum[_: P]: P[GremlinStep] = P("sum()").map(_ => Sum)
  def tail[_: P]: P[GremlinStep] = P("tail(" ~ scope ~ ",".? ~ number ~ ")").map(n => Tail(n._1, n._2))
  def times[_: P]: P[GremlinStep] = P("times(" ~ number ~ ")").map(n => Times(n.intValue()))
  def to[_: P]: P[GremlinStep] = P("to(" ~ string ~ ")").map(To)
  def unfold[_: P]: P[GremlinStep] = P("unfold()").map(_ => Unfold)
  def union[_: P]: P[GremlinStep] = P("union(" ~/ traversal.rep(sep = ",") ~ ")").map(n => Union(n: _*))
  def until[_: P]: P[GremlinStep] = P("until(" ~/ traversal ~ ")").map(Until)
  def value[_: P]: P[GremlinStep] = P("value()").map(_ => Value)
  def valueMap[_: P]: P[GremlinStep] = P("valueMap(" ~ bool.? ~ ")").map {
    case Some(b) => ValueMap(b)
    case None    => ValueMap
  }
  def values[_: P]: P[GremlinStep] = P("values(" ~/ string.rep(sep = ",") ~ ")").map(n => Values(n: _*))
  def where[_: P]: P[GremlinStep] = P("where(" ~ traversal.? ~ predicate.? ~ ")").map {
    case (Some(t), None) => WhereT(t)
    case (None, Some(p)) => WhereP(p)
    case e               => fail(e)
  }
  def step[_: P]: P[GremlinStep] =
    P(V | E | addE | addV | aggregate | and | as | barrier | bothE | by | cap | choose | coalesce | constant | count | dedup | drop | emit | flatMap | fold | from | group | has | hasKey | hasLabel | hasNot | id | identity | inE | inV | inject | is | key | label | limit | local | loops | map | math | max | mean | min | not | option | optional | or | order | otherV | outE | outV | path | properties | property | project | range | repeat | select | sideEffect | simplePath | skip | sum | tail | times | to | unfold | union | until | value | valueMap | values | where)

  def traversal[_: P]: P[Seq[GremlinStep]] = P("__.".? ~ step.rep(sep = ".", min = 1))
  def query[_: P]: P[Seq[GremlinStep]] = P("g.".? ~ traversal ~ End)
}
