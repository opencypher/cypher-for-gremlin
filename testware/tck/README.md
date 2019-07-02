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

### Tools

Repository contains tools that extends [openCypher TCK](https://github.com/opencypher/openCypher/tree/master/tools/tck-api) functionality: 

* [TCK Regression](#tck-regression-report) compares two JUnit5 reports and generates html diff
* [TCK Cucumber Report](#tck-cucumber-report) allows to generate Cucumber reports for openCypher tck-api


#### TCK Regression Report

Compares two JUnit 5 reports and generates html diff.

To compare TCK results with some baseline TCK run use these tasks:

* `tckSaveReport` to save the current report to `build/test-results/tck/TckTest-before.xml`:
  ```groovy
    task('tckSaveReport', type: Copy) {
        from 'build/test-results/tck/TEST-org.opencypher.gremlin.tck.TckTest.xml'
        into 'build/test-results/tck/'
        rename { fileName -> "TckTest-before.xml" }
    }
  ```
* `tckRegression`([TckResultsComparator](src/test/java/org/opencypher/gremlin/tck/regression/TckResultsComparator.java)) to compare `TckTest-before.xml` and the current report `TEST-org.opencypher.gremlin.tck.TckTest.xml`:
  ```groovy
    task('tckRegression', type: JavaExec) {
        main = 'org.opencypher.gremlin.tck.regression.TckResultsComparator'
        classpath = sourceSets.test.runtimeClasspath
    }
  ```
  - Html report will be created in `testware/tck/build/reports/tests/regression.html`

The usual workflow is:

1. Start with a stable state
1. `./gradlew --continue clean tck tckSaveReport`
1. Make changes
1. `./gradlew --continue tck tckRegression`
  - The `clean` target is not used here because it will remove `TEST-junit-jupiter-before.xml`, which is required for `tckRegression`.

#### TCK Cucumber Report

Creates a [better Cucumber report](https://github.com/damianszczepanik/cucumber-reporting) from generated json file. See [example](https://opencypher.github.io/cypher-for-gremlin/test-reports/0.9.11/cucumber-html-reports/overview-features.html).

1. Annotate TCK test class with `org.junit.jupiter.api.extension.ExtendWith`:

  ```scala
  @ExtendWith(Array(classOf[CucumberReportAdapter]))
  class TckTest {
  
    @TestFactory
    def testStandardTCK(): util.Collection[DynamicTest] = {
      //...
    }
    
    //...
  ```

2. After TCK execution, run [OpenCypherTckReport](src/test/java/org/opencypher/gremlin/tck/reports/OpenCypherTckReport.java). 
  For example, Gradle task:

  ```groovy
  task('tckReport', type: JavaExec) {
      main = 'org.opencypher.gremlin.tck.reports.OpenCypherTckReport'
      classpath = sourceSets.test.runtimeClasspath
  }
  ```    

By default `cucumber.json` report file is created in a working directory. To configure report format and location use `-Dcucumber.options="--plugin PLUGIN[:PATH_OR_URL]"`

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
