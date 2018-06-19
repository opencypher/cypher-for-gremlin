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
package org.opencypher.gremlin.server.op.cypher;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static org.apache.tinkerpop.gremlin.driver.message.ResponseStatusCode.SERVER_ERROR;
import static org.opencypher.gremlin.ClientServerCommunication.CYPHER_OP_PROCESSOR_NAME;
import static org.opencypher.gremlin.translation.StatementOption.EXPLAIN;
import static org.slf4j.LoggerFactory.getLogger;

import io.netty.channel.ChannelHandlerContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.tinkerpop.gremlin.driver.Tokens;
import org.apache.tinkerpop.gremlin.driver.message.RequestMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseMessage;
import org.apache.tinkerpop.gremlin.driver.message.ResponseStatusCode;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.server.Context;
import org.apache.tinkerpop.gremlin.server.GraphManager;
import org.apache.tinkerpop.gremlin.server.OpProcessor;
import org.apache.tinkerpop.gremlin.server.op.AbstractEvalOpProcessor;
import org.apache.tinkerpop.gremlin.server.op.OpProcessorException;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.util.function.ThrowingConsumer;
import org.opencypher.gremlin.translation.CypherAst;
import org.opencypher.gremlin.translation.groovy.GroovyPredicate;
import org.opencypher.gremlin.translation.ir.TranslationWriter;
import org.opencypher.gremlin.translation.ir.model.GremlinStep;
import org.opencypher.gremlin.translation.translator.Translator;
import org.opencypher.gremlin.translation.translator.TranslatorFlavor;
import org.opencypher.gremlin.traversal.ParameterNormalizer;
import org.opencypher.gremlin.traversal.ProcedureContext;
import org.opencypher.gremlin.traversal.ReturnNormalizer;
import org.slf4j.Logger;
import scala.collection.Seq;

/**
 * {@link OpProcessor} implementation for processing Cypher {@link RequestMessage}s:
 * <pre>
 * {
 *   "requestId": "&lt;some UUID&gt;",
 *   "op": "eval",
 *   "processor": "cypher",
 *   "args": { "gremlin": "&lt;CYPHER QUERY&gt;" }
 * }
 * </pre>
 */
public class CypherOpProcessor extends AbstractEvalOpProcessor {

    private static final Logger logger = getLogger(CypherOpProcessor.class);

    public CypherOpProcessor() {
        super(true);
    }

    @Override
    public String getName() {
        return CYPHER_OP_PROCESSOR_NAME;
    }

    @Override
    public ThrowingConsumer<Context> getEvalOp() {
        return this::evalCypher;
    }

    @Override
    public Optional<ThrowingConsumer<Context>> selectOther(RequestMessage requestMessage) throws OpProcessorException {
        return empty();
    }

    private void evalCypher(Context context) throws OpProcessorException {
        Map<String, Object> args = context.getRequestMessage().getArgs();
        String cypher = (String) args.get(Tokens.ARGS_GREMLIN);
        logger.info("Cypher: {}", cypher.replaceAll("\n", " "));

        GraphTraversalSource gts = traversal(context);
        DefaultGraphTraversal g = new DefaultGraphTraversal(gts.clone());
        Map<String, Object> parameters = ParameterNormalizer.normalize(getParameters(args));
        CypherAst ast = CypherAst.parse(cypher, parameters);

        ProcedureContext procedureContext = ProcedureContext.global();
        TranslatorFlavor flavor = TranslatorFlavor.gremlinServer();
        Seq<GremlinStep> ir = ast.translate(flavor, procedureContext);

        Translator<String, GroovyPredicate> stringTranslator = Translator.builder()
            .gremlinGroovy()
            .inlineParameters()
            .build();

        String gremlin = TranslationWriter.write(ir, stringTranslator, parameters);
        logger.info("Gremlin: {}", gremlin);

        if (ast.getOptions().contains(EXPLAIN)) {
            explainQuery(context, ast, gremlin);
            return;
        }

        Translator<GraphTraversal, P> traversalTranslator = Translator.builder()
            .traversal(g)
            .build();

        GraphTraversal<?, ?> traversal = TranslationWriter.write(ir, traversalTranslator, parameters);
        ReturnNormalizer returnNormalizer = ReturnNormalizer.create(ast.getReturnTypes());
        Traversal<?, Map<String, Object>> normalizedTraversal = traversal.map(returnNormalizer::normalize);
        inTransaction(gts, () -> handleIterator(context, normalizedTraversal));
    }

