CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})
CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})
CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})
CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})
CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})
CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})
CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})
CREATE (JoelS:Person {name:'Joel Silver', born:1952})
CREATE
    (Keanu)-[:ACTED_IN]->(TheMatrix),
    (Carrie)-[:ACTED_IN]->(TheMatrix),
    (Laurence)-[:ACTED_IN]->(TheMatrix),
    (Hugo)-[:ACTED_IN]->(TheMatrix),
    (LillyW)-[:DIRECTED]->(TheMatrix),
    (LanaW)-[:DIRECTED]->(TheMatrix),
    (JoelS)-[:PRODUCED]->(TheMatrix)

CREATE (Emil:Person {name:"Emil Eifrem", born:1978})
CREATE (Emil)-[:ACTED_IN]->(TheMatrix)

CREATE (TheMatrixReloaded:Movie {title:'The Matrix Reloaded', released:2003, tagline:'Free your mind'})
CREATE
    (Keanu)-[:ACTED_IN]->(TheMatrixReloaded),
    (Carrie)-[:ACTED_IN]->(TheMatrixReloaded),
    (Laurence)-[:ACTED_IN]->(TheMatrixReloaded),
    (Hugo)-[:ACTED_IN]->(TheMatrixReloaded),
    (LillyW)-[:DIRECTED]->(TheMatrixReloaded),
    (LanaW)-[:DIRECTED]->(TheMatrixReloaded),
    (JoelS)-[:PRODUCED]->(TheMatrixReloaded)

CREATE (TheMatrixRevolutions:Movie {title:'The Matrix Revolutions', released:2003, tagline:'Everything that has a beginning has an end'})
CREATE
    (Keanu)-[:ACTED_IN]->(TheMatrixRevolutions),
    (Carrie)-[:ACTED_IN]->(TheMatrixRevolutions),
    (Laurence)-[:ACTED_IN]->(TheMatrixRevolutions),
    (Hugo)-[:ACTED_IN]->(TheMatrixRevolutions),
    (LillyW)-[:DIRECTED]->(TheMatrixRevolutions),
    (LanaW)-[:DIRECTED]->(TheMatrixRevolutions),
    (JoelS)-[:PRODUCED]->(TheMatrixRevolutions)

CREATE (TheDevilsAdvocate:Movie {title:"The Devil's Advocate", released:1997, tagline:'Evil has its winning ways'})
CREATE (Charlize:Person {name:'Charlize Theron', born:1975})
CREATE (Al:Person {name:'Al Pacino', born:1940})
CREATE (Taylor:Person {name:'Taylor Hackford', born:1944})
CREATE
    (Keanu)-[:ACTED_IN]->(TheDevilsAdvocate),
    (Charlize)-[:ACTED_IN]->(TheDevilsAdvocate),
    (Al)-[:ACTED_IN]->(TheDevilsAdvocate),
    (Taylor)-[:DIRECTED]->(TheDevilsAdvocate)

CREATE (AFewGoodMen:Movie {title:"A Few Good Men", released:1992, tagline:"In the heart of the nation's capital, in a courthouse of the U.S. government, one man will stop at nothing to keep his honor, and one will stop at nothing to find the truth."})
CREATE (TomC:Person {name:'Tom Cruise', born:1962})
CREATE (JackN:Person {name:'Jack Nicholson', born:1937})
CREATE (DemiM:Person {name:'Demi Moore', born:1962})
CREATE (KevinB:Person {name:'Kevin Bacon', born:1958})
CREATE (KieferS:Person {name:'Kiefer Sutherland', born:1966})
CREATE (NoahW:Person {name:'Noah Wyle', born:1971})
CREATE (CubaG:Person {name:'Cuba Gooding Jr.', born:1968})
CREATE (KevinP:Person {name:'Kevin Pollak', born:1957})
CREATE (JTW:Person {name:'J.T. Walsh', born:1943})
CREATE (JamesM:Person {name:'James Marshall', born:1967})
CREATE (ChristopherG:Person {name:'Christopher Guest', born:1948})
CREATE (RobR:Person {name:'Rob Reiner', born:1947})
CREATE (AaronS:Person {name:'Aaron Sorkin', born:1961})
CREATE
    (TomC)-[:ACTED_IN]->(AFewGoodMen),
    (JackN)-[:ACTED_IN]->(AFewGoodMen),
    (DemiM)-[:ACTED_IN]->(AFewGoodMen),
    (KevinB)-[:ACTED_IN]->(AFewGoodMen),
    (KieferS)-[:ACTED_IN]->(AFewGoodMen),
    (NoahW)-[:ACTED_IN]->(AFewGoodMen),
    (CubaG)-[:ACTED_IN]->(AFewGoodMen),
    (KevinP)-[:ACTED_IN]->(AFewGoodMen),
    (JTW)-[:ACTED_IN]->(AFewGoodMen),
    (JamesM)-[:ACTED_IN]->(AFewGoodMen),
    (ChristopherG)-[:ACTED_IN]->(AFewGoodMen),
    (AaronS)-[:ACTED_IN]->(AFewGoodMen),
    (RobR)-[:DIRECTED]->(AFewGoodMen),
    (AaronS)-[:WROTE]->(AFewGoodMen)

