# Gremlin Server Cypher Plugin

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-plugin/badge.svg?style=shield)](https://maven-badges.herokuapp.com/maven-central/org.opencypher.gremlin/cypher-gremlin-server-plugin)

Included in the plugin:

- [`OpProcessor`](https://tinkerpop.apache.org/docs/current/reference/#opprocessor-configurations) implementation for Gremlin Server that translates Cypher queries to [`GraphTraversal`](https://tinkerpop.apache.org/docs/current/reference/#traversal)
- [Gremlin Extensions for Cypher](../cypher-gremlin-extensions) for Gremlin in the form of custom functions and predicates


## Installation

The plugin and its dependencies can be automatically downloaded and installed into the Gremlin Server by using `bin/gremlin-server.sh install`. You can also manually "install" the plugin by copying jar file into the server classpath.

Cypher for Gremlin could be installed on any TinkerPop server with ability to install Gremlin Server plugins. Latest version of Cypher for Gremlin has dependencies on TinkerPop 3.4.2 and Scala 2.12, so for legacy implementations please use previous releases. For more information about compatibility with different Gremlin implementation refer to [documentation](https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations).

`$VERSION` depends on target implementation:


|             Gremlin Server             | Cypher for Gremlin |
|----------------------------------------|--------------------|
| TinkerPop 3.4.x                        | 1.0.3 (latest)     |
| TinkerPop 3.3.x                        | 0.9.13             |
| JanusGraph 0.4.x ([Scala 2.12][scala]) | 1.0.3 (latest)     |
| JanusGraph 0.4.x                       | 1.0.0              |
| JanusGraph 0.3.x                       | 0.9.13             |
| JanusGraph 0.2.x                       | Not compatible     |

[scala]: https://github.com/opencypher/cypher-for-gremlin/wiki/Gremlin-implementations#janusgraph-with-cypher-plugin


### Automated Installation

Run `bin/gremlin-server.sh` with `install` switch and supply the Maven coordinates of the plugin:

```sh
bin/gremlin-server.sh install org.opencypher.gremlin cypher-gremlin-server-plugin $VERSION
Installing dependency org.opencypher.gremlin cypher-gremlin-server-plugin $VERSION
...
```
  ```
  
### Manual Installation

Run the following commands from project root.

1. Download [compatible release](https://github.com/opencypher/cypher-for-gremlin/releases) or build the plugin JAR file:
   ```sh
   ./gradlew :tinkerpop:cypher-gremlin-server-plugin:shadowJar
   ```
1. Copy plugin JAR file to Gremlin Server `lib/` directory:
   ```sh
   cp tinkerpop/cypher-gremlin-server-plugin/build/libs/cypher-gremlin-server-plugin-*-all.jar /path/to/gremlin-server/lib/
   ```
1. [Register](https://tinkerpop.apache.org/docs/current/reference/#opprocessor-configurations) the `org.opencypher.gremlin.server.op.cypher.CypherOpProcessor`.
1. Add `['org.opencypher.gremlin.process.traversal.CustomPredicates.*']` to [Gremlin Server configuration file](https://tinkerpop.apache.org/docs/current/reference/#_configuring_2) at `scriptEngines`/`gremlin-groovy`/`staticImports`.
1. Restart Gremlin Server.
1. If the plugin has been installed correctly, you should see the following line among the logs:
   ```
   [INFO] OpLoader - Adding the cypher OpProcessor.
   ```
   
## Usage

### Gremlin-Java/Gremlin-Groovy

Recommended way is to use [Cypher Client for Gremlin Server](https://github.com/opencypher/cypher-for-gremlin/tree/master/tinkerpop/cypher-gremlin-server-client),
however it is possible to send Cypher using [Gremlin Client](http://tinkerpop.apache.org/docs/current/reference/#gremlin-java) by creating custom `RequestMessage`:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/java/org/opencypher/gremlin/snippets/CypherGremlinServerClientSnippets.java#gremlinClient) -->
```java
Cluster cluster = Cluster.open(configuration);
Client client = cluster.connect();

String cypherQuery = "MATCH (n) RETURN n.name";
RequestMessage request = RequestMessage.build(Tokens.OPS_EVAL)
    .processor("cypher")
    .add(Tokens.ARGS_GREMLIN, cypherQuery)
    .create();

ResultSet results = client.submitAsync(request).get();
```

### Gremlin-Javascript

Example connect using [Gremlin-JavaScript](http://tinkerpop.apache.org/docs/current/reference/#gremlin-javascript) 3.4.2+ by setting `processor` to `cypher`:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/resources/snippets/gremlin-javascript.js#example) -->
```js
// npm install gremlin@3.4.2

const gremlin = require('gremlin');
const client = new gremlin.driver.Client('ws://localhost:8182/gremlin', { traversalSource: 'g', processor: 'cypher'});
const cypherQuery = 'MATCH (n) RETURN n.name'

const results = await client.submit(cypherQuery);

for (const result of results) {
  console.log(result);
}
```

### Gremlin-Python

Example connect using [Gremlin-Python](http://tinkerpop.apache.org/docs/current/reference/#gremlin-python) by creating custom `RequestMessage`:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/resources/snippets/gremlin-python.py#example) -->
```python
from gremlin_python.driver.client import Client
from gremlin_python.driver.request import RequestMessage
from gremlin_python.driver.serializer import GraphSONMessageSerializer

serializer = GraphSONMessageSerializer()
# workaround to avoid exception on any opProcessor other than `standard` or `traversal`:
serializer.cypher = serializer.standard

client = Client('ws://localhost:8182/gremlin', 'g', message_serializer=serializer)

cypherQuery = 'MATCH (n) RETURN n.name'
message = RequestMessage('cypher', 'eval', {'gremlin': cypherQuery})
results = client.submit(message).all().result()
```

### Gremlin.Net

Example connect using [Gremlin.Net](http://tinkerpop.apache.org/docs/current/reference/#gremlin-DotNet) by creating custom `RequestMessage`:

<!-- [freshReadmeSource](../../testware/integration-tests/src/test/resources/snippets/gremlin-dotnet.cs#example) -->
```csharp
var client = new GremlinClient(new GremlinServer("localhost", 8182));
var cypherQuery = "MATCH (n) RETURN n.name";
var requestMessage = RequestMessage.Build(Tokens.OpsEval)
                .AddArgument(Tokens.ArgsGremlin, cypherQuery)
                .Processor("cypher")
                .Create();
var result = await client.SubmitAsync<Dictionary<object, object>>(requestMessage);
```

## Configuration

In most of the cases, the plugin does not need any additional configuration.  However, if you know what you are doing it is possible to configure the translator.

Configuration options:

* `translatorDefinition` - full translator definition in format: `"FLAVOR[+FEATURE][+FEATURE]..."`
* `translatorFeatures` - additional [TranslatorFeature](https://opencypher.github.io/cypher-for-gremlin/api/1.0.3/java/org/opencypher/gremlin/translation/translator/TranslatorFeature.html#skip.navbar.top) that will be added to default configuration

For examples, refer to `DEFAULT_TRANSLATOR_DEFINITION` in [CypherOpProcessor](src/main/java/org/opencypher/gremlin/server/op/cypher/CypherOpProcessor.java#L70) or [Translator.FlavorBuilder#build(String)](https://opencypher.github.io/cypher-for-gremlin/api/1.0.3/java/org/opencypher/gremlin/translation/translator/Translator.FlavorBuilder.html#build-java.lang.String-).

In `processors` section of [Gremlin server configuration](https://github.com/apache/tinkerpop/blob/master/gremlin-server/conf/gremlin-server.yaml) add following:

```yaml
processors:
# ...
  - { className: org.opencypher.gremlin.server.op.cypher.CypherOpProcessor, config: { translatorFeatures: "+multiple_labels" }}
# ...
```  



## Troubleshooting

- Make sure that Gremlin Server or the database you are using is based on TinkerPop 3.4.0 or later.
