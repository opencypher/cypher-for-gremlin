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
import java.util.Collections

import org.opencypher.gremlin.extension.CypherBindingType._
import org.opencypher.gremlin.extension.CypherProcedures.procedureName
import org.opencypher.gremlin.extension._
import org.opencypher.gremlin.translation.context.WalkerContext
import org.opencypher.gremlin.translation.exception.SyntaxException
import org.opencypher.gremlin.translation.ir.TranslationWriter
import org.opencypher.gremlin.translation.ir.builder.{IRGremlinBindings, IRGremlinPredicates, IRGremlinSteps}
import org.opencypher.gremlin.translation.ir.model.GremlinStep
import org.opencypher.gremlin.translation.preparser._
import org.opencypher.gremlin.translation.translator.{Translator, TranslatorFlavor}
import org.opencypher.gremlin.translation.walker.StatementWalker
import org.opencypher.gremlin.traversal.ProcedureContext
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.frontend.phases._
import org.opencypher.v9_0.rewriting.RewriterStepSequencer
import org.opencypher.v9_0.rewriting.rewriters.Never
import org.opencypher.v9_0.util.symbols._
import org.opencypher.v9_0.util.{ASTNode, CypherException}

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * Parsed Cypher AST wrapper that can transform it in a suitable format
  * for executing a Gremlin traversal.
  *
  * @param statement       AST root node
  * @param parameters      Cypher query parameters
  * @param procedures      registered procedure context
  * @param expressionTypes expression Cypher types
  * @param returnTypes     return types by alias
  * @param options         pre-parser options provided by Cypher parser
  */
class CypherAst private (
    val statement: Statement,
    parameters: Map[String, Any],
    procedures: ProcedureContext,
    expressionTypes: Map[Expression, CypherType],
    returnTypes: Map[String, CypherType],
    options: Seq[PreParserOption]) {

  /**
    * Creates an intermediate representation of the translation.
    *
    * @param flavor translation flavor
    * @return to-Gremlin translation
    */
  def translate(flavor: TranslatorFlavor): Seq[GremlinStep] = {
    val dsl = Translator
      .builder()
      .custom(
        new IRGremlinSteps,
        new IRGremlinPredicates,
        new IRGremlinBindings
      )
      .enableCypherExtensions()
      .enableMultipleLabels()
      .build()

    val context = WalkerContext(dsl, expressionTypes, returnTypes, procedures, parameters)
    StatementWalker.walk(context, statement)
    val ir = dsl.translate()

    val rewritten = flavor.rewriters.foldLeft(ir)((ir, rewriter) => rewriter(ir))

    flavor.postConditions
      .flatMap(postCondition => postCondition(rewritten))
      .foreach(msg => throw new SyntaxException(msg))

    rewritten
  }

  /**
    * Creates a translation to Gremlin.
    *
    * @param dsl instance of [[Translator]]
    * @tparam T translation target type
    * @tparam P predicate target type
    * @return to-Gremlin translation
    */
  def buildTranslation[T, P](dsl: Translator[T, P]): T = {
    val ir = translate(dsl.flavor())
    TranslationWriter.write(ir, dsl, parameters)
  }

  private val javaOptions: util.Set[StatementOption] = options.flatMap {
    case ExplainOption => Some(StatementOption.EXPLAIN)
    case _             => None // ignore unknown
  }.toSet.asJava

  /**
    * Gets declared options for this query.
    *
    * @return set of statement options
    */
  def getOptions = new util.HashSet(javaOptions)

  /**
    * Gets types or return items
    *
    * @return map of aliases to types
    */
  def getReturnTypes = new util.HashMap[String, CypherType](returnTypes.asJava)

  /**
    * Pretty-prints the Cypher AST.
    *
    * @return string representation
    */
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

  /**
    * Constructs a new Cypher AST from the provided query.
    *
    * @param queryText Cypher query
    * @return Cypher AST wrapper
    */
  @throws[CypherException]
  def parse(queryText: String): CypherAst = {
    parse(queryText, Collections.emptyMap[String, Any](), ProcedureContext.empty())
  }

  /**
    * Constructs a new Cypher AST from the provided query.
    *
    * @param queryText  Cypher query
    * @param parameters Cypher query parameters
    * @return Cypher AST wrapper
    */
  @throws[CypherException]
  def parse(queryText: String, parameters: util.Map[String, _]): CypherAst = {
    parse(queryText, parameters, ProcedureContext.empty())
  }

  /**
    * Constructs a new Cypher AST from the provided query.
    *
    * @param queryText  Cypher query
    * @param parameters Cypher query parameters
    * @param procedures registered procedure context
    * @return Cypher AST wrapper
    */
  @throws[CypherException]
  def parse(queryText: String, parameters: util.Map[String, _], procedures: ProcedureContext): CypherAst = {
    val scalaParameters = Option(parameters)
      .map(_.asScala.toMap)
      .getOrElse(Map())
    parse(queryText, scalaParameters, procedures)
  }

  @throws[CypherException]
  private def parse(queryText: String, parameters: Map[String, Any], procedures: ProcedureContext): CypherAst = {
    val PreParsedStatement(preParsedQueryText, options, offset) = CypherPreParser(queryText)
    val startState = InitialState(preParsedQueryText, Some(offset), EmptyPlannerName)
    val state = CompilationPhases
      .parsing(RewriterStepSequencer.newPlain, literalExtraction = Never)
      .andThen(SemanticAnalysis(warn = false))
      .andThen(Normalization)
      .transform(startState, EmptyParserContext(preParsedQueryText, Some(offset)))

    val statement = state.statement()
    val expressionTypes = getExpressionTypes(state)
    val returnTypes = getReturnTypes(expressionTypes, statement, procedures)

    new CypherAst(statement, parameters, procedures, expressionTypes, returnTypes, options)
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
      statement: Statement,
      procedures: ProcedureContext): Map[String, CypherType] = {
    val clauses = statement match {
      case Query(_, part) =>
        part match {
          case union: Union =>
            union.unionedQueries.flatMap(_.clauses)
          case single: SingleQuery =>
            single.clauses
        }
    }

    val standaloneCall = clauses.forall {
      case UnresolvedCall(_, _, _, None) => true
      case _                             => false
    }

    if (standaloneCall) {
      val UnresolvedCall(Namespace(namespaceParts), ProcedureName(name), _, _) = clauses.head
      val qualifiedName = procedureName(namespaceParts, name)
      return procedures
        .findOrThrow(qualifiedName)
        .results()
        .asScala
        .map(b => (b.getName, bindingType(b.getType)))
        .toMap
    }

    clauses.flatMap {
      case Return(_, returnItems, _, _, _, _) => returnItems.items
      case _                                  => Nil
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

  private def bindingType(typ: CypherBindingType): CypherType = {
    typ match {
      case ANY          => CTAny
      case BOOLEAN      => CTBoolean
      case STRING       => CTString
      case NUMBER       => CTNumber
      case FLOAT        => CTFloat
      case INTEGER      => CTInteger
      case MAP          => CTMap
      case LIST         => CTList(CTAny)
      case NODE         => CTNode
      case RELATIONSHIP => CTRelationship
    }
  }
}