CREATE (TopGun:Movie {title:"Top Gun", released:1986, tagline:'I feel the need, the need for speed.'})
CREATE (KellyM:Person {name:'Kelly McGillis', born:1957})
CREATE (ValK:Person {name:'Val Kilmer', born:1959})
CREATE (AnthonyE:Person {name:'Anthony Edwards', born:1962})
CREATE (TomS:Person {name:'Tom Skerritt', born:1933})
CREATE (MegR:Person {name:'Meg Ryan', born:1961})
CREATE (TonyS:Person {name:'Tony Scott', born:1944})
CREATE (JimC:Person {name:'Jim Cash', born:1941})
CREATE
    (TomC)-[:ACTED_IN]->(TopGun),
    (KellyM)-[:ACTED_IN]->(TopGun),
    (ValK)-[:ACTED_IN]->(TopGun),
    (AnthonyE)-[:ACTED_IN]->(TopGun),
    (TomS)-[:ACTED_IN]->(TopGun),
    (MegR)-[:ACTED_IN]->(TopGun),
    (TonyS)-[:DIRECTED]->(TopGun),
    (JimC)-[:WROTE]->(TopGun)

CREATE (JerryMaguire:Movie {title:'Jerry Maguire', released:2000, tagline:'The rest of his life begins now.'})
CREATE (ReneeZ:Person {name:'Renee Zellweger', born:1969})
CREATE (KellyP:Person {name:'Kelly Preston', born:1962})
CREATE (JerryO:Person {name:"Jerry O'Connell", born:1974})
CREATE (JayM:Person {name:'Jay Mohr', born:1970})
CREATE (BonnieH:Person {name:'Bonnie Hunt', born:1961})
CREATE (ReginaK:Person {name:'Regina King', born:1971})
CREATE (JonathanL:Person {name:'Jonathan Lipnicki', born:1996})
CREATE (CameronC:Person {name:'Cameron Crowe', born:1957})
CREATE
    (TomC)-[:ACTED_IN]->(JerryMaguire),
    (CubaG)-[:ACTED_IN]->(JerryMaguire),
    (ReneeZ)-[:ACTED_IN]->(JerryMaguire),
    (KellyP)-[:ACTED_IN]->(JerryMaguire),
    (JerryO)-[:ACTED_IN]->(JerryMaguire),
    (JayM)-[:ACTED_IN]->(JerryMaguire),
    (BonnieH)-[:ACTED_IN]->(JerryMaguire),
    (ReginaK)-[:ACTED_IN]->(JerryMaguire),
    (JonathanL)-[:ACTED_IN]->(JerryMaguire),
    (CameronC)-[:DIRECTED]->(JerryMaguire),
    (CameronC)-[:PRODUCED]->(JerryMaguire),
    (CameronC)-[:WROTE]->(JerryMaguire)

CREATE (StandByMe:Movie {title:"Stand By Me", released:1986, tagline:"For some, it's the last real taste of innocence, and the first real taste of life. But for everyone, it's the time that memories are made of."})
CREATE (RiverP:Person {name:'River Phoenix', born:1970})
CREATE (CoreyF:Person {name:'Corey Feldman', born:1971})
CREATE (WilW:Person {name:'Wil Wheaton', born:1972})
CREATE (JohnC:Person {name:'John Cusack', born:1966})
CREATE (MarshallB:Person {name:'Marshall Bell', born:1942})
CREATE
    (WilW)-[:ACTED_IN]->(StandByMe),
    (RiverP)-[:ACTED_IN]->(StandByMe),
    (JerryO)-[:ACTED_IN]->(StandByMe),
    (CoreyF)-[:ACTED_IN]->(StandByMe),
    (JohnC)-[:ACTED_IN]->(StandByMe),
    (KieferS)-[:ACTED_IN]->(StandByMe),
    (MarshallB)-[:ACTED_IN]->(StandByMe),
    (RobR)-[:DIRECTED]->(StandByMe)

