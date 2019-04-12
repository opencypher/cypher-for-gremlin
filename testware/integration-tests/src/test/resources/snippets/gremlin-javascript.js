// npm install gremlin@2.7.0

const gremlin = require('gremlin');

const client = gremlin.createClient(8182, "localhost", {processor: "cypher"})

const cypherQuery = 'MATCH (n) RETURN n.name'

client.execute(cypherQuery, (err, results) => {
    console.log(results)
});
