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
package org.opencypher.gremlin.translation.ir.rewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.opencypher.gremlin.translation.ir.model._

class RewritingTest {

  @Test
  def extractOne(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val relWithLabel: String => PartialFunction[Seq[GremlinStep], String] = (stepLabel) => {
      case OutE(edgeLabel) :: As(`stepLabel`) :: InV :: _ => edgeLabel
    }
    val extracted = Rewriting.extract(seq, relWithLabel("r"))
    val notExtracted = Rewriting.extract(seq, relWithLabel("other"))

    assertThat(extracted).isEqualTo(Seq("rel"))
    assertThat(notExtracted).isEqualTo(Nil)
  }

  @Test
  def extractedMultiple(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val extracted = Rewriting.extract(seq, {
      case As(stepLabel) :: _ => stepLabel
    })

    assertThat(extracted).isEqualTo(Seq("n", "r", "m"))
  }

  @Test
  def replaceOne(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val replaced = Rewriting.replace(seq, {
      case OutE(edgeLabel) :: As(_) :: InV :: rest => OutE(edgeLabel) :: InV :: rest
    })

    assertThat(replaced).isEqualTo(
      Vertex :: As("n") :: OutE("rel") :: InV :: As("m") :: Nil
    )
  }

  @Test
  def replaceMultiple(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val replaced = Rewriting.replace(seq, {
      case As(stepLabel) :: rest => As(s"_$stepLabel") :: rest
    })

    assertThat(replaced).isEqualTo(
      Vertex :: As("_n") :: OutE("rel") :: As("_r") :: InV :: As("_m") :: Nil
    )
  }

  @Test
  def splitAfter(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val segments = Rewriting.splitAfter(seq, {
      case As(_) => true
      case _     => false
    })

    assertThat(segments).isEqualTo(
      Seq(
        Vertex :: As("n") :: Nil,
        OutE("rel") :: As("r") :: Nil,
        InV :: As("m") :: Nil
      ))
  }

}