CREATE (AsGoodAsItGets:Movie {title:'As Good as It Gets', released:1997, tagline:'A comedy from the heart that goes for the throat.'})
CREATE (HelenH:Person {name:'Helen Hunt', born:1963})
CREATE (GregK:Person {name:'Greg Kinnear', born:1963})
CREATE (JamesB:Person {name:'James L. Brooks', born:1940})
CREATE
    (JackN)-[:ACTED_IN]->(AsGoodAsItGets),
    (HelenH)-[:ACTED_IN]->(AsGoodAsItGets),
    (GregK)-[:ACTED_IN]->(AsGoodAsItGets),
    (CubaG)-[:ACTED_IN]->(AsGoodAsItGets),
    (JamesB)-[:DIRECTED]->(AsGoodAsItGets)

CREATE (WhatDreamsMayCome:Movie {title:'What Dreams May Come', released:1998, tagline:'After life there is more. The end is just the beginning.'})
CREATE (AnnabellaS:Person {name:'Annabella Sciorra', born:1960})
CREATE (MaxS:Person {name:'Max von Sydow', born:1929})
CREATE (WernerH:Person {name:'Werner Herzog', born:1942})
CREATE (Robin:Person {name:'Robin Williams', born:1951})
CREATE (VincentW:Person {name:'Vincent Ward', born:1956})
CREATE
    (Robin)-[:ACTED_IN]->(WhatDreamsMayCome),
    (CubaG)-[:ACTED_IN]->(WhatDreamsMayCome),
    (AnnabellaS)-[:ACTED_IN]->(WhatDreamsMayCome),
    (MaxS)-[:ACTED_IN]->(WhatDreamsMayCome),
    (WernerH)-[:ACTED_IN]->(WhatDreamsMayCome),
    (VincentW)-[:DIRECTED]->(WhatDreamsMayCome)

CREATE (SnowFallingonCedars:Movie {title:'Snow Falling on Cedars', released:1999, tagline:'First loves last. Forever.'})
CREATE (EthanH:Person {name:'Ethan Hawke', born:1970})
CREATE (RickY:Person {name:'Rick Yune', born:1971})
CREATE (JamesC:Person {name:'James Cromwell', born:1940})
CREATE (ScottH:Person {name:'Scott Hicks', born:1953})
CREATE
    (EthanH)-[:ACTED_IN]->(SnowFallingonCedars),
    (RickY)-[:ACTED_IN]->(SnowFallingonCedars),
    (MaxS)-[:ACTED_IN]->(SnowFallingonCedars),
    (JamesC)-[:ACTED_IN]->(SnowFallingonCedars),
    (ScottH)-[:DIRECTED]->(SnowFallingonCedars)

CREATE (YouveGotMail:Movie {title:"You've Got Mail", released:1998, tagline:'At odds in life... in love on-line.'})
CREATE (ParkerP:Person {name:'Parker Posey', born:1968})
CREATE (DaveC:Person {name:'Dave Chappelle', born:1973})
CREATE (SteveZ:Person {name:'Steve Zahn', born:1967})
CREATE (TomH:Person {name:'Tom Hanks', born:1956})
CREATE (NoraE:Person {name:'Nora Ephron', born:1941})
CREATE
    (TomH)-[:ACTED_IN]->(YouveGotMail),
    (MegR)-[:ACTED_IN]->(YouveGotMail),
    (GregK)-[:ACTED_IN]->(YouveGotMail),
    (ParkerP)-[:ACTED_IN]->(YouveGotMail),
    (DaveC)-[:ACTED_IN]->(YouveGotMail),
    (SteveZ)-[:ACTED_IN]->(YouveGotMail),
    (NoraE)-[:DIRECTED]->(YouveGotMail)

