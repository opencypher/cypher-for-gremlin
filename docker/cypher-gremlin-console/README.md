# Cypher for Gremlin Console - Docker

Docker image for Gremlin Console 3.4.0 with [Gremlin Console Cypher plugin](../../tinkerpop/cypher-gremlin-console-plugin) installed.

## Usage

1. Build and run image or `docker run --rm -it -e JAVA_OPTIONS="-Dplugins=v3d3" --net="host" --name=cypher-gremlin-console neueda/cypher-gremlin-console`
2. Console history contains examples (press `↑` or `Ctrl+R`)
  - For client side translation append `translate gremlin`, `translate neptune`, `translate cosmosdb` to `:remote connect` command 
  - For server side translation connect to [Gremlin Server with Gremlin Server Cypher Plugin](../cypher-gremlin-server) 

## Development

Docker and Make should be installed and found in `PATH`.

Command | Description
------- | -----------
`make build`  | Build the image and tag as `cypher-for-gremlin/cypher-gremlin-console`
`make run`    | Run the latest build image
`make sh`     | Open container's shell
