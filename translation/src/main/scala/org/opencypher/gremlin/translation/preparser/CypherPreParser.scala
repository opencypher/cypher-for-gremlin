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
package org.opencypher.gremlin.translation.preparser

import org.neo4j.cypher.internal.frontend.v3_2.parser.Base
import org.parboiled.scala.{Parser, Rule1}

object CypherPreParser extends Parser with Base {
  def apply(input: String): PreParsedStatement = parseOrThrow(input, None, QueryWithOptions)

  def QueryWithOptions: Rule1[Seq[PreParsedStatement]] =
    WS ~ AllSUpportedOptions ~ WS ~ AnySomething ~~>>
      ((options: Seq[PreParserOption], text: String) => pos => Seq(PreParsedStatement(text, options, pos)))

  def AllSUpportedOptions: Rule1[Seq[PreParserOption]] = zeroOrMore(Explain, WS)

  def AnySomething: Rule1[String] = rule("Query") {
    oneOrMore(org.parboiled.scala.ANY) ~> identity
  }

  def Explain: Rule1[PreParserOption] = keyword("EXPLAIN") ~ push(ExplainOption)
}