CREATE (SleeplessInSeattle:Movie {title:'Sleepless in Seattle', released:1993, tagline:'What if someone you never met, someone you never saw, someone you never knew was the only someone for you?'})
CREATE (RitaW:Person {name:'Rita Wilson', born:1956})
CREATE (BillPull:Person {name:'Bill Pullman', born:1953})
CREATE (VictorG:Person {name:'Victor Garber', born:1949})
CREATE (RosieO:Person {name:"Rosie O'Donnell", born:1962})
CREATE
    (TomH)-[:ACTED_IN]->(SleeplessInSeattle),
    (MegR)-[:ACTED_IN]->(SleeplessInSeattle),
    (RitaW)-[:ACTED_IN]->(SleeplessInSeattle),
    (BillPull)-[:ACTED_IN]->(SleeplessInSeattle),
    (VictorG)-[:ACTED_IN]->(SleeplessInSeattle),
    (RosieO)-[:ACTED_IN]->(SleeplessInSeattle),
    (NoraE)-[:DIRECTED]->(SleeplessInSeattle)

CREATE (JoeVersustheVolcano:Movie {title:'Joe Versus the Volcano', released:1990, tagline:'A story of love, lava and burning desire.'})
CREATE (JohnS:Person {name:'John Patrick Stanley', born:1950})
CREATE (Nathan:Person {name:'Nathan Lane', born:1956})
CREATE
    (TomH)-[:ACTED_IN]->(JoeVersustheVolcano),
    (MegR)-[:ACTED_IN]->(JoeVersustheVolcano),
    (Nathan)-[:ACTED_IN]->(JoeVersustheVolcano),
    (JohnS)-[:DIRECTED]->(JoeVersustheVolcano)

CREATE (WhenHarryMetSally:Movie {title:'When Harry Met Sally', released:1998, tagline:'At odds in life... in love on-line.'})
CREATE (BillyC:Person {name:'Billy Crystal', born:1948})
CREATE (CarrieF:Person {name:'Carrie Fisher', born:1956})
CREATE (BrunoK:Person {name:'Bruno Kirby', born:1949})
CREATE
    (BillyC)-[:ACTED_IN]->(WhenHarryMetSally),
    (MegR)-[:ACTED_IN]->(WhenHarryMetSally),
    (CarrieF)-[:ACTED_IN]->(WhenHarryMetSally),
    (BrunoK)-[:ACTED_IN]->(WhenHarryMetSally),
    (RobR)-[:DIRECTED]->(WhenHarryMetSally),
    (RobR)-[:PRODUCED]->(WhenHarryMetSally),
    (NoraE)-[:PRODUCED]->(WhenHarryMetSally),
    (NoraE)-[:WROTE]->(WhenHarryMetSally)

CREATE (ThatThingYouDo:Movie {title:'That Thing You Do', released:1996, tagline:'In every life there comes a time when that thing you dream becomes that thing you do'})
CREATE (LivT:Person {name:'Liv Tyler', born:1977})
CREATE
    (TomH)-[:ACTED_IN]->(ThatThingYouDo),
    (LivT)-[:ACTED_IN]->(ThatThingYouDo),
    (Charlize)-[:ACTED_IN]->(ThatThingYouDo),
    (TomH)-[:DIRECTED]->(ThatThingYouDo)

CREATE (TheReplacements:Movie {title:'The Replacements', released:2000, tagline:'Pain heals, Chicks dig scars... Glory lasts forever'})
CREATE (Brooke:Person {name:'Brooke Langton', born:1970})
CREATE (Gene:Person {name:'Gene Hackman', born:1930})
CREATE (Orlando:Person {name:'Orlando Jones', born:1968})
CREATE (Howard:Person {name:'Howard Deutch', born:1950})
CREATE
    (Keanu)-[:ACTED_IN]->(TheReplacements),
    (Brooke)-[:ACTED_IN]->(TheReplacements),
    (Gene)-[:ACTED_IN]->(TheReplacements),
    (Orlando)-[:ACTED_IN]->(TheReplacements),
    (Howard)-[:DIRECTED]->(TheReplacements)

CREATE (RescueDawn:Movie {title:'RescueDawn', released:2006, tagline:"Based on the extraordinary true story of one man's fight for freedom"})
CREATE (ChristianB:Person {name:'Christian Bale', born:1974})
CREATE (ZachG:Person {name:'Zach Grenier', born:1954})
CREATE
    (MarshallB)-[:ACTED_IN]->(RescueDawn),
    (ChristianB)-[:ACTED_IN]->(RescueDawn),
    (ZachG)-[:ACTED_IN]->(RescueDawn),
    (SteveZ)-[:ACTED_IN]->(RescueDawn),
    (WernerH)-[:DIRECTED]->(RescueDawn)

