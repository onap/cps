# Running CPS on local environment

### Maven profiles description

The profiles below require to be enabled in order to access the desired functionality.
Docker images are produced using local docker on maven `package` stage.

- `cps-docker` - CPS only REST endpoints exposed, `cps-service` docker image
- `xnf-docker` - xNF Proxy only REST endpoints exposed, `cps-nf-proxy` docker image
- `cps-xnf-docker` - both CPS and xNF Proxy endpoints exposed, `cps-and-nf-proxy` docker image

Compile without generating the docker images

```bash
mvn clean install -Pcps-docker -Pxnf-docker -Pcps-xnf-docker -Djib.skip
```

### Docker-compose deployment example for local environment

Execute following commands from the `docker-compose` folder:

Generate all the docker images:

```bash
mvn clean package -Pcps-docker -Pxnf-docker -Pcps-xnf-docker
```

Generate a specific type of docker image (e.g. `cps-docker`):

```bash
mvn clean package -Pcps-docker
```

Validate the image being populated to local docker:

```bash
docker images | grep cps
```

Run the containers:

```bash
VERSION=0.0.1-SNAPSHOT DB_HOST=dbpostgresql DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
```

Stopping containers:

```bash
docker-compose stop
```

### Running application from Intellij IDEA

1. **Enable** associated **maven profile** (`cps-docker` or `xnf-docker` or `cps-xnf-docker`)

2. **Build** the project:

```bash
mvn clean install
```

3. Create the **run configuration** for `org.onap.cps.Application` class located in `cps-application` module:

- Ensure the selected module is `-cp cps-application` 
- Ensure working directory is pointing the docker-compose folder where application.yml file is: e.g. `~/workspace/onap/cps/docker-compose/`
- Provide the local DB connection properties using environment variables field, e.g. `DB_HOST=127.0.0.1;DB_USERNAME=cps;DB_PASSWORD=cps`

4. Execute.