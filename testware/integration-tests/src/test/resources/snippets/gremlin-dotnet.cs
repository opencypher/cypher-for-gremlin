using System;
using System.Diagnostics;
using Gremlin.Net.Driver;
using Gremlin.Net.Driver.Messages;
using Gremlin.Net.Driver.Remote;
using Gremlin.Net.Structure;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;


using static Gremlin.Net.Process.Traversal.AnonymousTraversalSource;

namespace dotnetapp
{
    internal class Program
    {
        static private async Task callWebApi()
        {
            // freshReadmeSnippet: example
            var client = new GremlinClient(new GremlinServer("localhost", 8182));
            var cypherQuery = "MATCH (n) RETURN n.name";
            var requestMessage = RequestMessage.Build(Tokens.OpsEval)
                            .AddArgument(Tokens.ArgsGremlin, cypherQuery)
                            .Processor("cypher")
                            .Create();
            var result = await client.SubmitAsync<Dictionary<object, object>>(requestMessage);
            // freshReadmeSnippet: example

            List<object> results = new List<object>();
            foreach (var c in result)
            {
                foreach (var n in (c.Values)) {
                    Console.WriteLine(n);
                    results.Add(n);
                }
            }

            var expected = new List<object>() { "marko", "vadas", "lop", "josh", "ripple", "peter" };

            Debug.Assert(results.SequenceEqual(expected));
        }


        private static void Main()
        {
            callWebApi().Wait();
        }
    }
}
