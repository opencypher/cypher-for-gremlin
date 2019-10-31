## openCypher TCK

This module uses [openCypher TCK Tools](https://github.com/opencypher/openCypher/tree/master/tools/tck-api) to verify translated queries. It executes openCypher queries against a Gremlin Server instance with TinkerGraph as backend.

To execute the TCK, from project root:

```
./gradlew tck
``` 

It is expected for some tests for fail, but the report will give you an overview of supported Cypher features.

### Options

To execute single scenario:

```
./gradlew tck -Dfeature="featureName" -Dscenario="scenarioName"
```

To execute single feature:

```
./gradlew tck -Dfeature="featureName"
``` 

To exclude scenarios listed in csv file (csv should contain columns `feature` and `scenario`)

```
./gradlew tck -DignoreFile="/path/to/file.csv"
```

#### Reporting and Regression

Project uses [TCK Reporting](https://github.com/opencypher/openCypher/tree/master/tools/tck-reporting).

For TCK regression use [TCK Report Comparison](https://github.com/opencypher/openCypher/tree/master/tools/tck-report-comparison)

```
./gradlew tck tckRegression
```

Build will fail if TCK does not match [expected TCK failures](src/test/resources/tck-failing.csv) list.
Developer should update this file before committing changes that affect TCK.

### Predefined procedures

To test Gremlin Server with TCK Predefined procedures:

```bash
gradle testware:tck:testJar
cp testware/tck/build/libs/*.jar /path/to/gremlin-server/lib
```

In server config `.yaml` add `TckPredefinedProceduresPlugin`:

```yaml
scriptEngines: {
  gremlin-groovy: {
    plugins: { 
      org.opencypher.gremlin.traversal.TckPredefinedProceduresPlugin: {}
    //...
```  