CREATE (TheBirdcage:Movie {title:'The Birdcage', released:1996, tagline:'Come as you are'})
CREATE (MikeN:Person {name:'Mike Nichols', born:1931})
CREATE
    (Robin)-[:ACTED_IN]->(TheBirdcage),
    (Nathan)-[:ACTED_IN]->(TheBirdcage),
    (Gene)-[:ACTED_IN]->(TheBirdcage),
    (MikeN)-[:DIRECTED]->(TheBirdcage)

CREATE (Unforgiven:Movie {title:'Unforgiven', released:1992, tagline:"It's a hell of a thing, killing a man"})
CREATE (RichardH:Person {name:'Richard Harris', born:1930})
CREATE (ClintE:Person {name:'Clint Eastwood', born:1930})
CREATE
    (RichardH)-[:ACTED_IN]->(Unforgiven),
    (ClintE)-[:ACTED_IN]->(Unforgiven),
    (Gene)-[:ACTED_IN]->(Unforgiven),
    (ClintE)-[:DIRECTED]->(Unforgiven)

CREATE (JohnnyMnemonic:Movie {title:'Johnny Mnemonic', released:1995, tagline:'The hottest data on earth. In the coolest head in town'})
CREATE (Takeshi:Person {name:'Takeshi Kitano', born:1947})
CREATE (Dina:Person {name:'Dina Meyer', born:1968})
CREATE (IceT:Person {name:'Ice-T', born:1958})
CREATE (RobertL:Person {name:'Robert Longo', born:1953})
CREATE
    (Keanu)-[:ACTED_IN]->(JohnnyMnemonic),
    (Takeshi)-[:ACTED_IN]->(JohnnyMnemonic),
    (Dina)-[:ACTED_IN]->(JohnnyMnemonic),
    (IceT)-[:ACTED_IN]->(JohnnyMnemonic),
    (RobertL)-[:DIRECTED]->(JohnnyMnemonic)

CREATE (CloudAtlas:Movie {title:'Cloud Atlas', released:2012, tagline:'Everything is connected'})
CREATE (HalleB:Person {name:'Halle Berry', born:1966})
CREATE (JimB:Person {name:'Jim Broadbent', born:1949})
CREATE (TomT:Person {name:'Tom Tykwer', born:1965})
CREATE (DavidMitchell:Person {name:'David Mitchell', born:1969})
CREATE (StefanArndt:Person {name:'Stefan Arndt', born:1961})
CREATE
    (TomH)-[:ACTED_IN]->(CloudAtlas),
    (Hugo)-[:ACTED_IN]->(CloudAtlas),
    (HalleB)-[:ACTED_IN]->(CloudAtlas),
    (JimB)-[:ACTED_IN]->(CloudAtlas),
    (TomT)-[:DIRECTED]->(CloudAtlas),
    (LillyW)-[:DIRECTED]->(CloudAtlas),
    (LanaW)-[:DIRECTED]->(CloudAtlas),
    (DavidMitchell)-[:WROTE]->(CloudAtlas),
    (StefanArndt)-[:PRODUCED]->(CloudAtlas)

CREATE (TheDaVinciCode:Movie {title:'The Da Vinci Code', released:2006, tagline:'Break The Codes'})
CREATE (IanM:Person {name:'Ian McKellen', born:1939})
CREATE (AudreyT:Person {name:'Audrey Tautou', born:1976})
CREATE (PaulB:Person {name:'Paul Bettany', born:1971})
CREATE (RonH:Person {name:'Ron Howard', born:1954})
CREATE
    (TomH)-[:ACTED_IN]->(TheDaVinciCode),
    (IanM)-[:ACTED_IN]->(TheDaVinciCode),
    (AudreyT)-[:ACTED_IN]->(TheDaVinciCode),
    (PaulB)-[:ACTED_IN]->(TheDaVinciCode),
    (RonH)-[:DIRECTED]->(TheDaVinciCode)

