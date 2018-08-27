import org.apache.tinkerpop.gremlin.server.util.LifeCycleHook

// an init script that returns a Map allows explicit setting of global bindings.
def globals = [:]

// Generates the modern graph and the crew graph into an "empty" TinkerGraph via LifeCycleHook.
globals << [hook: [
    onStartUp: { ctx ->
        ctx.logger.info("Loading graph data.")

        org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory.generateModern(graph)
        org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory.generateTheCrew(graph2)
    }
] as LifeCycleHook]

// define the TraversalSources to bind queries to - these will be named "g" and "g2".
globals << [g: graph.traversal()]
globals << [g2: graph2.traversal()]
