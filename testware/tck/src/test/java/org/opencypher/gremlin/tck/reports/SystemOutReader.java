/*
 * Copyright (c) 2018-2019 "Neo4j, Inc." [https://neo4j.com]
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
package org.opencypher.gremlin.tck.reports;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

class SystemOutReader {
    private PrintStream systemOut = System.out;
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private TeeOutputStream teeOutput = new TeeOutputStream(System.out, buffer);

    SystemOutReader() {
        System.setOut(new PrintStream(teeOutput));
    }

    void close() throws IOException {
        System.setOut(systemOut);
        buffer.close();
    }

    String clear() {
        String output = buffer.toString();
        buffer.reset();
        return output;
    }

    static class TeeOutputStream extends OutputStream {
        private OutputStream out1;
        private OutputStream out2;

        TeeOutputStream(final OutputStream out1, final OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(final byte[] b) throws IOException {
            this.out1.write(b);
            this.out2.write(b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            this.out1.write(b, off, len);
            this.out2.write(b, off, len);
        }

        @Override
        public void write(final int b) throws IOException {
            this.out1.write(b);
            this.out2.write(b);
        }

        @Override
        public void flush() throws IOException {
            this.out1.flush();
            this.out2.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                this.out1.close();
            } finally {
                this.out2.close();
            }
        }

    }

}
