// npm install gremlin@2.7.0

const gremlin = require('gremlin');

const client = gremlin.createClient(8182, "localhost", {processor: "cypher"})

client.execute('MATCH (n) RETURN count(n)', (err, results) => {
    console.log(results)
});
