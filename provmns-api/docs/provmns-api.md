# provmns-api

Generated Java models from the [3GPP TS28532 ProvMnS OpenAPI spec](https://forge.3gpp.org/rep/sa5/MnS).

## Why this module exists

Java models generated from the 3GPP TS28532 ProvMnS OpenAPI spec. A pre-built JAR is committed to
`local-repo/` in Maven repository layout and resolved as a `compile` scope dependency. Code generation
only runs when explicitly activating the `generate-provmns-api` profile.

## Module structure

- `local-repo/org/onap/cps/provmns-api/18.6.0/` — pre-built JAR of generated model classes in Maven
  repository layout
- `src/main/java/` — two hand-written source files that complement the generated models:
  - `Resource.java` — replaces the generated `Resource` class to avoid pulling in NRM-related model dependencies
  - `ClassNameIdGetDataNodeSelectorParameter.java` — hand-written model not present in the generated sources

During the normal build, antrun plugin merges the `local-repo` JAR with the two compiled hand-written classes
into the final JAR, with the hand-written classes taking precedence.

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
3. Build the JAR (still using the old `local-repo` JAR at this point):

```bash
mvn install -pl provmns-api
```

4. Install the new JAR into `local-repo/`, remove the old one, and update `provmns.lib.version`
   and `provmns.spec.version` in `provmns-api/pom.xml`:

```bash
mvn install:install-file \
  -Dfile=provmns-api/target/provmns-api-<new-version>.jar \
  -DgroupId=org.onap.cps \
  -DartifactId=provmns-api \
  -Dversion=<new-version> \
  -Dpackaging=jar \
  -DlocalRepositoryPath=provmns-api/local-repo

git add -f provmns-api/local-repo/
git rm -r provmns-api/local-repo/org/onap/cps/provmns-api/<old-version>/
```

5. Run `mvn install -pl provmns-api` once more to verify the build works with the new `local-repo` JAR.
