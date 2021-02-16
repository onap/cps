**Build step**

From the project's root directory, run

    mvn clean install

**Deployment**

Run postgres DB and create a database. If using docker, use the following command

    docker run -d --name dev-postgres \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_DB=template_db \
    -p 5432:5432 postgres:10


Configure the DB details(username, password, db name)
in application.yml

Run the application using the following command

    java -jar target/cps-tdmt-0.0.1-SNAPSHOT.jar --spring.config.location=file:application.yml
