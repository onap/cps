# Docker Compose deployment example for local enviroments, CPS deployment is done via OOM

To run the application locally using `docker-compose`, execute following command from this `docker-compose` folder:

Generate the containers

```bash
mvn clean install -Pcps-docker -Pxnf-docker -Pcps-xnf-docker
```
or for generate an specific type

```bash
mvn clean install -Pcps-docker
```
Run the containers

```bash
VERSION=0.0.1-SNAPSHOT DB_HOST=dbpostgresql DB_USERNAME=cps DB_PASSWORD=cps docker-compose up
```