FROM tinkerpop/gremlin-console:3.4.2

COPY libs/ conf/
COPY plugins.txt ext/
COPY --chown=gremlin .gremlin_groovy_history /home/gremlin/
COPY libs/*.jar ext/cypher-for-gremlin/plugin/
COPY libs/*.jar ext/cypher-for-gremlin/lib/

