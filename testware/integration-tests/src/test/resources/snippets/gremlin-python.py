#!/usr/bin/python3

from gremlin_python.driver.client import Client
from gremlin_python.driver.serializer import GraphSONMessageSerializer
from gremlin_python.driver.request import RequestMessage

serializer = GraphSONMessageSerializer()
serializer.cypher = serializer.standard

client = Client('ws://localhost:8182/gremlin', 'g', message_serializer=serializer)

message = RequestMessage('cypher', 'eval', {'gremlin': 'MATCH (n) RETURN n'})
result_set = client.submit(message)

print(result_set.all().result())
