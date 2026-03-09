# Java Code Quality Skill

## Overview
This skill ensures Java code follows CPS (Configuration Persistence Service) quality standards and best practices.

## Core Principle
**Simple is Good, Complex is Bad** - Always prefer simple, readable code over complex patterns. When choosing between solutions, select the one that is easiest to understand and maintain.

## Code Quality Checks

### 1. Naming Conventions
- Use consistent naming throughout the codebase
- Don't shorten instance names:
  ```java
  // Bad: CmHandleState state = new CmHandleState(..);
  // Good: CmHandleState cmHandleState = new CmHandleState(..);
  ```
- Variable names should match across method calls:
  ```java
  // Good:
  int orderNumber = 123;
  String status = getStatus(orderNumber);
  String getStatus(int orderNumber) { ... }
  ```

### 2. String Constants
- Avoid overuse of constants for simple string literals
- Don't create constants just to avoid duplication - consider extracting a method instead:
  ```java
  // Bad:
  final static String HELLO = "Hello ";
  String message = HELLO + name;

  // Good:
  String message = "Hello " + name;
  // Or better:
  String sayHello(final String name) {
      return "Hello " + name;
  }
  ```

### 3. Null Safety
- Put constants first in equals() to prevent NPEs:
  ```java
  // Bad: if (xpath.equals(ROOT_NODE_PATH) {..}
  // Good: if (ROOT_NODE_PATH.equals(xpath) {..}
  ```

### 4. Control Flow
- Avoid using `!` (negation) when else block is implemented - invert the condition:
  ```java
  // Bad:
  if (x != null) {
      do something;
  } else {
      report error;
  }

  // Good:
  if (x == null) {
      report error;
  } else {
      do something;
  }
  ```

- No need for else after return:
  ```java
  // Bad:
  if (ROOT_NODE_PATH.equals(xpath) {
      return something;
  } else {
      return somethingElse;
  }

  // Good:
  if (x == true) {
      return something;
  }
  return something-else;
  ```

### 5. Collections
- No need to check isEmpty before iterating:
  ```java
  // Bad:
  if (!myCollection.isEmpty()) {
      collection.forEach(some action);
  }

  // Good:
  collection.forEach(some action);
  ```

- Initialize collections/arrays with known size:
  ```java
  // Bad:
  void processNames(Collection<String> orginalNames) {
      String[] processedNames = new String[0];

  // Good:
  void processNames(Collection<String> orginalNames) {
      String[] processedNames = new String[orginalNames.size()];
  ```

### 6. Performance
- Use string concatenation instead of String.format (5x slower) where possible:
  ```java
  // Bad: String.format("cm-handle:%s", cmHandleId);
  // Good: "cm-handle:" + cmHandleId;
  ```

### 7. Spring Annotations
- Use `@Service` when a class includes operations
- Use `@Component` when it is a data object only
- Currently no functional difference but may change in future Spring versions

### 8. Prefer Simplicity Over Optional Patterns
- Avoid complex Optional chains when simple null checks are clearer:
  ```java
  // Bad (complex):
  Optional<String> optionalResponseBody =
      Optional.ofNullable(responseEntity.getBody())
          .filter(Predicate.not(String::isBlank));
  return (optionalResponseBody.isPresent()) ?
      convert(optionalResponseBody.get()) : Collections.emptyList();

  // Good (simple):
  String responseBody = responseEntity.getBody();
  if (responseBody == null || responseBody.isBlank()) {
      return Collections.emptyList();
  }
  return convert(responseBody);
  ```

## Security Checks

### Never Log User Data
- Do not log any user data at any log level
- User data may contain sensitive information
- When logging objects, ensure toString() implementation doesn't include user data
- Only log well-defined fields that do not contain user data

## Commit Guidelines
Follow the [ONAP commit message guidelines](https://wiki.onap.org/display/DW/Commit+Message+Guidelines)

## Application
When reviewing or writing Java code:
1. Check for these patterns in new/modified code
2. Suggest simpler alternatives when complex code is found
3. Ensure security checks (especially logging) are followed
4. Verify naming conventions are consistent
5. Look for performance anti-patterns (String.format, uninitialized collections)
