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
package org.opencypher.gremlin.tck

import java.{lang, util}

import org.apache.tinkerpop.gremlin.driver.exception.ResponseException
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.opencypher.gremlin.translation.CypherAst
import org.opencypher.gremlin.translation.ReturnProperties._
import org.opencypher.tools.tck.api.{CypherValueRecords, ExecutionFailed}
import org.opencypher.tools.tck.values._

import scala.collection.JavaConverters._

object TCKGremlinCypherValueConverter {

  def toExecutionFailed(e: Throwable): ExecutionFailed = {
    val gremlinRemoteException = Iterator
      .iterate(e)(_.getCause)
      .filter(c => c == null || c.isInstanceOf[ResponseException])
      .next()

    gremlinRemoteException match {
      case r: ResponseException =>
        val errors = GremlinErrors.mappings.filterKeys(k => r.getMessage matches k)

        errors.size match {
          case 0 => throw new RuntimeException(s"Unknown error message `${r.getMessage}`", e)
          case 1 => errors.head._2
          case _ => throw new RuntimeException(s"More than 1 errors `${errors.keySet}` match `${r.getMessage}`", e)
        }
      case _ =>
        throw new RuntimeException(
          "Unable to find org.apache.tinkerpop.gremlin.driver.exception.ResponseException in stack ",
          e)
    }
  }

  def toGremlinParams(params: Map[String, CypherValue]): util.Map[String, Object] = {
    new util.HashMap[String, AnyRef](params.mapValues {
      case s: CypherString =>
        s.toString
          .stripPrefix("'")
          .stripSuffix("'")
      case i: CypherInteger => i.value
      case d: CypherFloat   => d.value
      case b: CypherBoolean => b.value
      case n                => throw new IllegalArgumentException(s"Unable to convert param $n")
    }.mapValues(_.asInstanceOf[Object]).asJava)
  }

  def toCypherValueRecords(query: String, results: util.List[util.Map[String, AnyRef]]): CypherValueRecords = {
    val rows = results.asScala
      .map(
        javaMap => javaMap.asScala.mapValues((v) => toCypherValue(v)).toMap
      )
      .toList

    val header =
      if (results.isEmpty) List.empty[String]
      else results.get(0).keySet().asScala.toList

    emptyHeaderWorkaround(query, header, rows)
  }

  private def emptyHeaderWorkaround(query: String, header: List[String], rows: List[Map[String, CypherValue]]) = {
    if (rows.isEmpty) {
      val columns = CypherAst.parse(query, new util.HashMap[String, Any]).statement.returnColumns
      CypherValueRecords.emptyWithHeader(columns)
    } else {
      CypherValueRecords(header, rows)
    }
  }

  def toCypherValue(v: Any): CypherValue = v match {
    case s: String                              => CypherString(s)
    case d: lang.Double                         => CypherFloat(d)
    case n: Number                              => CypherInteger(n.longValue())
    case b: lang.Boolean                        => CypherBoolean(b)
    case null                                   => CypherNull
    case n: util.Map[_, _] if isNode(n)         => toCypherNode(n)
    case r: util.Map[_, _] if isRelationship(r) => toCypherRelationship(r)
    case m: util.Map[_, _]                      => toCypherPropertyMap(m)
    case p: util.List[_] if isPath(v)           => toCypherPath(p.asInstanceOf[util.List[util.Map[_, _]]])
    case p: util.List[_]                        => toCypherList(p)
  }

  def toCypherPropertyMap(javaMap: util.Map[_, _]): CypherPropertyMap = {
    val map = javaMap.asScala
      .filterKeys(k => !ALL_PROPERTIES.contains(k))
      .map { case (k, v) => (k.toString, toCypherValue(v)) }
      .toMap

    CypherPropertyMap(map)
  }

  private def toCypherList(gremlinList: util.List[_]): CypherValue = {
    val list = gremlinList.asScala
      .map(e => toCypherValue(e))
      .toList

    CypherOrderedList(list)
  }

  def toCypherRelationship(e: util.Map[_, _]): CypherRelationship = {
    val properties = toCypherPropertyMap(e)
    val label = String.valueOf(e.get(LABEL))
    CypherRelationship(label, properties)
  }

  def toCypherNode(v: util.Map[_, _]): CypherNode = {
    val labels = new util.HashSet[String]
    val label = String.valueOf(v.get(LABEL))
    if (!(Vertex.DEFAULT_LABEL == label)) labels.add(label)
    val properties = toCypherPropertyMap(v)
    CypherNode(labels.asScala.toSet, properties)
  }

  private def toCypherPath(gremlinPath: util.List[util.Map[_, _]]) = {
    val gremlinPathScala = gremlinPath.asScala

    val startingNode = toCypherNode(gremlinPathScala.head)

    val vertexes = gremlinPathScala
      .filter(e => isNode(e))
      .map(_.asInstanceOf[util.Map[String, Object]])
      .map(v => (v.get(ID), v))
      .toMap[Object, util.Map[String, Object]]

    val relationships = gremlinPathScala
      .grouped(2)
      .filter(it => isNode(it.head) && isRelationship(it.last))
      .map(
        it => {
          val v = it.head.asInstanceOf[util.Map[String, Object]]
          val e = it.last.asInstanceOf[util.Map[String, Object]]

          if (e.get(OUTV).equals(v.get(ID))) {
            Forward(toCypherRelationship(e), toCypherNode(vertexes(e.get(INV))))
          } else {
            Backward(toCypherRelationship(e), toCypherNode(vertexes(e.get(OUTV))))
          }
        }
      )
      .toList

    CypherPath(startingNode, relationships)
  }
}