    private void inTransaction(GraphTraversalSource gts, Runnable runnable) {
        Graph graph = gts.getGraph();
        boolean supportsTransactions = graph.features().graph().supportsTransactions();

        if (!supportsTransactions) {
            runnable.run();
            return;
        }

        try {
            graph.tx().open();
            runnable.run();
            graph.tx().commit();
        } catch (Exception e) {
            if (graph.tx().isOpen()) {
                graph.tx().rollback();
            }
        }
    }

    private GraphTraversalSource traversal(Context context) throws OpProcessorException {
        RequestMessage msg = context.getRequestMessage();
        GraphManager graphManager = context.getGraphManager();

        Optional<Map<String, String>> aliasesOptional = msg.optionalArgs(Tokens.ARGS_ALIASES);
        String gAlias = aliasesOptional
            .map(aliases -> aliases.get(Tokens.VAL_TRAVERSAL_SOURCE_ALIAS))
            .orElse(null);

        if (gAlias == null) {
            return graphManager.getGraphNames().stream()
                .findFirst()
                .map(graphManager::getGraph)
                .map(Graph::traversal)
                .orElseThrow(() -> opProcessorException(msg, "No graphs found on the server"));
        }

        Graph graph = graphManager.getGraph(gAlias);
        if (graph != null) {
            return graph.traversal();
        }

        TraversalSource traversalSource = graphManager.getTraversalSource(gAlias);
        if (traversalSource instanceof GraphTraversalSource) {
            return (GraphTraversalSource) traversalSource;
        }

        throw opProcessorException(msg, "Traversable alias '" + gAlias + "' not found");
    }

    private OpProcessorException opProcessorException(RequestMessage msg, String errorMessage) {
        return new OpProcessorException(errorMessage,
            ResponseMessage.build(msg)
                .code(SERVER_ERROR)
                .statusMessage(errorMessage).create());
    }

    private void handleIterator(Context context, Traversal traversal) {
        try {
            super.handleIterator(context, traversal);
        } catch (Exception ex) {
            logger.error("Error during traversal iteration", ex);
            ChannelHandlerContext ctx = context.getChannelHandlerContext();
            ctx.writeAndFlush(ResponseMessage.build(context.getRequestMessage())
                .code(SERVER_ERROR)
                .statusMessage(ex.getMessage())
                .statusAttributeException(ex)
                .create());
        }
    }

    private void explainQuery(Context context, CypherAst ast, String gremlin) {
        Map<String, Object> explanation = new LinkedHashMap<>();
        explanation.put("translation", gremlin);
        explanation.put("options", ast.getOptions().toString());

        ResponseMessage explainMsg = ResponseMessage.build(context.getRequestMessage())
            .code(ResponseStatusCode.SUCCESS)
            .statusMessage("OK")
            .result(singletonList(explanation))
            .create();

        ChannelHandlerContext ctx = context.getChannelHandlerContext();
        ctx.writeAndFlush(explainMsg);
    }

    @Override
    public void close() {
        // do nothing = no resources to release
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getParameters(Map<String, Object> args) {
        if (args.containsKey(Tokens.ARGS_BINDINGS)) {
            return (Map<String, Object>) args.get(Tokens.ARGS_BINDINGS);
        } else {
            return new HashMap<>();
        }
    }
}
