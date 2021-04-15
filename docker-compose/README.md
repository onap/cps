# Building and running CPS locally

## Building Java Archive only

Following command builds all Java components to `cps-application/target/cps-application-x.y.z-SNAPSHOT.jar` JAR file
without generating any docker images:  

```bash
mvn clean install -Pcps-docker -Pncmp-docker -Pcps-ncmp-docker -Djib.skip
```

## Building Java Archive and Docker images

* Following command builds the JAR file and also generates the Docker image for all CPS components:

```bash
mvn clean install -Pcps-docker -Pncmp-docker -Pcps-ncmp-docker -Dnexus.repository=
```

* Following command builds the JAR file and generates the Docker image for specified CPS component:
  (with `<docker-profile>` being one of `cps-docker`, `ncmp-docker` or `cps-ncmp-docker`):

```bash
mvn clean install -P<docker-profile> -Dnexus.repository=
```

## Running Docker containers

`docker-compose/docker-compose.yml` file is provided to be run with `docker-compose` tool and images previously built.
It starts both Postgres database and CPS services.

1. Edit `docker-compose.yml` and uncomment desired service to be deployed, by default `cps-and-ncmp`
   is enabled. You can comment it and uncomment `cps-standalone` or `ncmp-standalone`.
2. Execute following command from `docker-compose` folder:

Use one of the below version type that has been generated in the docker image list after the build.
```bash
VERSION=latest DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
or
VERSION=x.y.z DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
or
VERSION=x.y.z-yyyyMMddTHHmmssZ DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
or
VERSION=x.y.z-SNAPSHOT DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
or
VERSION=x.y.z-SNAPSHOT-yyyyMMddTHHmmssZ DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
```

## Running or debugging Java built code

Before running CPS, a Postgres database instance needs to be started. This can be done with following
command:

```bash
docker run --name postgres -p 5432:5432 -d \
  -e POSTGRES_DB=cpsdb -e POSTGRES_USER=cps -e POSTGRES_PASSWORD=cps \
  postgres:12.4-alpine
```

Then CPS can be started either using a Java Archive previously built or directly from Intellij IDE.

### Running from Jar Archive

Following command starts the application using JAR file:

```bash
DB_HOST=localhost DB_USERNAME=cps DB_PASSWORD=cps \
  java -jar cps-application/target/cps-application-x.y.z-SNAPSHOT.jar
```

### Running from IntelliJ IDE

Here are the steps to run or debug the application from Intellij:

1. Enable the desired maven profile form Maven Tool Window
2. Run a configuration from `Run -> Edit configurations` with following settings:
   * `Environment variables`: `DB_HOST=localhost;DB_USERNAME=cps;DB_PASSWORD=cps`

## Accessing services

Swagger UI and Open API specifications are available to discover service endpoints and send requests.

* `http://localhost:<port-number>/swagger-ui/index.html`
* `http://localhost:<port-number>/v3/api-docs?group=cps-docket`

with <port-number> being either `8080` if running the plain Java build or retrieved using following command
if running from `docker-compose`:

```bash
docker inspect \
  --format='{{range $p, $conf := .NetworkSettings.Ports}} {{$p}} -> {{(index $conf 0).HostPort}} {{end}}' \
  <cps-docker-container>
```

Enjoy CPS !
