# Spock/Groovy Testing Guide for CPS-NCMP

This document describes important aspects of our Spock/Groovy test framework, conventions, and best practices observed in the CPS-NCMP codebase.

## Framework and Tools

### Core Testing Framework
- **Spock Framework**: Behavior-driven development (BDD) testing framework for Java and Groovy applications
- **Groovy**: Dynamic JVM language used for writing expressive and readable tests
- **Specification Base Class**: All test classes extend `spock.lang.Specification`

### Additional Libraries
- **Mockito/Spock Mocks**: For creating mock objects and stubbing behavior
- **Logback**: For testing logging behavior (using `ListAppender`)
- **Hazelcast**: For testing distributed cache functionality
- **Reactor**: For testing reactive/asynchronous operations (e.g., `Mono`, `Flux`)

## Naming Conventions

### Test Class Names
- All test classes end with `Spec` suffix
- Example: `DataJobServiceImplSpec`, `TrustLevelManagerSpec`, `NetworkCmProxyFacadeSpec`

### Test Method Names
- Use descriptive sentences in single quotes
- Follow pattern: `def 'Description of what is being tested'()`
- **Do NOT include expectations in the test name** (e.g., "throws exception", "returns true")
- Use `then:` blocks with descriptions to express expectations instead
- Examples:
  - ✅ `def 'Registration with invalid cm handle name.'()`
  - ❌ `def 'Registration of invalid cm handle throws exception.'()`
  - ✅ `def 'Read data job request.'()`
  - ✅ `def 'DMI Registration: Create, Update, Delete & Upgrade operations are processed in the right order'()`
  - ✅ `def 'Initial cm handle registration with a cm handle that is not trusted'()`

### Variable Naming
- Mock objects: Prefix with `mock` (e.g., `mockInventoryPersistence`, `mockDmiSubJobRequestHandler`)
- Object under test: **Always use** `objectUnderTest` (not custom names like `parameterMapper` or class-specific names)
- Test result: **Always use** `result` for the outcome of method calls
- Test data: Use meaningful prefixes like `my` or `some` for values that don't affect the test (e.g., `'my user'`, `'some scope'`)
- Important test values: Use descriptive names without generic prefixes to distinguish them from arbitrary values

## Test Structure

### Standard Spock Blocks

Tests use labeled blocks for clarity and readability:

```groovy
def 'Test description'() {
    given: 'context setup description'
        // Setup code
    and: 'additional context'
        // More setup
    when: 'action being tested'
        // Execute the method under test
    then: 'expected outcome'
        // Assertions and verifications
    and: 'additional expectations'
        // More assertions
}
```

### Common Blocks
- **given**: Setup test data and preconditions
- **and**: Additional setup or assertions (improves readability)
- **when**: Execute the code under test
- **then**: Assert expected outcomes and verify mock interactions
- **where**: Define parameterized test data (data-driven tests)
- **expect**: Combined when+then for simple tests
- **setup**: Instance-level setup (runs before each test)
- **cleanup**: Instance-level teardown (runs after each test)

## Test Patterns

### 1. Object Under Test Initialization

Pattern with constructor injection and mocks:
```groovy
class DataJobServiceImplSpec extends Specification {
    def mockWriteRequestExaminer = Mock(WriteRequestExaminer)
    def mockDmiSubJobRequestHandler = Mock(DmiSubJobRequestHandler)

    def objectUnderTest = new DataJobServiceImpl(
        mockDmiSubJobRequestHandler,
        mockWriteRequestExaminer,
        mockJsonObjectMapper
    )
}
```

Alternative with Spy for partial mocking:
```groovy
def objectUnderTest = Spy(new CmHandleRegistrationService(
    mockPropertyHandler,
    mockInventoryPersistence,
    ...
))
```

### 2. Mock Interactions

**Stubbing return values:**
```groovy
mockInventoryPersistence.getYangModelCmHandle('ch-1') >> new YangModelCmHandle(id: 'ch-1')
```

**Verifying method calls:**
```groovy
1 * mockInventoryEventProducer.sendAvcEvent('ch-1', 'trustLevel', 'NONE', 'COMPLETE')
0 * mockInventoryEventProducer.sendAvcEvent(*_)  // No calls expected
```

**Argument matching:**
```groovy
1 * mockWriteRequestExaminer.splitDmiWriteOperationsFromRequest('my-job-id', dataJobWriteRequest)
mockAlternateIdChecker.getIdsOfCmHandlesWithRejectedAlternateId(*_) >> []  // Any arguments
```

### 3. Parameterized Tests (Data-Driven)

Use `where` block for data tables:
```groovy
def 'Determining cloud event using ce_type header for a #scenario.'() {
    given: 'headers contain a header for key: #key'
        headers.lastHeader(key) >> header
    expect: 'the check for cloud events returns #expectedResult'
        assert objectUnderTest.isCloudEvent(headers) == expectedResult
    where: 'the following headers (keys) are defined'
        scenario          | key       || expectedResult
        'cloud event'     | 'ce_type' || true
        'non-cloud event' | 'other'   || false
}
```

**Conventions:**
- Use `#variableName` in test name to include parameter values in test output
- Use `|` to separate input columns, `||` before expected output columns
- Variables from `where` block are accessible throughout the test

### 4. Setup and Cleanup

```groovy
def setup() {
    setupLogger(Level.DEBUG)
    mockAlternateIdChecker.getIdsOfCmHandlesWithRejectedAlternateId(*_) >> []
}

def cleanup() {
    ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class)).detachAndStopAllAppenders()
    hazelcastInstance.shutdown()
}
```

