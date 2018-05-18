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
import org.opencypher.gremlin.traversal.ProcedureRegistry.procedureCall
import org.opencypher.v9_0.ast._
import org.opencypher.v9_0.expressions._

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
        val qualifiedName = namespaceParts.mkString(".") + "." + name
        val arguments = argumentOption.getOrElse(Nil)
        val resultsMapName = context.generateName()

        g.map(asList(arguments, context))
          .map(procedureCall(qualifiedName))
          .unfold()
          .as(resultsMapName)

        keyAliases(results).foreach {
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

    node match {
      case UnresolvedCall(Namespace(namespaceParts), ProcedureName(name), argumentOption, results) =>
        val qualifiedName = namespaceParts.mkString(".") + "." + name
        val arguments = argumentOption.getOrElse(Nil)

        g.map(asList(arguments, context))
          .map(procedureCall(qualifiedName))
          .unfold()

        val keyAliasMap = keyAliases(results)
        if (results.nonEmpty) {
          val keys = keyAliasMap.map(_._1)
          val aliases = keyAliasMap.map(_._2)

          g.project(aliases: _*)
          keys.foreach(key => g.by(g.start().select(key)))
        }
    }
  }

  private def keyAliases(results: Option[ProcedureResult]) = {
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
