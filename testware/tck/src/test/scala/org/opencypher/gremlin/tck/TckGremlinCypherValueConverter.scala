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
package org.opencypher.gremlin.tck

import java.{lang, util}

import org.apache.tinkerpop.gremlin.driver.exception.ResponseException
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.opencypher.gremlin.translation.CypherAst
import org.opencypher.gremlin.translation.ReturnProperties._
import org.opencypher.gremlin.traversal.ProcedureContext
import org.opencypher.tools.tck.ListAccessor.unorderedList
import org.opencypher.tools.tck.api.{CypherValueRecords, ExecutionFailed}
import org.opencypher.tools.tck.constants.TCKErrorPhases.RUNTIME
import org.opencypher.tools.tck.values._

import scala.collection.JavaConverters._

object TckGremlinCypherValueConverter {

  private val UNKNOWN_ERROR = "UnknownError"

  def toExecutionFailed(e: Throwable): ExecutionFailed = {

    val gremlinRemoteException = Iterator
      .iterate(e)(_.getCause)
      .filter(c => {
        c == null |
          c.isInstanceOf[ResponseException] |
          c.isInstanceOf[RuntimeException]
      })
      .next()

    parseException(gremlinRemoteException)
  }

  def parseException(e: Throwable): ExecutionFailed = {
    val errors = GremlinErrors.mappings.filterKeys(e.getMessage.split("\\r?\\n").head.matches(_))

    errors.size match {
      case 0 if e.getCause != null => parseException(e.getCause)
      case 0                       => ExecutionFailed(UNKNOWN_ERROR, RUNTIME, e.getMessage, Some(e))
      case 1 =>
        val r = errors.head._2
        ExecutionFailed(r.errorType, r.phase, r.detail, Some(e))
      case _ =>
        ExecutionFailed(
          UNKNOWN_ERROR,
          RUNTIME,
          s"More than 1 errors `${errors.keySet}` match `${e.getMessage}`",
          Some(e)
        )
    }
  }

  def toGremlinParams(params: Map[String, CypherValue]): util.Map[String, Object] = {
    new util.HashMap[String, AnyRef](params.mapValues {
      fromCypherValue
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
      val procedures = ProcedureContext.global().getSignatures
      val ast = CypherAst.parse(query, new util.HashMap[String, Any], procedures)
      val columns = ast.statement.returnColumns
      CypherValueRecords.emptyWithHeader(columns)
    } else {
      CypherValueRecords(header, rows)
    }
  }

  def fromCypherValue: CypherValue => Any = {
    case s: CypherString =>
      s.toString
        .stripPrefix("'")
        .stripSuffix("'")
    case i: CypherInteger     => i.value
    case d: CypherFloat       => d.value
    case b: CypherBoolean     => b.value
    case m: CypherPropertyMap => toGremlinParams(m.properties)
    case l: CypherOrderedList =>
      val list = new util.ArrayList[Object]
      l.elements.foreach(e => {
        list.add(fromCypherValue(e).asInstanceOf[Object])
      })
      list
    case CypherNull => null
    case other      => throw new IllegalArgumentException(s"Unable to convert param $other")
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
    case p: util.List[_] if isListOfPaths(p)    => toCypherList(p, ordered = false)
    case p: util.List[_]                        => toCypherList(p)
    case other                                  => throw new IllegalArgumentException(s"Unable to convert result $other")
  }

  def isListOfPaths(value: util.List[_]): Boolean = value.asScala.forall(isPath)

  def toCypherPropertyMap(javaMap: util.Map[_, _]): CypherPropertyMap = {
    val map = javaMap.asScala
      .filterKeys(k => !ALL_PROPERTIES.contains(k))
      .map { case (k, v) => (k.toString, toCypherValue(v)) }
      .toMap

    CypherPropertyMap(map)
  }

  private def toCypherList(gremlinList: util.List[_], ordered: Boolean = true): CypherValue = {
    val list = gremlinList.asScala
      .map(e => toCypherValue(e))
      .toList

    if (ordered) {
      CypherOrderedList(list)
    } else {
      unorderedList(list)
    }
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