### 5. Assertion Patterns

**Groovy assert (preferred):**
```groovy
assert result.statusCode == HttpStatus.OK
assert trustLevelPerCmHandleId.get('ch-1') == TrustLevel.COMPLETE
```

**Spock with block for complex assertions:**
```groovy
with(logger.list.find { it.level == Level.DEBUG }) {
    assert it.formattedMessage.contains("Initiating WRITE operation")
}
```

**JUnit-style assertions (less common):**
```groovy
assertEquals(expected, actual)
assertTrue(condition)
```

### 6. Testing Logging Behavior

```groovy
def logger = Spy(ListAppender<ILoggingEvent>)

def setup() {
    def setupLogger = ((Logger) LoggerFactory.getLogger(DataJobServiceImpl.class))
    setupLogger.setLevel(Level.DEBUG)
    setupLogger.addAppender(logger)
    logger.start()
}

def 'Test with logging verification'() {
    when: 'method is called'
        objectUnderTest.someMethod()
    then: 'correct log message is generated'
        def loggingEvent = logger.list[0]
        assert loggingEvent.level == Level.INFO
        assert loggingEvent.formattedMessage.contains('Expected message')
}
```

### 7. Testing Reactive Code

```groovy
def 'Get resource data from DMI (delegation).'() {
    given: 'a cm resource address'
        def cmResourceAddress = new CmResourceAddress('ncmp-datastore:operational', 'ch-1', 'resource-id')
    and: 'get resource data returns a Mono'
        mockHandler.executeRequest(cmResourceAddress, 'options', NO_TOPIC, false, 'auth') >>
            Mono.just('dmi response')
    when: 'get resource data is called'
        def response = objectUnderTest.getResourceDataForCmHandle(cmResourceAddress, ...).block()
    then: 'response is correct'
        assert response == 'dmi response'
}
```

## Best Practices

### 1. Descriptive Test Names (Slogans)
- Use natural language that describes the scenario being tested
- **Do NOT include expectations** - use `then:` blocks with descriptions instead
- Include context about what's being tested
- Examples:
  - ✅ `def 'Registration with invalid cm handle name.'()` with `then: 'a validation exception is thrown'`
  - ❌ `def 'Registration of invalid cm handle throws exception.'()`
  - ✅ `def 'Initial cm handle registration with a cm handle that is not trusted'()`
  - ❌ `def 'test1'()`

### 2. Clear Given-When-Then Structure
- Always use labeled blocks with descriptions
- Descriptions should explain the setup/action/expectation
- **Keep descriptions up to date** with the actual test code
- Use `and` blocks to break up complex setups
- Example:
  ```groovy
  given: 'a invalid cm handle name'
      cmHandle.id = 'invalid,name'
  when: 'permission is checked for unauthorized user'
      objectUnderTest.checkPermission(someCmh, 'my user', 'some scope')
  then: 'exception message contains the user id'
      thrown(PermissionException).message.contains('my user')
  ```

### 3. Mock Setup
- Initialize mocks as instance variables
- Set up default mock behavior in `setup()` when applicable
- Keep test-specific mocking in the test method itself

### 4. Assertions
- Prefer Groovy's `assert` over JUnit assertions
- Use specific assertions rather than generic ones
- Verify both positive and negative cases

### 5. Test Data
- **Minimize test data to only what's needed** for the specific test case
- Avoid copying and pasting unnecessary test data from other tests
- Use Groovy Map Constructor to populate only required fields: `new CmHandle(id: 'invalid,name')`
- Use meaningful test data that reflects real scenarios
- Keep test data close to where it's used
- Use constants for commonly reused values (e.g., `NO_TOPIC = null`)
- Clearly distinguish between important values and arbitrary values using prefixes like `my` or `some`

### 6. Mock Verification
- Verify important interactions explicitly
- Use `0 *` to ensure methods are NOT called when expected
- Use `*_` (any arguments) sparingly; prefer specific argument matching

### 7. Parameterized Tests
- Use for testing multiple similar scenarios
- Keep data tables readable (align columns)
- Use descriptive scenario names
- Reference parameters in test descriptions with `#paramName`

## Common Pitfalls to Avoid

1. **Over-mocking**: Don't mock everything; use real objects for simple data classes
2. **Unclear test names**: Avoid generic names like "test1" or "testMethod"
3. **Expectations in test names**: Don't include "throws exception" or "returns true" in test names - use `then:` blocks instead
4. **Wrong variable names**: Always use `objectUnderTest` and `result`, not custom names
5. **Unnecessary test data**: Minimize test data to only what's needed; don't copy/paste from other tests
6. **Unclear test values**: Use `my` or `some` prefixes for arbitrary values to distinguish from important ones
7. **Outdated descriptions**: Keep `given:`, `when:`, `then:` descriptions synchronized with code
8. **Missing block labels**: Always label blocks (given, when, then) with descriptions
9. **Testing implementation details**: Focus on behavior, not internal implementation
10. **Brittle tests**: Avoid over-specifying mock interactions; verify what matters
11. **Not using `where` blocks**: Don't duplicate similar tests; parameterize instead
12. **Ignoring cleanup**: Always clean up resources (loggers, Hazelcast instances, etc.)
13. **Not using Java features**: Avoid unnecessary Java syntax (types, access modifiers, semicolons) in Groovy tests unless required

## Resources

- [Spock Framework Documentation](https://spockframework.org/spock/docs/2.3/all_in_one.html)
- [Groovy Documentation](https://groovy-lang.org/documentation.html)
- CPS-NCMP codebase: `cps-ncmp-service/src/test/groovy/`
