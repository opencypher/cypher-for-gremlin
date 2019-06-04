
async function start() {
    // freshReadmeSnippet: example
    // npm install gremlin@3.4.2

    const gremlin = require('gremlin');
    const client = new gremlin.driver.Client('ws://localhost:8182/gremlin', { traversalSource: 'g', processor: 'cypher'});
    const cypherQuery = 'MATCH (n) RETURN n.name'

    const results = await client.submit(cypherQuery);

    for (const result of results) {
      console.log(result);
    }
    // freshReadmeSnippet: example
}

start();