CREATE (VforVendetta:Movie {title:'V for Vendetta', released:2006, tagline:'Freedom! Forever!'})
CREATE (NatalieP:Person {name:'Natalie Portman', born:1981})
CREATE (StephenR:Person {name:'Stephen Rea', born:1946})
CREATE (JohnH:Person {name:'John Hurt', born:1940})
CREATE (BenM:Person {name: 'Ben Miles', born:1967})
CREATE
    (Hugo)-[:ACTED_IN]->(VforVendetta),
    (NatalieP)-[:ACTED_IN]->(VforVendetta),
    (StephenR)-[:ACTED_IN]->(VforVendetta),
    (JohnH)-[:ACTED_IN]->(VforVendetta),
    (BenM)-[:ACTED_IN]->(VforVendetta),
    (JamesM)-[:DIRECTED]->(VforVendetta),
    (LillyW)-[:PRODUCED]->(VforVendetta),
    (LanaW)-[:PRODUCED]->(VforVendetta),
    (JoelS)-[:PRODUCED]->(VforVendetta),
    (LillyW)-[:WROTE]->(VforVendetta),
    (LanaW)-[:WROTE]->(VforVendetta)

CREATE (SpeedRacer:Movie {title:'Speed Racer', released:2008, tagline:'Speed has no limits'})
CREATE (EmileH:Person {name:'Emile Hirsch', born:1985})
CREATE (JohnG:Person {name:'John Goodman', born:1960})
CREATE (SusanS:Person {name:'Susan Sarandon', born:1946})
CREATE (MatthewF:Person {name:'Matthew Fox', born:1966})
CREATE (ChristinaR:Person {name:'Christina Ricci', born:1980})
CREATE (Rain:Person {name:'Rain', born:1982})
CREATE
    (EmileH)-[:ACTED_IN]->(SpeedRacer),
    (JohnG)-[:ACTED_IN]->(SpeedRacer),
    (SusanS)-[:ACTED_IN]->(SpeedRacer),
    (MatthewF)-[:ACTED_IN]->(SpeedRacer),
    (ChristinaR)-[:ACTED_IN]->(SpeedRacer),
    (Rain)-[:ACTED_IN]->(SpeedRacer),
    (BenM)-[:ACTED_IN]->(SpeedRacer),
    (LillyW)-[:DIRECTED]->(SpeedRacer),
    (LanaW)-[:DIRECTED]->(SpeedRacer),
    (LillyW)-[:WROTE]->(SpeedRacer),
    (LanaW)-[:WROTE]->(SpeedRacer),
    (JoelS)-[:PRODUCED]->(SpeedRacer)

CREATE (NinjaAssassin:Movie {title:'Ninja Assassin', released:2009, tagline:'Prepare to enter a secret world of assassins'})
CREATE (NaomieH:Person {name:'Naomie Harris'})
CREATE
    (Rain)-[:ACTED_IN]->(NinjaAssassin),
    (NaomieH)-[:ACTED_IN]->(NinjaAssassin),
    (RickY)-[:ACTED_IN]->(NinjaAssassin),
    (BenM)-[:ACTED_IN]->(NinjaAssassin),
    (JamesM)-[:DIRECTED]->(NinjaAssassin),
    (LillyW)-[:PRODUCED]->(NinjaAssassin),
    (LanaW)-[:PRODUCED]->(NinjaAssassin),
    (JoelS)-[:PRODUCED]->(NinjaAssassin)

CREATE (TheGreenMile:Movie {title:'The Green Mile', released:1999, tagline:"Walk a mile you'll never forget."})
CREATE (MichaelD:Person {name:'Michael Clarke Duncan', born:1957})
CREATE (DavidM:Person {name:'David Morse', born:1953})
CREATE (SamR:Person {name:'Sam Rockwell', born:1968})
CREATE (GaryS:Person {name:'Gary Sinise', born:1955})
CREATE (PatriciaC:Person {name:'Patricia Clarkson', born:1959})
CREATE (FrankD:Person {name:'Frank Darabont', born:1959})
CREATE
    (TomH)-[:ACTED_IN]->(TheGreenMile),
    (MichaelD)-[:ACTED_IN]->(TheGreenMile),
    (DavidM)-[:ACTED_IN]->(TheGreenMile),
    (BonnieH)-[:ACTED_IN]->(TheGreenMile),
    (JamesC)-[:ACTED_IN]->(TheGreenMile),
    (SamR)-[:ACTED_IN]->(TheGreenMile),
    (GaryS)-[:ACTED_IN]->(TheGreenMile),
    (PatriciaC)-[:ACTED_IN]->(TheGreenMile),
    (FrankD)-[:DIRECTED]->(TheGreenMile)

