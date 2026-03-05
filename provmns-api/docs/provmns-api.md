# provmns-api

Generated Java models from the [3GPP TS28532 ProvMnS OpenAPI spec](https://forge.3gpp.org/rep/sa5/MnS).

## Why this module exists

The ProvMnS OpenAPI spec is hosted externally. This module centralises generation into one place
and publishes a versioned JAR to Nexus. Consumers simply declare a dependency — no generation,
no network call, no build overhead.

This module is **not listed in the root aggregator `pom.xml`** so it is never built as part of
the normal `mvn install`.

## Using the JAR

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.onap.cps</groupId>
    <artifactId>provmns-api</artifactId>
</dependency>
```

Version is managed by `cps-bom`.

A pre-built JAR is committed to `provmns-api/lib/` and used as a file-system dependency so the
build works without running the generator or fetching from Nexus.

## Regenerating (only when the spec changes)

Sources are generated into `target/` and are **never committed**. Run manually when the upstream
spec version changes:

```bash
# From the repo root
mvn generate-sources -Pgenerate-provmns-api -pl provmns-api
```

This downloads the spec from `forge.3gpp.org` and writes sources into
`target/generated-sources/openapi/`.

After generation:
1. Review the diff — update `.openapi-generator-ignore` if new models need to be included or excluded.
2. Update `provmns.spec.version` in `provmns-api/pom.xml` to match the new spec tag.
3. Build and install the JAR:

```bash
mvn install -pl provmns-api
```

4. Copy the new JAR to `lib/` and force-add it (root `.gitignore` excludes `*.jar`):

```bash
cp target/provmns-api-<version>.jar lib/
git add -f lib/provmns-api-<version>.jar
git rm lib/provmns-api-<old-version>.jar
```

5. Update the `<version>` in `provmns-api/pom.xml` and the `systemPath` in `cps-bom/pom.xml`.
6. Commit the updated `pom.xml` files and the new JAR. Do **not** commit any `.java` sources.
