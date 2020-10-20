# Configuration & Persistency Service

This folder contains all files for
[Configuration & Persistency Service](https://wiki.onap.org/pages/viewpage.action?pageId=81406119).

The code here is related to CPS POC, then it must be kept self contained in this cps folder to prevent any impact on
current ccsdk components and to be ready to be moved in its own repo once CPS becomes a standalone project.


## Running Locally

* Run a postgres container instance and create `cpsdb' database:

```
CREATE USER cps WITH PASSWORD 'cps';
CREATE DATABASE cpsdb OWNER cps;
```

* Build (from cps root folder)

```bash
mvn clean package
```

* Run (from cps root folder)

```bash
java -DDB_HOST=localhost -DDB_USERNAME=cps -DDB_PASSWORD=cps -jar cps-rest/target/cps-rest-0.0.1-SNAPSHOT.jar
```

* Browse
  * [Swagger UI](http://localhost:8080/swagger-ui/index.html)
  * OpenAPI Specification in [JSON](http://localhost:8080/api/cps/openapi.json)
   or [YAML](http://localhost:8080/api/cps/openapi.yaml) format