CREATE (FrostNixon:Movie {title:'Frost/Nixon', released:2008, tagline:'400 million people were waiting for the truth.'})
CREATE (FrankL:Person {name:'Frank Langella', born:1938})
CREATE (MichaelS:Person {name:'Michael Sheen', born:1969})
CREATE (OliverP:Person {name:'Oliver Platt', born:1960})
CREATE
    (FrankL)-[:ACTED_IN]->(FrostNixon),
    (MichaelS)-[:ACTED_IN]->(FrostNixon),
    (KevinB)-[:ACTED_IN]->(FrostNixon),
    (OliverP)-[:ACTED_IN]->(FrostNixon),
    (SamR)-[:ACTED_IN]->(FrostNixon),
    (RonH)-[:DIRECTED]->(FrostNixon)

CREATE (Hoffa:Movie {title:'Hoffa', released:1992, tagline:"He didn't want law. He wanted justice."})
CREATE (DannyD:Person {name:'Danny DeVito', born:1944})
CREATE (JohnR:Person {name:'John C. Reilly', born:1965})
CREATE
    (JackN)-[:ACTED_IN]->(Hoffa),
    (DannyD)-[:ACTED_IN]->(Hoffa),
    (JTW)-[:ACTED_IN]->(Hoffa),
    (JohnR)-[:ACTED_IN]->(Hoffa),
    (DannyD)-[:DIRECTED]->(Hoffa)

CREATE (Apollo13:Movie {title:'Apollo 13', released:1995, tagline:'Houston, we have a problem.'})
CREATE (EdH:Person {name:'Ed Harris', born:1950})
CREATE (BillPax:Person {name:'Bill Paxton', born:1955})
CREATE
    (TomH)-[:ACTED_IN]->(Apollo13),
    (KevinB)-[:ACTED_IN]->(Apollo13),
    (EdH)-[:ACTED_IN]->(Apollo13),
    (BillPax)-[:ACTED_IN]->(Apollo13),
    (GaryS)-[:ACTED_IN]->(Apollo13),
    (RonH)-[:DIRECTED]->(Apollo13)

CREATE (Twister:Movie {title:'Twister', released:1996, tagline:"Don't Breathe. Don't Look Back."})
CREATE (PhilipH:Person {name:'Philip Seymour Hoffman', born:1967})
CREATE (JanB:Person {name:'Jan de Bont', born:1943})
CREATE
    (BillPax)-[:ACTED_IN]->(Twister),
    (HelenH)-[:ACTED_IN]->(Twister),
    (ZachG)-[:ACTED_IN]->(Twister),
    (PhilipH)-[:ACTED_IN]->(Twister),
    (JanB)-[:DIRECTED]->(Twister)

CREATE (CastAway:Movie {title:'Cast Away', released:2000, tagline:'At the edge of the world, his journey begins.'})
CREATE (RobertZ:Person {name:'Robert Zemeckis', born:1951})
CREATE
    (TomH)-[:ACTED_IN]->(CastAway),
    (HelenH)-[:ACTED_IN]->(CastAway),
    (RobertZ)-[:DIRECTED]->(CastAway)

CREATE (OneFlewOvertheCuckoosNest:Movie {title:"One Flew Over the Cuckoo's Nest", released:1975, tagline:"If he's crazy, what does that make you?"})
CREATE (MilosF:Person {name:'Milos Forman', born:1932})
CREATE
    (JackN)-[:ACTED_IN]->(OneFlewOvertheCuckoosNest),
    (DannyD)-[:ACTED_IN]->(OneFlewOvertheCuckoosNest),
    (MilosF)-[:DIRECTED]->(OneFlewOvertheCuckoosNest)

CREATE (SomethingsGottaGive:Movie {title:"Something's Gotta Give", released:2003})
CREATE (DianeK:Person {name:'Diane Keaton', born:1946})
CREATE (NancyM:Person {name:'Nancy Meyers', born:1949})
CREATE
    (JackN)-[:ACTED_IN]->(SomethingsGottaGive),
    (DianeK)-[:ACTED_IN]->(SomethingsGottaGive),
    (Keanu)-[:ACTED_IN]->(SomethingsGottaGive),
    (NancyM)-[:DIRECTED]->(SomethingsGottaGive),
    (NancyM)-[:PRODUCED]->(SomethingsGottaGive),
    (NancyM)-[:WROTE]->(SomethingsGottaGive)

