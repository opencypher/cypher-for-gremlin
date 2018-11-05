# Cypher for Gremlin Server with TinkerGraph - Docker

Docker image for Gremlin Server 3.3.4 (TinkerGraph) with [Gremlin Server Cypher Plugin](../../tinkerpop/cypher-gremlin-server-plugin) installed.

## Usage

1. `docker run -it --rm -p 8182:8182 --name=cypher-gremlin-server cypher-for-gremlin/cypher-gremlin-server`
2. Connect using [Gremlin Console](../cypher-gremlin-console), [Cypher Client](../../tinkerpop/cypher-gremlin-server-client) or [Cypher Gremlin Neo4j Driver](../../tinkerpop/cypher-gremlin-neo4j-driver)

## Development

Docker and Make should be installed and found in `PATH`.

Command | Description
------- | -----------
`make build` | Build the image and tag as `cypher-for-gremlin/cypher-gremlin-server-tinkergraph`
`make run`   | Run the latest build image
`make sh`    | Open container's shell
