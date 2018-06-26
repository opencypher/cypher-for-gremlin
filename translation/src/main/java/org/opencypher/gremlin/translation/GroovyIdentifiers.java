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
package org.opencypher.gremlin.translation;

import static java.lang.Character.isJavaIdentifierPart;
import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Set;

public final class GroovyIdentifiers {
    private GroovyIdentifiers() {
    }

    private static final Set<String> GROOVY_KEYWORDS = new HashSet<>(asList(
        "as", "assert", "break", "case", "catch", "class", "const", "continue",
        "def", "default", "do", "else", "enum", "extends", "false", "finally",
        "for", "goto", "if", "implements", "import", "in", "instanceof", "interface",
        "new", "null", "package", "return", "super", "switch", "this", "throw",
        "throws", "trait", "true", "try", "while"
    ));

    public static boolean isValidIdentifier(String value) {
        char[] chars = value.toCharArray();
        int length = chars.length;
        if (length == 0) {
            return false;
        }
        if (!isJavaIdentifierStart(chars[0])) {
            return false;
        }
        for (int i = 1; i < length; i++) {
            if (!isJavaIdentifierPart(chars[i])) {
                return false;
            }
        }
        return !GROOVY_KEYWORDS.contains(value);
    }

    private static boolean isJavaIdentifierStart(char c) {
        return Character.isJavaIdentifierStart(c) && Character.getType(c) != Character.CURRENCY_SYMBOL;
    }
}
