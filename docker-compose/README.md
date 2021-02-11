# Docker Compose deployment example for local enviroments after build, CPS deployment is done via OOM

To run the application locally using `docker-compose`, execute following command from this `docker-compose` folder:

Compile without generating the docker images

```bash
mvn clean install -Pcps-docker -Pxnf-docker -Pcps-xnf-docker -Djib.skip
```

Generate the docker images

```bash
mvn clean install -Pcps-docker -Pxnf-docker -Pcps-xnf-docker -Dnexus.repository=""
```

for generate a specific type of docker images

```bash
mvn clean install -Pcps-docker
```

Run the containers

Edit docker-compose/docker-compose.yml and uncomment desired service to be deployed, by default ``cps-and-nf-proxy`` is enabled.
You can comment it and uncomment ``cps-standalone`` or ``nf-proxy-standalone``.

```bash
VERSION=0.0.1-SNAPSHOT DB_HOST=dbpostgresql DB_USERNAME=cps DB_PASSWORD=cps docker-compose up -d
```

Run application from Intellj IDE

you need first to enable the maven profile desired under tab Maven
then go to Run -> Edit configurations
 1- Working directory -> select docker-compose folder e.g.  ~/workspace/onap/cps/docker-compose/
 2- Enviroment variables -> add variables configuration e.g. DB_HOST=127.0.0.1;DB_USERNAME=cps;DB_PASSWORD=cps