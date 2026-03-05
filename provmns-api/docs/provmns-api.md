# provmns-api

Generated Java models from the [3GPP TS28532 ProvMnS OpenAPI spec](https://forge.3gpp.org/rep/sa5/MnS).

## Why this module exists

Java models generated from the 3GPP TS28532 ProvMnS OpenAPI spec. A pre-built JAR is committed to `lib/` and installed into the local Maven repository during the normal build. Code generation only runs when explicitly activating the `generate-provmns-api` profile.

## Module structure

- `lib/provmns-api-<version>.jar` — pre-built JAR of generated model classes, committed to source control
- `src/main/java/` — two hand-written source files that complement the generated models:
  - `Resource.java` — replaces the generated `Resource` class to avoid pulling in NRM-related model dependencies
  - `ClassNameIdGetDataNodeSelectorParameter.java` — hand-written model not present in the generated sources

During the normal build, antrun merges the lib JAR with the two compiled hand-written classes into
the final JAR, with the hand-written classes taking precedence.

## Using the JAR

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>org.onap.cps</groupId>
    <artifactId>provmns-api</artifactId>
</dependency>
```

Version is managed by `cps-bom`.

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
2. Update `provmns.spec.version` in `provmns-api/pom.xml` to match the new spec version.
3. Build the JAR (still using the old lib JAR at this point):

```bash
mvn install -pl provmns-api
```

4. Copy the built JAR to `lib/`, update source control, and update the lib JAR filename in `provmns-api/pom.xml` (`<finalName>`, `systemPath`, and antrun `zipfileset src`):

```bash
cp provmns-api/target/provmns-api-<new-version>.jar provmns-api/lib/
git add -f provmns-api/lib/provmns-api-<new-version>.jar
git rm provmns-api/lib/provmns-api-<old-version>.jar
```

5. Run `mvn install -pl provmns-api` once more to verify the build works with the new lib JAR.
