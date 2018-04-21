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
package org.opencypher.gremlin.tck.reports.tools

import java.io.{ByteArrayOutputStream, PrintStream}

import org.apache.commons.io.output.TeeOutputStream

class SystemOutReader {

  private val systemOut = System.out
  private val buffer = new ByteArrayOutputStream()
  private val teeOutput = new TeeOutputStream(System.out, buffer)

  System.setOut(new PrintStream(this.teeOutput))

  def close(): Unit = {
    System.setOut(systemOut)
  }

  def readOutput(): String = {
    val output = buffer.toString()
    clear()
    output
  }

  def clear(): Unit = {
    buffer.reset()
  }
}
