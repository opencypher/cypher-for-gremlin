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
package org.opencypher.gremlin.translation

import java.util

import org.neo4j.cypher.internal.frontend.v3_2.CypherException
import org.neo4j.cypher.internal.frontend.v3_2.ast._
import org.neo4j.cypher.internal.frontend.v3_2.ast.rewriters.Never
import org.neo4j.cypher.internal.frontend.v3_2.helpers.rewriting.RewriterStepSequencer
import org.neo4j.cypher.internal.frontend.v3_2.phases._
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.preparser.{
  CypherPreParser,
  ExplainOption,
  PreParsedStatement,
  PreParserOption
}
import org.opencypher.gremlin.translation.translator.Translator
import org.opencypher.gremlin.translation.walker.StatementWalker

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Parsed Cypher AST wrapper that can transform it in a suitable format
  * for executing a Gremlin traversal.
  *
  * @param statement           AST root node
  * @param extractedParameters extracted parameters provided by Cypher parser
  * @param options             pre-parser options provided by Cypher parser
  */
class CypherAst(val statement: Statement, val extractedParameters: Map[String, Any], options: Seq[PreParserOption]) {

  /**
    * Create a translation by passing the wrapped AST, parameters, and options
    * to [[StatementWalker.walk]].
    *
    * @param dsl Instance of [[Translator]]
    * @return to-Gremlin translation
    */
  def buildTranslation[T, P](dsl: Translator[T, P]): T = {
    val context = StatementContext(dsl, extractedParameters)
    StatementWalker.walk(context, statement)
    context.dsl.translate()
  }

  private val javaExtractedParameters: util.Map[String, Object] =
    new util.HashMap(extractedParameters.mapValues(deepToJava).asJava)

  private def deepToJava(value: Any): Object = {
    value match {
      case null =>
        Tokens.NULL
      case seq: Seq[_] =>
        val mappedSeq = seq.map(deepToJava)
        new util.ArrayList(mappedSeq.asJava)
      case map: Map[_, _] =>
        val mappedMap = map.mapValues(deepToJava)
        new util.LinkedHashMap[Any, Any](mappedMap.asJava)
      case v =>
        v.asInstanceOf[Object]
    }
  }

  def getExtractedParameters: util.Map[String, Object] = new util.HashMap(javaExtractedParameters)

  private val javaOptions: util.Set[StatementOption] = options.flatMap {
    case ExplainOption => Some(StatementOption.EXPLAIN)
    case _             => None // ignore unknown
  }.toSet.asJava

  def getOptions: util.Set[StatementOption] = new util.HashSet(javaOptions)

  override def toString: String = {
    val acc = mutable.ArrayBuffer.empty[(String, Int)]
    flattenText(acc, statement, 0)
    acc.map {
      case (text, depth) => s"${"  " * depth}$text"
    } mkString "\n"
  }

  private def flattenText(acc: mutable.ArrayBuffer[(String, Int)], node: Any, depth: Int) {
    node match {
      case astNode: ASTNode =>
        acc += ((astNode.getClass.getSimpleName, depth))
        astNode.children.foreach(flattenText(acc, _, depth + 1))
      case n: List[_] =>
        acc.append()
        acc += (("List", depth))
        n.foreach(flattenText(acc, _, depth + 1))
      case n: Set[_] =>
        acc += (("Set", depth))
        n.foreach(flattenText(acc, _, depth + 1))
      case Some(n) =>
        acc += (("Some", depth))
        flattenText(acc, n, depth + 1)
      case n =>
        acc += ((n.toString, depth))
    }
  }
}

/**
  * Convenience object for [[CypherAst]] construction.
  * Delegates to Neo4j Cypher frontend.
  * See [[CompilationPhases.parsing]] and [[Normalization]] for a list of AST rewriters in use.
  */
object CypherAst {

  @throws[CypherException]
  def parse(queryText: String, parameters: util.Map[String, _]): CypherAst = {
    val scalaParameters = Option(parameters)
      .map(_.asScala.toMap)
      .getOrElse(Map())
    parse(queryText, scalaParameters)
  }

  @throws[CypherException]
  private def parse(queryText: String, parameters: Map[String, Any]): CypherAst = {
    val PreParsedStatement(preParsedQueryText, options, offset) = CypherPreParser(queryText)
    val startState = BaseStateImpl(preParsedQueryText, Some(offset), EmptyPlannerName)
    val state = CompilationPhases
      .parsing(RewriterStepSequencer.newPlain, Never)
      .andThen(Normalization)
      .transform(startState, EmptyParserContext(preParsedQueryText, Some(offset)))

    val params = parameters ++ state.extractedParams()

    new CypherAst(state.statement(), params, options)
  }
}
