// an init script that returns a Map allows explicit setting of global bindings.
def globals = [:]

// Generates the modern graph into an "empty" TinkerGraph via LifeCycleHook.
// Note that the name of the key in the "global" map is unimportant.
globals << [hook: [
    onStartUp: { ctx ->
        ctx.logger.info("Loading 'modern' graph data.")

        org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory.generateModern(graph)
    }
] as LifeCycleHook]

// define the default TraversalSource to bind queries to - this one will be named "g".
globals << [g: graph.traversal()]
