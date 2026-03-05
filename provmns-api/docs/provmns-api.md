# provmns-api

Generated Java interfaces and models from the [3GPP TS28532 ProvMnS OpenAPI spec](https://forge.3gpp.org/rep/sa5/MnS).

## Why this module exists

The ProvMnS OpenAPI spec is hosted externally. Previously every module that needed the generated
models had to duplicate the `openapi-generator-maven-plugin` configuration and re-download the spec
on every build. This module centralises generation into one place and publishes a versioned JAR to
Nexus. Consumers simply declare a dependency — no generation, no network call, no build overhead.

This module is **not listed in the root aggregator `pom.xml`** so it is never built as part of
`mvn install` on the main project.

## Using the JAR

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.onap.cps</groupId>
    <artifactId>provmns-api</artifactId>
</dependency>
```

Version is managed by `cps-bom`.

## Regenerating sources (only when the spec changes)

Code generation is **not** part of the normal build. Run it manually when the upstream spec version changes:

```bash
# From the repo root
mvn generate-sources -Pgenerate-provmns-api -pl provmns-api
```

This downloads the spec from `forge.3gpp.org` and writes sources into `src/main/java/`.

After generation:
1. Review the diff — check `.openapi-generator-ignore` if new models need to be included or excluded.
2. Update `provmns.spec.version` in `provmns-api/pom.xml` to match the spec tag used.
3. Commit the generated sources and the updated `pom.xml`.
4. Build and install the JAR:

```bash
mvn install -pl provmns-api
```


