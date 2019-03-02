FROM tinkerpop/gremlin-server:3.4.0

COPY libs/*.jar lib/

COPY conf/gremlin-server-modern.yaml /opt/gremlin-server/conf/gremlin-server-modern.yaml

CMD ["conf/gremlin-server-modern.yaml"]
