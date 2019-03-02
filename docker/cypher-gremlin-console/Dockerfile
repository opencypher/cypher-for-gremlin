FROM tinkerpop/gremlin-console:3.4.0

COPY libs/ conf/
COPY plugins.txt ext/
COPY .gremlin_groovy_history /root
COPY libs/*.jar ext/cypher-for-gremlin/plugin/
COPY libs/*.jar ext/cypher-for-gremlin/lib/

