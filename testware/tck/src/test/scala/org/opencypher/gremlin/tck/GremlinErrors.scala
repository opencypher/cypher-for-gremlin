/*
 * Copyright (c) 2018 "Neo4j, Inc." [https://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opencypher.gremlin.tck

import org.opencypher.tools.tck.api.ExecutionFailed
import org.opencypher.tools.tck.constants.TCKErrorDetails._
import org.opencypher.tools.tck.constants.TCKErrorPhases._
import org.opencypher.tools.tck.constants.TCKErrorTypes._

object GremlinErrors {
  val mappings = Map(
    "Invalid input '-(.+)' is not a valid value, must be a positive integer .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, NEGATIVE_INTEGER_ARGUMENT),
    "Type mismatch: expected (.+) but was (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_ARGUMENT_TYPE),
    "Invalid input '(.*\\..+)' is not a valid value, must be a positive integer.+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_ARGUMENT_TYPE),
    "DELETE doesn't support (.+)\\. Try (.+)\\. .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_DELETE),
    "Type mismatch: (.+) defined with conflicting type (.+) \\(expected (.+)\\) .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_TYPE_CONFLICT),
    "Variable length (.+) cannot be used in (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, CREATING_VAR_LENGTH),
    ".+ relationship type must be specified for (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, NO_SINGLE_RELATIONSHIP_TYPE),
    "Cannot use the same relationship variable '(.+)' for multiple patterns .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, RELATIONSHIP_UNIQUENESS_VIOLATION),
    "It is not allowed to refer to variables in (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, NON_CONSTANT_EXPRESSION),
    "Parameter (.+) cannot be used in (.+) \\(use (.+) instead.+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_PARAMETER_USE),
    "Argument to (.+) is not a property or pattern .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_ARGUMENT_EXPRESSION),
    "Expression in (.+) must be aliased \\(use AS\\) .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, NO_EXPRESSION_ALIAS),
    "Invalid input '(.+)': expected four hexadecimal digits specifying a unicode character .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_UNICODE_LITERAL),
    "Invalid input '(.+)': expected whitespace, comment, '.', node labels, '\\[', (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_UNICODE_CHARACTER),
    "Invalid input '(.+)': expected an identifier character, whitespace, '\\|', a length specification, a property map or '\\]'.+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_RELATIONSHIP_PATTERN),
    "Invalid input '(.+)': expected whitespace, RangeLiteral, a property map or '\\]'.+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_RELATIONSHIP_PATTERN),
    "All sub queries in an UNION must have the same column names .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, DIFFERENT_COLUMNS_IN_UNION),
    "(.+) is not allowed when there are no variables in scope .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, NO_VARIABLES_IN_SCOPE),
    "Multiple result columns with the same name are not supported .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, COLUMN_NAME_CONFLICT),
    "Can't create node '(.+)' with labels or properties here. The variable is already declared in this context" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_ALREADY_BOUND),
    "Can't create node `(.+)` with labels or properties here. It already exists in this context" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_ALREADY_BOUND),
    "Can't create `(.+)` with properties or labels here. The variable is already declared in this context" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_ALREADY_BOUND),
    "Can't create `(.+)` with properties or labels here. It already exists in this context" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_ALREADY_BOUND),
    "Can't create `(.+)` with labels or properties here. It already exists in this context" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_ALREADY_BOUND),
    "(.+) already declared" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_ALREADY_BOUND),
    "Variable `(.+)` already declared .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, VARIABLE_ALREADY_BOUND),
    "Invalid use of aggregating function (.+) in this context .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_AGGREGATION),
    "Cannot use aggregation in (.+) if there are no aggregate expressions in the preceding (.+) .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_AGGREGATION),
    "Can't use aggregating expressions inside of expressions executing over lists .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_AGGREGATION),
    "Variable `(.+)` not defined .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, UNDEFINED_VARIABLE),
    "floating point number is too (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, FLOATING_POINT_OVERFLOW),
    "Invalid combination of (.+) and (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_CLAUSE_COMPOSITION),
    "(.+) cannot follow (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_CLAUSE_COMPOSITION),
    "Only directed relationships are supported in CREATE .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, REQUIRES_DIRECTED_RELATIONSHIP),
    "invalid literal number .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_NUMBER_LITERAL),
    "Unknown function (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, UNKNOWN_FUNCTION),
    "Unsupported graph element type: (.+)" ->
      ExecutionFailed(SYNTAX_ERROR, RUNTIME, PROPERTY_ACCESS_ON_NON_MAP),
    "Procedure not found: .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, PROCEDURE_NOT_FOUND),
    "In-query call with implicit arguments: .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_ARGUMENT_PASSING_MODE),
    "Parameter .+ missing for procedure .+" ->
      ExecutionFailed(PARAMETER_MISSING, COMPILE_TIME, MISSING_PARAMETER),
    "Invalid number of arguments for .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_NUMBER_OF_ARGUMENTS),
    "Invalid argument types for .+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_ARGUMENT_TYPE),
    "Procedure call cannot take an aggregating function as argument.+" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, INVALID_AGGREGATION),
    "Cannot convert .+ to .+" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, INVALID_ARGUMENT_VALUE),
    ".+ cannot be cast to .+" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, INVALID_ARGUMENT_VALUE),
    "Number out of range: (.+)" ->
      ExecutionFailed(ARGUMENT_ERROR, RUNTIME, NUMBER_OUT_OF_RANGE),
    "Step argument to range\\(\\) cannot be zero" ->
      ExecutionFailed(ARGUMENT_ERROR, RUNTIME, NUMBER_OUT_OF_RANGE),
    "Unable to convert param (.+)" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, INVALID_ARGUMENT_TYPE),
    "Unable to convert result (.+)" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, INVALID_ARGUMENT_TYPE),
    "List element access by non-integer: .+" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, LIST_ELEMENT_ACCESS_BY_NON_INTEGER),
    "Map element access by non-string: .+" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, MAP_ELEMENT_ACCESS_BY_NON_STRING),
    "Invalid property access of .+" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, PROPERTY_ACCESS_ON_NON_MAP),
    "Invalid element access of .+" ->
      ExecutionFailed(TYPE_ERROR, RUNTIME, INVALID_ELEMENT_ACCESS),
    ".*Cannot delete node, because it still has relationships.+" ->
      ExecutionFailed(SEMANTIC_ERROR, RUNTIME, DELETE_CONNECTED_NODE),
    "Can't use aggregate functions inside of aggregate functions" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, NESTED_AGGREGATION),
    "Can't use non-deterministic \\(random\\) functions inside of aggregate functions" ->
      ExecutionFailed(SYNTAX_ERROR, COMPILE_TIME, NON_CONSTANT_EXPRESSION),
    "Deleted entity (.+) access (.+)" ->
      ExecutionFailed(ENTITY_NOT_FOUND, RUNTIME, DELETED_ENTITY_ACCESS)
  )
}
