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
package org.opencypher.gremlin.translation.walker

import org.opencypher.gremlin.translation.GremlinSteps
import org.opencypher.gremlin.translation.Tokens.START
import org.opencypher.gremlin.translation.context.StatementContext
import org.opencypher.gremlin.translation.walker.NodeUtils._
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._
import org.opencypher.v9_0.util.InputPosition
import org.opencypher.v9_0.util.symbols.AnyType

import scala.collection.JavaConverters._

object CallWalker {
  def walk[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: UnresolvedCall): Unit = {
    new CallWalker(context, g).walk(node)
  }

  def walkStandalone[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P], node: UnresolvedCall): Unit = {
    new CallWalker(context, g).walkStandalone(node)
  }
}

private class CallWalker[T, P](context: StatementContext[T, P], g: GremlinSteps[T, P]) {

  def walk(node: UnresolvedCall): Unit = {
    if (context.isFirstStatement) {
      context.markFirstStatement()
      g.inject(START)
    }

    node match {
      case UnresolvedCall(Namespace(namespaceParts), ProcedureName(name), argumentOption, results) =>
        val procedures = context.dsl.procedures()
        val qualifiedName = namespaceParts.mkString(".") + "." + name
        procedures.findOrThrow(qualifiedName)

        val arguments = argumentOption.getOrElse {
          throw new IllegalArgumentException(s"In-query call with implicit arguments: $qualifiedName")
        }
        val resultsMapName = context.generateName()

        g.map(asList(arguments, context))

        val callG = g
          .start()
          .map(procedures.procedureCall(qualifiedName))
          .unfold()
          .as(resultsMapName)

        val keyAliases = resultsAsPairs(results)
        if (keyAliases.isEmpty) {
          g.optional(callG)
        } else {
          g.map(callG)
        }
        keyAliases.foreach {
          case (key, alias) =>
            g.select(resultsMapName).select(key).as(alias)
            context.alias(alias)
        }
    }
  }

  def walkStandalone(node: UnresolvedCall): Unit = {
    if (context.isFirstStatement) {
      context.markFirstStatement()
      g.inject(START)
    }

    val procedures = context.dsl.procedures()
    node match {
      case UnresolvedCall(Namespace(namespaceParts), ProcedureName(name), argumentOption, results) =>
        val qualifiedName = namespaceParts.mkString(".") + "." + name
        val procedure = procedures.findOrThrow(qualifiedName)

        val arguments = argumentOption.getOrElse {
          val argumentNames = procedure.arguments().asScala.map(_.getName)
          argumentNames.foreach { argumentName =>
            if (!context.parameterDefined(argumentName)) {
              throw new IllegalArgumentException(s"Parameter $argumentName missing for procedure $qualifiedName")
            }
          }
          argumentNames.map(Parameter(_, AnyType.instance)(InputPosition.NONE))
        }

        g.map(asList(arguments, context))
          .map(procedures.procedureCall(qualifiedName))
          .unfold()

        val keyAliases = resultsAsPairs(results)
        if (results.nonEmpty) {
          val keys = keyAliases.map(_._1)
          val aliases = keyAliases.map(_._2)

          g.project(aliases: _*)
          keys.foreach(key => g.by(g.start().select(key)))
        }
    }
  }

  private def resultsAsPairs(results: Option[ProcedureResult]): Seq[(String, String)] = {
    results.map {
      case ProcedureResult(items, _) =>
        items.map {
          case ProcedureResultItem(Some(ProcedureOutput(result)), Variable(alias)) =>
            (result, alias)
          case ProcedureResultItem(None, Variable(result)) =>
            (result, result)
        }
    }.getOrElse(Seq())
  }
}
