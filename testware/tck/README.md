## openCypher TCK

This module uses [openCypher TCK Tools](https://github.com/opencypher/openCypher/tree/master/tools/tck-api) to verify translated queries. It executes openCypher queries against a Gremlin Server instance with TinkerGraph as backend.

To execute the TCK, from project root:

```
./gradlew tck
``` 

It is expected for some tests for fail, but the report will give you an overview of supported Cypher features.

### TCK Regression Report

To compare TCK results with some baseline TCK run use these tasks:

* `tckSaveReport` to save the current report to `build/test-results/junit-platform/TEST-junit-jupiter-before.xml`
* `tckRegression` to compare `TEST-junit-jupiter-before.xml` and the current report `TEST-junit-jupiter.xml`

The usual workflow is:

1. Start with a stable state
1. `gradle --continue clean tck tckSaveReport`
1. Make changes
1. `gradle --continue tck tckRegression`
  - The `clean` target is not used here because it will remove `TEST-junit-jupiter-before.xml`, which is required for `tckRegression`.
