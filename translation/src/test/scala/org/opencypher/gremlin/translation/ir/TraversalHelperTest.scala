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
package org.opencypher.gremlin.translation.ir

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.opencypher.gremlin.translation.ir.model._

class TraversalHelperTest {

  @Test
  def extractOne(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val relWithLabel: String => PartialFunction[Seq[GremlinStep], String] = stepLabel => {
      case OutE(edgeLabel) :: As(`stepLabel`) :: InV :: _ => edgeLabel
    }
    val extracted = TraversalHelper.extract(relWithLabel("r"))(seq)
    val notExtracted = TraversalHelper.extract(relWithLabel("other"))(seq)

    assertThat(extracted).isEqualTo(Seq("rel"))
    assertThat(notExtracted).isEqualTo(Nil)
  }

  @Test
  def extractMultiple(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val extracted = TraversalHelper.extract({
      case As(stepLabel) :: _ => stepLabel
    })(seq)

    assertThat(extracted).isEqualTo(Seq("n", "r", "m"))
  }

  @Test
  def extractNested(): Unit = {
    val seq = Vertex :: As("a") :: FlatMapT(As("b") :: Nil) :: Nil
    val extracted = TraversalHelper.foldTraversals(Seq.empty[String])({ (acc, steps) =>
      acc ++ TraversalHelper.extract({
        case As(stepLabel) :: _ => stepLabel
      })(steps)
    })(seq)

    assertThat(extracted).isEqualTo(Seq("a", "b"))
  }

  @Test
  def replaceOne(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val replaced = TraversalHelper.replace({
      case OutE(edgeLabel) :: As(_) :: InV :: rest => OutE(edgeLabel) :: InV :: rest
    })(seq)

    assertThat(replaced).isEqualTo(
      Vertex :: As("n") :: OutE("rel") :: InV :: As("m") :: Nil
    )
  }

  @Test
  def replaceMultiple(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val replaced = TraversalHelper.replace({
      case As(stepLabel) :: rest => As(s"_$stepLabel") :: rest
    })(seq)

    assertThat(replaced).isEqualTo(
      Vertex :: As("_n") :: OutE("rel") :: As("_r") :: InV :: As("_m") :: Nil
    )
  }

  @Test
  def replaceNested(): Unit = {
    val seq = Vertex :: Project("a", "b") :: By(Values("foo") :: Nil, None) :: By(Values("bar") :: Nil, None) :: Nil
    val replaced = TraversalHelper.mapTraversals({ steps =>
      TraversalHelper.replace({
        case Values(propertyKey) :: rest => Properties() :: HasKey(propertyKey) :: Value :: rest
      })(steps)
    })(seq)

    assertThat(replaced).isEqualTo(
      Vertex :: Project("a", "b") ::
        By(Properties() :: HasKey("foo") :: Value :: Nil, None) ::
        By(Properties() :: HasKey("bar") :: Value :: Nil, None) :: Nil
    )
  }

  @Test
  def splitAfter(): Unit = {
    val seq = Vertex :: As("n") :: OutE("rel") :: As("r") :: InV :: As("m") :: Nil
    val segments = TraversalHelper.splitAfter({
      case As(_) => true
      case _     => false
    })(seq)

    assertThat(segments).isEqualTo(
      Seq(
        Vertex :: As("n") :: Nil,
        OutE("rel") :: As("r") :: Nil,
        InV :: As("m") :: Nil
      ))
  }

}