CREATE (BicentennialMan:Movie {title:'Bicentennial Man', released:1999, tagline:"One robot's 200 year journey to become an ordinary man."})
CREATE (ChrisC:Person {name:'Chris Columbus', born:1958})
CREATE
    (Robin)-[:ACTED_IN]->(BicentennialMan),
    (OliverP)-[:ACTED_IN]->(BicentennialMan),
    (ChrisC)-[:DIRECTED]->(BicentennialMan)

CREATE (CharlieWilsonsWar:Movie {title:"Charlie Wilson's War", released:2007, tagline:"A stiff drink. A little mascara. A lot of nerve. Who said they couldn't bring down the Soviet empire."})
CREATE (JuliaR:Person {name:'Julia Roberts', born:1967})
CREATE
    (TomH)-[:ACTED_IN]->(CharlieWilsonsWar),
    (JuliaR)-[:ACTED_IN]->(CharlieWilsonsWar),
    (PhilipH)-[:ACTED_IN]->(CharlieWilsonsWar),
    (MikeN)-[:DIRECTED]->(CharlieWilsonsWar)

CREATE (ThePolarExpress:Movie {title:'The Polar Express', released:2004, tagline:'This Holiday Season… Believe'})
CREATE
    (TomH)-[:ACTED_IN]->(ThePolarExpress),
    (RobertZ)-[:DIRECTED]->(ThePolarExpress)

CREATE (ALeagueofTheirOwn:Movie {title:'A League of Their Own', released:1992, tagline:'Once in a lifetime you get a chance to do something different.'})
CREATE (Madonna:Person {name:'Madonna', born:1954})
CREATE (GeenaD:Person {name:'Geena Davis', born:1956})
CREATE (LoriP:Person {name:'Lori Petty', born:1963})
CREATE (PennyM:Person {name:'Penny Marshall', born:1943})
CREATE
    (TomH)-[:ACTED_IN]->(ALeagueofTheirOwn),
    (GeenaD)-[:ACTED_IN]->(ALeagueofTheirOwn),
    (LoriP)-[:ACTED_IN]->(ALeagueofTheirOwn),
    (RosieO)-[:ACTED_IN]->(ALeagueofTheirOwn),
    (Madonna)-[:ACTED_IN]->(ALeagueofTheirOwn),
    (BillPax)-[:ACTED_IN]->(ALeagueofTheirOwn),
    (PennyM)-[:DIRECTED]->(ALeagueofTheirOwn)

CREATE (PaulBlythe:Person {name:'Paul Blythe'})
CREATE (AngelaScope:Person {name:'Angela Scope'})
CREATE (JessicaThompson:Person {name:'Jessica Thompson'})
CREATE (JamesThompson:Person {name:'James Thompson'})

CREATE
    (JamesThompson)-[:FOLLOWS]->(JessicaThompson),
    (AngelaScope)-[:FOLLOWS]->(JessicaThompson),
    (PaulBlythe)-[:FOLLOWS]->(AngelaScope)

CREATE
    (JessicaThompson)-[:REVIEWED {summary:'An amazing journey', rating:95}]->(CloudAtlas),
    (JessicaThompson)-[:REVIEWED {summary:'Silly, but fun', rating:65}]->(TheReplacements),
    (JamesThompson)-[:REVIEWED {summary:'The coolest football movie ever', rating:100}]->(TheReplacements),
    (AngelaScope)-[:REVIEWED {summary:'Pretty funny at times', rating:62}]->(TheReplacements),
    (JessicaThompson)-[:REVIEWED {summary:'Dark, but compelling', rating:85}]->(Unforgiven),
    (JessicaThompson)-[:REVIEWED {summary:"Slapstick redeemed only by the Robin Williams and Gene Hackman's stellar performances", rating:45}]->(TheBirdcage),
    (JessicaThompson)-[:REVIEWED {summary:'A solid romp', rating:68}]->(TheDaVinciCode),
    (JamesThompson)-[:REVIEWED {summary:'Fun, but a little far fetched', rating:65}]->(TheDaVinciCode),
    (JessicaThompson)-[:REVIEWED {summary:'You had me at Jerry', rating:92}]->(JerryMaguire)
;
