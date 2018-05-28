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

import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.ir.TranslationWriter
import org.opencypher.gremlin.translation.ir.builder.{IRGremlinBindings, IRGremlinPredicates, IRGremlinSteps}
import org.opencypher.gremlin.translation.preparser._
import org.opencypher.gremlin.translation.translator.Translator
import org.opencypher.gremlin.translation.walker.StatementWalker
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.frontend.phases.{BaseState, CompilationPhases, InitialState}
import org.opencypher.v9_0.rewriting.RewriterStepSequencer
import org.opencypher.v9_0.rewriting.rewriters.Never
import org.opencypher.v9_0.util.symbols.{AnyType, CypherType}
import org.opencypher.v9_0.util.{ASTNode, CypherException}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Parsed Cypher AST wrapper that can transform it in a suitable format
  * for executing a Gremlin traversal.
  *
  * @param statement           AST root node
  * @param expressionTypes     expression Cypher types
  * @param returnTypes         return types by alias
  * @param options             pre-parser options provided by Cypher parser
  */
class CypherAst(
    val statement: Statement,
    val expressionTypes: Map[Expression, CypherType],
    val returnTypes: Map[String, CypherType],
    parameters: Map[String, Any],
    options: Seq[PreParserOption]) {

  /**
    * Create a translation by passing the wrapped AST, parameters, and options
    * to [[StatementWalker.walk]].
    *
    * @param dsl instance of [[Translator]]
    * @return to-Gremlin translation
    */
  def buildTranslation[T, P](dsl: Translator[T, P]): T = {
    val irDsl = Translator
      .builder()
      .custom(
        new IRGremlinSteps,
        new IRGremlinPredicates,
        new IRGremlinBindings
      )
      .procedures(dsl.procedures().all())
      .build()

    val context = StatementContext(irDsl, expressionTypes, returnTypes, parameters)
    StatementWalker.walk(context, statement)
    val ir = irDsl.translate()

    val flavor = dsl.flavor()
    TranslationWriter
      .from(ir)
      .rewrite(flavor.rewriters: _*)
      .verify(flavor.postConditions: _*)
      .translate(dsl)
  }

  private val javaOptions: util.Set[StatementOption] = options.flatMap {
    case ExplainOption => Some(StatementOption.EXPLAIN)
    case _             => None // ignore unknown
  }.toSet.asJava

  def getOptions: util.Set[StatementOption] = new util.HashSet(javaOptions)

  def getReturnTypes: util.HashMap[String, CypherType] = new util.HashMap[String, CypherType](returnTypes.asJava)

  override def toString: String = {
    val acc = mutable.ArrayBuffer.empty[(String, Int)]
    flattenText(acc, statement, 0)
    acc.map {
      case (text, depth) => s"${"  " * depth}$text"
    } mkString "\n"
  }

  private def flattenText(acc: mutable.ArrayBuffer[(String, Int)], node: Any, depth: Int): Unit = {
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
    val startState = InitialState(preParsedQueryText, Some(offset), EmptyPlannerName)
    val state = CompilationPhases
      .parsing(RewriterStepSequencer.newPlain, Never)
      .andThen(Normalization)
      .transform(startState, EmptyParserContext(preParsedQueryText, Some(offset)))

    val params = parameters ++ state.extractedParams()
    val statement = state.statement()
    val expressionTypes = getExpressionTypes(state)
    val returnTypes = getReturnTypes(expressionTypes, statement)

    new CypherAst(statement, expressionTypes, returnTypes, params, options)
  }

  private def getExpressionTypes(state: BaseState): Map[Expression, CypherType] = {
    state.semantics().typeTable.mapValues { typeInfo =>
      val typeSpec = typeInfo.specified
      if (typeSpec.ranges.lengthCompare(1) == 0) {
        val typ = typeSpec.ranges.head.lower
        typ
      } else {
        AnyType.instance
      }
    }
  }

  private def getReturnTypes(
      expressionTypes: Map[Expression, CypherType],
      statement: Statement): Map[String, CypherType] = {
    val clauses = statement match {
      case Query(_, part) =>
        part match {
          case union: Union =>
            union.unionedQueries.flatMap(_.clauses)
          case single: SingleQuery =>
            single.clauses
        }
    }

    clauses.flatMap {
      case Return(_, returnItems, _, _, _, _, _) => returnItems.items
      case _                                     => Nil
    }.flatMap {
      case AliasedReturnItem(expression, variable @ Variable(name)) =>
        val pair = expressionTypes
          .get(expression)
          .orElse(expressionTypes.get(variable))
          .map(typ => (name, typ))
        pair
      case _ =>
        None
    }.toMap
  }
}
