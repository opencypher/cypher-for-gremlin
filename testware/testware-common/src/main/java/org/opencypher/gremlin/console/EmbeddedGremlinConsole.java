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
package org.opencypher.gremlin.console;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.apache.tinkerpop.gremlin.console.Console;
import org.codehaus.groovy.tools.shell.IO;

public final class EmbeddedGremlinConsole {

    private InputStream originalIn;
    private PrintWriter input;
    private Thread console;

    private EmbeddedGremlinConsole() {
    }

    private static class LazyHolder {
        private static final EmbeddedGremlinConsole INSTANCE = new EmbeddedGremlinConsole();
    }

    public static EmbeddedGremlinConsole getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void start() {
        System.setProperty("plugins", "v3d3");

        PipedInputStream in = new PipedInputStream();
        replaceSystemIn(in);
        try {
            input = new PrintWriter(new PipedOutputStream(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        console = new Thread(() -> new Console(new IO(), new ArrayList<>(), true));
        console.start();
    }

    public void stop() {
        restoreSystemIn();
        console.interrupt();
    }

    private void replaceSystemIn(InputStream in) {
        Preconditions.checkState(originalIn == null, "System.in already replaced");
        originalIn = System.in;
        System.setIn(in);
    }

    private void restoreSystemIn() {
        Preconditions.checkNotNull(originalIn, "System.in not replaced");
        System.setIn(originalIn);
        originalIn = null;
    }

    public void writeln(String s) {
        input.write(s);
        input.write('\n');
        input.flush();
    }
}
