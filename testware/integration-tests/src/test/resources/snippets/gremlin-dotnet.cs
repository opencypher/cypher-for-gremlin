var client = new GremlinClient(new GremlinServer(GremlinServerHostname, GremlinServerPort));
var requestMessage = RequestMessage.Build(Tokens.OpsEval)
                .AddArgument(Tokens.ArgsGremlin, "RETURN 2")
                .Processor("cypher")
                .Create();
var result = await client.SubmitAsync<object>(requestMessage);
