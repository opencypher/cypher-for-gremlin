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
import org.apache.tinkerpop.gremlin.structure.{Edge, Property, Vertex}
import org.opencypher.gremlin.translation.CypherAst
import org.opencypher.tools.tck.api.{CypherValueRecords, ExecutionFailed}
import org.opencypher.tools.tck.values._

import scala.collection.JavaConverters._

object GremlinCypherValueConverter {

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

  private def toCypherValue(v: Any): CypherValue = v match {
    case v: Vertex         => toCypherNode(v)
    case e: Edge           => toCypherRelationship(e)
    case s: String         => CypherString(s)
    case d: lang.Double    => CypherFloat(d)
    case n: Number         => CypherInteger(n.longValue())
    case b: lang.Boolean   => CypherBoolean(b)
    case null              => CypherNull
    case m: util.Map[_, _] => toCypherPropertyMap(m)
    case p: util.List[_]   => toCypherList(p)
  }

  private def toCypherRelationship(e: Edge) = {
    CypherRelationship(e.label(), toCypherPropertyMap(e.properties()))
  }

  private def toCypherNode(v: Vertex) = {
    val labels = if (Vertex.DEFAULT_LABEL.equals(v.label())) Set.empty[String] else Set(v.label())
    CypherNode(labels, toCypherPropertyMap(v.properties()))
  }

  private def toCypherPropertyMap[A <: Property[B], B](iterator: util.Iterator[A]): CypherPropertyMap = {
    val map = iterator.asScala
      .map(v => (v.key(), toCypherValue(v.value())))
      .toMap

    CypherPropertyMap(map)
  }

  private def toCypherPropertyMap(javaMap: util.Map[_, _]): CypherPropertyMap = {
    val map = javaMap.asScala.map { case (k, v) => (k.toString, toCypherValue(v)) }.toMap

    CypherPropertyMap(map)
  }

  private def toCypherList(gremlinList: util.List[_]): CypherValue = {
    toCypherPath(gremlinList) match {
      case Some(path) =>
        path
      case None =>
        val list = gremlinList.asScala
          .map(e => toCypherValue(e))
          .toList

        CypherOrderedList(list)
    }
  }

  private def toCypherPath(gremlinPath: util.List[_]): Option[CypherPath] = {
    val gremlinPathScala = gremlinPath.asScala

    if (gremlinPathScala.nonEmpty && gremlinPathScala.head.isInstanceOf[Vertex]) {
      val startingNode = toCypherNode(gremlinPathScala.head.asInstanceOf[Vertex])
      val vertexes = gremlinPathScala
        .filter(_.isInstanceOf[Vertex])
        .map(_.asInstanceOf[Vertex])
        .map(v => (v.id(), v))
        .toMap[Object, Vertex]

      val relationships = gremlinPathScala
        .grouped(2)
        .filter(it => it.head.isInstanceOf[Vertex] && it.last.isInstanceOf[Edge])
        .map(
          it => {
            val v = it.head.asInstanceOf[Vertex]
            val e = it.last.asInstanceOf[Edge]

            if (e.outVertex().equals(v)) {
              Forward(toCypherRelationship(e), toCypherNode(vertexes(e.inVertex().id())))
            } else {
              Backward(toCypherRelationship(e), toCypherNode(vertexes(e.outVertex().id())))
            }
          }
        )
        .toList

      if (vertexes.nonEmpty) Some(CypherPath(startingNode, relationships)) else None
    } else {
      None
    }
  }
}
