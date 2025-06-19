Spring Data OpenSearch Java Client Spring Boot Example Project
=== 

This sample project demonstrates the usage of the [Spring Data OpenSearch](https://github.com/opensearch-project/spring-data-opensearch/) in the typical Spring Boot web application. The application assumes that there is an [OpenSearch](https://opensearch.org) service up and running on the local machine.
This example uses the [`opensearch-java` client](https://opensearch.org/docs/latest/clients/java/).

## Pre-requisites

* [Docker](https://www.docker.com/)
* Java 17

## Using Docker CLI

1. Start [OpenSearch](https://opensearch.org) using

```shell
docker run -p 9200:9200 -e "discovery.type=single-node" -e OPENSEARCH_INITIAL_ADMIN_PASSWORD=<strong-password> opensearchproject/opensearch:2.15.0
```

2. Build and run the project using [Gradle](https://gradle.org/):

```shell
./gradlew :spring-data-opensearch-examples:spring-boot-java-client-gradle:bootRun
```

3. Exercise the REST endpoint available at: `http://localhost:8080/marketplace`

   - Fetch all products: `curl 'http://localhost:8080/marketplace/search'`
   - Search products by name: `curl 'http://localhost:8080/marketplace/search?name=pillow'`
   - Search products by name and price greater than: `curl 'http://localhost:8080/marketplace/search?name=pillow&price=35.0'`

## Using Spring Boot Testcontainers integration

1. Build and run the project using [Gradle](https://gradle.org/):

```shell
./gradlew :spring-data-opensearch-examples:spring-boot-java-client-gradle:bootTestRun
```

2. Exercise the REST endpoint available at: `http://localhost:8080/marketplace`

   - Fetch all products: `curl 'http://localhost:8080/marketplace/search'`
   - Search products by name: `curl 'http://localhost:8080/marketplace/search?name=pillow'`
   - Search products by name and price greater than: `curl 'http://localhost:8080/marketplace/search?name=pillow&price=35.0'`
