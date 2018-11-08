CREATE (marko:person {name: "marko", age: 29})
CREATE (vadas:person {name: "vadas", age: 27})
CREATE (lop:software { name: "lop", lang: "java"})
CREATE (josh:person {name: "josh", age: 32})
CREATE (ripple:software {name: "ripple", lang: "java"})
CREATE (peter:person { name: "peter", age: 35}),
(marko)-[marko_knows_vadas:knows {weight: 0.5}]->(vadas),
(marko)-[marko_knows_josh:knows {weight: 1.0}]->(josh),
(marko)-[marko_created_lop:created {weight: 0.4}]->(lop),
(josh)-[josh_created_ripple:created {weight: 1.0}]->(ripple),
(josh)-[josh_created_lop:created {weight: 0.4}]->(lop),
(peter)-[peter_created_lop:created {weight: 0.2}]->(lop)
RETURN marko, vadas, lop, josh, ripple, peter, marko_knows_vadas, marko_knows_josh, marko_created_lop,
  josh_created_ripple, josh_created_lop, peter_created_lop;
