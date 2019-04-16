#!/usr/bin/python3


# freshReadmeSnippet: example
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
# freshReadmeSnippet: example

print(results)

assert results==[{'n.name': 'marko'}, {'n.name': 'vadas'}, {'n.name': 'lop'}, {'n.name': 'josh'}, {'n.name': 'ripple'}, {'n.name': 'peter'}]
