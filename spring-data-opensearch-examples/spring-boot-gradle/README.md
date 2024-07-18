Spring Data OpenSearch Spring Boot Example Project
=== 

This sample project demonstrates the usage of the [Spring Data OpenSearch](https://github.com/opensearch-project/spring-data-opensearch/) in the typical Spring Boot web application. The application assumes that there is an [OpenSearch](https://opensearch.org) service up and running on the local machine, available at `https://localhost:9200` (protected by basic authentication with default credentials).

1. The easiest way to get [OpenSearch](https://opensearch.org) service up and running is by using [Docker](https://www.docker.com/):

```shell
docker run -p 9200:9200 -e "discovery.type=single-node" -e OPENSEARCH_INITIAL_ADMIN_PASSWORD=<strong-password> opensearchproject/opensearch:2.15.0
```

2. Build and run the project using [Gradle](https://gradle.org/):

```shell
./gradlew :spring-data-opensearch-examples:spring-boot-gradle:bootRun
```

3. Exercise the REST endpoint available at: `http://localhost:8080/marketplace`

   - Fetch all products: `curl 'http://localhost:8080/marketplace/search'`
   - Search products by name: `curl 'http://localhost:8080/marketplace/search?name=pillow'`
   - Search products by name and price greater than: `curl 'http://localhost:8080/marketplace/search?name=pillow&price=35.0'`