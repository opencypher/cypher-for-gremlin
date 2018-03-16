MATCH (n)
DETACH DELETE n;

CREATE (marko:person {name: 'marko', age: 29})
CREATE (vadas:person {name: 'vadas', age: 27})
CREATE (josh:person {name: 'josh', age: 32})
CREATE (peter:person {name: 'peter', age: 35})
CREATE (lop:software {name: 'lop', lang: 'java'})
CREATE (ripple:software {name: 'ripple', lang: 'java'})
CREATE (marko)-[:knows {weight: 0.5}]->(vadas)
CREATE (marko)-[:knows {weight: 1.0}]->(josh)
CREATE (marko)-[:created {weight: 0.4}]->(lop)
CREATE (peter)-[:created {weight: 0.2}]->(lop)
CREATE (josh)-[:created {weight: 0.4}]->(lop)
CREATE (josh)-[:created {weight: 1.0}]->(ripple);
