CREATE (id52171:City {cityId:52171, name:'Copenhagen'})
CREATE (id53019:City {cityId:53019, name:'Oslo'})
CREATE (id53109:City {cityId:53109, name:'Helsinki'})
CREATE (id53541:Continent {continentId:53541, name:'Europe'})
CREATE (id53467:Country {countryId:53467, name:'Denmark'})
CREATE (id53500:Country {countryId:53500, name:'Finland'})
CREATE (id53516:Country {countryId:53516, name:'Norway'})
CREATE (id53546:Interest {interestId:53546, name:'Book'})
CREATE (id53547:Interest {interestId:53547, name:'Video'})
CREATE (id53548:Interest {interestId:53548, name:'Music'})
CREATE (id53549:Interest {interestId:53549, name:'DVD'})
CREATE (id246:Person {personId:246, firstName:'Erlend', lastName:'Pedersen', born:1996, email:'Erlend2199023287971@gmail.com'})
CREATE (id777:Person {personId:777, firstName:'Lars', lastName:'Andresen', born:1986, email:'Lars.Nielsen.6711515243877401003@yahoo.com'})
CREATE (id840:Person {personId:840, firstName:'Olavi', lastName:'Lehtinen', born:1983, email:'Olavi4398046541996@gmx.com'})
CREATE (id20445:Person {personId:20445, firstName:'Martin', lastName:'Haugland', born:1984, email:'Martin4398046518130@hotmail.com'})
CREATE (id21478:Person {personId:21478, firstName:'Erik', lastName:'Andresen', born:1995, email:'ErikJunior@gmx.com'})
CREATE (id29765:Person {personId:29765, firstName:'Erik', lastName:'Andresen', born:1988, email:'TheErik@gmail.com'})
CREATE (id29766:Person {personId:29765, firstName:'Erik', lastName:'Andresen', born:1988, email:'ErikTheSecond4@gmail.com'})
CREATE
    (id246)-[:IS_LOCATED_IN]->(id53019),
    (id777)-[:IS_LOCATED_IN]->(id52171),
    (id840)-[:IS_LOCATED_IN]->(id53109),
    (id20445)-[:IS_LOCATED_IN]->(id53019),
    (id21478)-[:IS_LOCATED_IN]->(id52171),
    (id29765)-[:IS_LOCATED_IN]->(id53109),
    (id52171)-[:IS_PART_OF]->(id53467),
    (id53109)-[:IS_PART_OF]->(id53500),
    (id53019)-[:IS_PART_OF]->(id53516),
    (id53516)-[:IS_PART_OF]->(id53541),
    (id53500)-[:IS_PART_OF]->(id53541),
    (id53467)-[:IS_PART_OF]->(id53541),
    (id246)-[:KNOWS]->(id246),
    (id777)-[:KNOWS]->(id840),
    (id840)-[:KNOWS]->(id777),
    (id20445)-[:KNOWS]->(id246),
    (id21478)-[:KNOWS]->(id246),
    (id29765)-[:KNOWS]->(id246),
    (id20445)-[:LIKES]->(id53547),
    (id21478)-[:LIKES]->(id53547),
    (id29765)-[:LIKES]->(id53547),
    (id29765)-[:LIKES]->(id53548);
