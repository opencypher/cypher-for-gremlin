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
package org.opencypher.gremlin.translation.translator;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.opencypher.gremlin.extension.CypherProcedure;
import org.opencypher.gremlin.traversal.ProcedureContext;

/**
 * Context that is used to produce the translation.
 */
public final class TranslationContext {
    private final TranslatorFlavor flavor;
    private final ProcedureContext procedures;

    /**
     * Default translation context with default flavor and no procedures.
     */
    public static final TranslationContext DEFAULT = new TranslationContext(
        TranslatorFlavor.gremlinServer(),
        new ProcedureContext(emptySet())
    );

    public TranslationContext(TranslatorFlavor flavor,
                              ProcedureContext procedures) {
        this.flavor = flavor;
        this.procedures = procedures;
    }

    /**
     * Returns the flavor of this translation.
     *
     * @return translation flavor
     */
    public TranslatorFlavor flavor() {
        return flavor;
    }

    /**
     * Returns the registered procedure context.
     *
     * @return procedure context
     */
    public ProcedureContext procedures() {
        return procedures;
    }

    /**
     * Starts to build a translation context.
     *
     * @return translation context builder
     */
    public static TranslationContext.Builder builder() {
        return new TranslationContext.Builder();
    }

    public static final class Builder {
        private Builder() {
        }

        private final Set<CypherProcedure> procedureSet = new HashSet<>();

        /**
         * Registers a procedure in this {@link TranslationContext}.
         *
         * @return builder for translator
         */
        public Builder procedure(CypherProcedure procedure) {
            procedureSet.add(procedure);
            return this;
        }

        /**
         * Registers multiple procedures in this {@link TranslationContext}.
         *
         * @return builder for translation context
         */
        public Builder procedures(Collection<CypherProcedure> procedures) {
            procedureSet.addAll(procedures);
            return this;
        }

        /**
         * Builds a {@link TranslationContext}.
         *
         * @return translation context
         */
        public TranslationContext build() {
            return build(null);
        }

        /**
         * Builds a {@link TranslationContext} with the given translation flavor.
         *
         * @param flavor translation flavor
         * @return translation context
         */
        public TranslationContext build(TranslatorFlavor flavor) {
            return new TranslationContext(
                flavor != null ? flavor : TranslatorFlavor.gremlinServer(),
                new ProcedureContext(procedureSet)
            );
        }
    }
}
