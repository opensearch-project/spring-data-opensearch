Spring Data for OpenSearch
=== 

The primary goal of the [Spring Data](https://projects.spring.io/spring-data) project is to make it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services.

The Spring Data OpenSearch project provides [Spring Data](https://projects.spring.io/spring-data) compatible integration with the [OpenSearch](https://opensearch.org/) search engine.
Key functional areas of Spring Data OpenSearch are a POJO centric model for interacting with a OpenSearch Documents and easily writing a Repository style data access layer. This project is built on top of [Spring Data Elasticsearch](https://spring.io/projects/spring-data-elasticsearch/).

## Features

* Spring configuration support using Java based `@Configuration` classes or an XML namespace for a OpenSearch clients instances.
* `ElasticsearchOperations` class and implementations that increases productivity performing common OpenSearch operations.
Includes integrated object mapping between documents and POJOs.
* Feature Rich Object Mapping integrated with Spring’s Conversion Service
* Annotation based mapping metadata
* Automatic implementation of `Repository` interfaces including support for custom search methods.
* CDI support for repositories

## About OpenSearch versions and clients

The Spring Data OpenSearch follows the release model of the Spring Data Elasticsearch / Spring Boot projects.

### Compatibility Matrix

| Spring Data Release Train | Spring Data OpenSearch | Spring Data Elasticsearch | OpenSearch Server | OpenSearch Client | Spring Framework | Spring Boot   |
|---------------------------|------------------------|---------------------------|-------------------|-------------------|------------------|---------------|
| 2025.0                    | 1.7.x                  | 5.5.x                     | 1.x / 2.x / 3.x   | 2.10.x and above  | 6.2.x            | 3.5.x         |
| 2024.1                    | 1.6.x                  | 5.4.x                     | 1.x / 2.x         | 2.10.x and above  | 6.2.x            | 3.4.x         |
| 2024.0                    | 1.5.x                  | 5.3.x                     | 1.x / 2.x         | 2.10.x and above  | 6.1.x            | 3.2.x / 3.3.x |
| 2023.1 (Vaughan)          | 1.4.x                  | 5.2.x                     | 1.x / 2.x         | 2.10.x and above  | 6.1.x            | 3.2.x         |
| 2023.1 (Vaughan)          | 1.3.x                  | 5.2.x                     | 1.x / 2.x         | 2.7.x and above   | 6.1.x            | 3.2.x         |
| 2023.0 (Ullman)           | 1.2.x                  | 5.1.x                     | 1.x / 2.x         | 2.7.x and above   | 6.0.x            | 3.1.x         |
| 2022.0 (Turing)           | 1.1.x                  | 5.0.x                     | 1.x / 2.x         | 2.7.x and above   | 6.0.x            | 3.0.x         |
| 2022.0 (Turing)           | 1.0.x                  | 5.0.x                     | 1.x / 2.x         | 1.x / 2.6.x       | 6.0.x            | 3.0.x         |
| 2022.0 (Turing)           | 0.2.0                  | 5.0.x                     | 1.x / 2.x         | 1.x / 2.x         | 6.0.x            | 3.0.x         |
| 2022.0 (Turing)           | 0.1.0                  | 5.0.x                     | 1.x / 2.x         | 1.x / 2.x         | 6.0.x            | 3.0.x         |

### OpenSearch 2.x / 1.x client libraries


Spring Data OpenSearch provides the possibility to use either `RestHighLevelCLient` or [OpenSearchClient](https://github.com/opensearch-project/opensearch-java) to connect to OpenSearch clusters. 

#### Using `RestHighLevelCLient` (default)

By default, the `RestHighLevelCLient` is configured as the means to communicate with OpenSearch clusters.

```xml
<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>spring-data-opensearch</artifactId>
	<version>1.7.0</version>
</dependency>
```

To use Spring Boot 3.x auto configuration support:

```xml
<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>spring-data-opensearch-starter</artifactId>
	<version>1.7.0</version>
</dependency>
```

To use Spring Boot 3.x auto configuration support for testing:

```xml
<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>spring-data-opensearch-test-autoconfigure</artifactId>
	<version>1.7.0</version>
	<scope>test</scope>
</dependency>
```

#### Using `OpenSearchClient` (preferred)

To switch over to `OpenSearchClient`, the `opensearch-rest-high-level-client` dependency has to be replaced in favor of `opensearch-java`.

```xml
<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>spring-data-opensearch</artifactId>
	<version>1.7.0</version>
	<exclusions>
		<exclusion>
			<groupId>org.opensearch.client</groupId>
			<artifactId>opensearch-rest-high-level-client</artifactId>
		</exclusion>
	</exclusions>
</dependency>

<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>opensearch-java</artifactId>
	<version>2.11.1</version>
</dependency>
```

To use Spring Boot 3.x auto configuration support:

```xml
<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>spring-data-opensearch-starter</artifactId>
	<version>1.7.0</version>
	<exclusions>
		<exclusion>
			<groupId>org.opensearch.client</groupId>
			<artifactId>opensearch-rest-high-level-client</artifactId>
		</exclusion>
	</exclusions>
</dependency>

<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>opensearch-java</artifactId>
	<version>2.11.1</version>
</dependency>
```

To use Spring Boot 3.x auto configuration support for testing:

```xml
<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>spring-data-opensearch-test-autoconfigure</artifactId>
	<version>1.7.0</version>
	<scope>test</scope>
	<exclusions>
		<exclusion>
			<groupId>org.opensearch.client</groupId>
			<artifactId>opensearch-rest-high-level-client</artifactId>
		</exclusion>
	</exclusions>
</dependency>

<dependency>
	<groupId>org.opensearch.client</groupId>
	<artifactId>opensearch-java</artifactId>
	<version>2.11.1</version>
</dependency>
```

## Getting Started

Here is a quick teaser of an application using Spring Data Repositories in Java:

```java
public interface PersonRepository extends CrudRepository<Person, Long> {

  List<Person> findByLastname(String lastname);

  List<Person> findByFirstnameLike(String firstname);
}
```

```java
@Service
public class MyService {

  private final PersonRepository repository;

  public MyService(PersonRepository repository) {
    this.repository = repository;
  }

  public void doWork() {

    repository.deleteAll();

    Person person = new Person();
    person.setFirstname("Oliver");
    person.setLastname("Gierke");
    repository.save(person);

    List<Person> lastNameResults = repository.findByLastname("Gierke");
    List<Person> firstNameResults = repository.findByFirstnameLike("Oli");
 }
}
```

Or, using the reactive Spring Data Repositories:

```java
public interface PersonRepository extends ReactiveCrudRepository<Person, Long> {

  Flux<Person> findByLastname(String lastname);

  Flux<Person> findByFirstnameLike(String firstname);
}
```

### Using the OpenSearch RestClient

Spring Data OpenSearch operates upon an OpenSearch client that is connected to a single OpenSearch node or a cluster. Although the OpenSearch Client can be used directly to work with the cluster, applications using Spring Data Elasticsearch normally use the higher level abstractions of `ElasticsearchOperations` and repositories (please consult [official Spring Data Elasticsearch documentation](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)). Use the builder to provide cluster addresses, set default `HttpHeaders` or enable SSL.

```java
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.RestClients;

@Configuration
public class RestClientConfig extends AbstractOpenSearchConfiguration {

    @Override
    @Bean
    public RestHighLevelClient opensearchClient() {

        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .build();

        return RestClients.create(clientConfiguration).rest();
    }
}

```

Once `RestHighLevelClient` is created, it is also possible to obtain the `lowLevelRest()` client.

``` java
// ...

  @Autowired
  RestHighLevelClient highLevelClient;

  RestClient lowLevelClient = highLevelClient.getLowLevelClient();

// ...

IndexRequest request = new IndexRequest("spring-data")
  .id(randomID())
  .source(singletonMap("feature", "high-level-rest-client"))
  .setRefreshPolicy(IMMEDIATE);

IndexResponse response = highLevelClient.index(request,RequestOptions.DEFAULT);

```

### Client Configuration

Client behaviour can be changed via the `ClientConfiguration` that allows to set options for SSL, connect and socket timeouts, headers and other parameters.

```java
HttpHeaders httpHeaders = new HttpHeaders();
httpHeaders.add("some-header", "on every request")

ClientConfiguration clientConfiguration = ClientConfiguration.builder()
  .connectedTo("localhost:9200", "localhost:9291")
  .usingSsl()
  .withProxy("localhost:8888")
  .withPathPrefix("ola")
  .withConnectTimeout(Duration.ofSeconds(5))
  .withSocketTimeout(Duration.ofSeconds(3))
  .withDefaultHeaders(defaultHeaders)
  .withBasicAuth(username, password)
  .withHeaders(() -> {
    HttpHeaders headers = new HttpHeaders();
    headers.add("currentTime", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    return headers;
  })
  .withClientConfigurer(clientConfigurer -> {
  	  // ...
      return clientConfigurer;
  	}))
  . // ... other options
  .build();

```

In this code snippet, the client configuration was customized to:
- Define default headers, if they need to be customized
- Use the builder to provide cluster addresses, set default `HttpHeaders` or enable SSL.
- Optionally enable SSL.
- Optionally set a proxy.
- Optionally set a path prefix, mostly used when different clusters a behind some reverse proxy.
- Set the connection timeout (default is `10 sec`).
- Set the socket timeout (default is `5 sec`).
- Optionally set headers.
- Add basic authentication.
- A `Supplier<Header>` function can be specified which is called every time before a request is sent to OpenSearch - here, as an example, the current time is written in a header.
- A function configuring the low level REST client

### Spring Boot integration

If you are using Spring Data OpenSearch along with Spring Boot 3.x, there is a dedicated `spring-data-opensearch-starter` module. You may consider excluding the `ElasticsearchDataAutoConfiguration` configuration from automatic discovery (otherwise, the `Elasticsearch` related initialization kicks in, see please https://github.com/spring-projects/spring-boot/issues/33010).

```java
@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class})
public class OpenSearchDemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(OpenSearchDemoApplication.class, args);
  }
}
```

For testing purposes, there is a new `@DataOpenSearchTest` annotation that is provided by `spring-data-opensearch-test-autoconfigure` (requires `spring-boot-test-autoconfigure`) module to simplify testing Spring Data OpenSearch (it explicitly excludes `ElasticsearchDataAutoConfiguration` from the list of configurations). Here is the typical usage along with `@EnableElasticsearchRepositories`:

```java
@DataOpenSearchTest
@EnableElasticsearchRepositories
public class MarketplaceRepositoryIntegrationTests {
   ...
}
```

Or, using the reactive Spring Data Repositories:

```java
@DataOpenSearchTest
@EnableReactiveElasticsearchRepositories
public class MarketplaceRepositoryIntegrationTests {
   ...
}
```

### Spring Boot Service Connection

#### Testcontainers Service Connections

See https://docs.spring.io/spring-boot/reference/testing/testcontainers.html#testing.testcontainers.service-connections 

```java
@Container
@ServiceConnection
static final OpenSearchContainer<?> container = new OpenSearchContainer<>("opensearchproject/opensearch:2.19.1");
```

So, the client will take values from the `OpenSearchContainer` configuration.

#### Docker Compose Service Connections

See https://docs.spring.io/spring-boot/reference/features/dev-services.html#features.dev-services.docker-compose.service-connections

Support image `opensearchproject/opensearch`.

### Apache Maven configuration

Add the Apache Maven dependency:

```xml
<dependency>
  <groupId>org.opensearch.client</groupId>
  <artifactId>spring-data-opensearch</artifactId>
  <version>1.7.0</version>
</dependency>
```

If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version:

```xml
<dependency>
  <groupId>org.opensearch.client</groupId>
  <artifactId>spring-data-opensearch</artifactId>
  <version>${version}-SNAPSHOT</version>
</dependency>

<repository>
  <id>opensearch-libs-snapshot</id>
  <name>AWS Snapshot Repository</name>
  <url>https://aws.oss.sonatype.org/content/repositories/snapshots/</url>
</repository>
```

### Gradle configuration

Add the Gradle dependency:

```groovy
dependencies {
  ...
  implementation "org.opensearch.client:spring-data-opensearch:1.7.0"
  ...
}
```
If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version:

```groovy
dependencies {
  ...
  implementation "org.opensearch.client:spring-data-opensearch:${version}-SNAPSHOT"
  ...
}

repositories {
  ...
  maven {
    url = "https://aws.oss.sonatype.org/content/repositories/snapshots/"
  }
  ...
}
```

## AWS OpenSearch Serverless support

For applications and services that use AWS OpenSearch Serverless offerings, the default clients will not work out of the box.

### Connecting AWS OpenSearch Serverless with OpenSearch RestClient

When using OpenSearch RestClient, the instance of `AwsRequestSigningApacheV5Interceptor` (see please [aws-request-signing-apache-interceptor](ttps://github.com/acm19/aws-request-signing-apache-interceptor) should be configured and injected into request interceptors chain. The sample configuration is provided below:

```
@Configuration
public class OpenSearchAwsClientConfiguration {
    @Value("${aws.os.region}")
    private String region = "";
      
    @Bean
    RestClientBuilderCustomizer customizer() {
        return new RestClientBuilderCustomizer() {
            @Override
            public void customize(HttpAsyncClientBuilder builder) {
                return  builder.addInterceptorLast(new AwsRequestSigningApacheV5Interceptor(
                    "service",
                    AwsV4HttpSigner.create(),
                    DefaultCredentialsProvider.create(),
                    Region.of(region)
                ));
            }

            @Override
            public void customize(RestClientBuilder builder) {
                // No additional customizations needed
            }
        };
    }
}
```

### Connecting AWS OpenSearch Serverless with OpenSearch Java Client

When using OpenSearch Java Client, the instance of the `AwsSdk2Transport` should be configured instead of the default one. The sample configuration is provided below:

```
@Configuration
public class OpenSearchAwsClientConfiguration extends OpenSearchConfiguration {
    @Value("${aws.os.endpoint}")
    private String endpoint = "";
      
    @Value("${aws.os.region}")
    private String region = "";
      
    @Value("${aws.os.username}")
    private String username = "";
      
    @Value("${aws.os.password}")
    private String password = "";

    @NonNull
    @Override
    public ClientConfiguration clientConfiguration() {
        return  ClientConfiguration.builder()
            .connectedTo(endpoint)
            .usingSsl()
            .withBasicAuth(username,password)
            .withConnectTimeout(Duration.ofSeconds(10))
            .withSocketTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public OpenSearchTransport opensearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {    
        final SdkHttpClient httpClient = ApacheHttpClient.builder().build();
        return new AwsSdk2Transport(
            httpClient,
            endpoint,
            "es" // signing service name, use "aoss" for OpenSearch Serverless
            region,
            AwsSdk2TransportOptions.builder().build());
    }
}
```

## Reporting Issues

Spring Data OpenSearch uses GitHub as issue tracking system to record bugs and feature requests.
If you want to raise an issue, please follow the recommendations below:

* Before you log a bug, please search the
[issue tracker](https://github.com/opensearch-project/spring-data-opensearch/issues) to see if someone has already reported the problem.
* If the issue doesn’t already exist, [create a new issue](https://github.com/opensearch-project/spring-data-opensearch/issues/new).
* Please provide as much information as possible with the issue report, we like to know the version of Spring Data OpenSearch that you are using and JVM version.
* If you need to paste code, or include a stack trace use Markdown +++```+++ escapes before and after your text.
* If possible try to create a test-case or project that replicates the issue.
Attach a link to your code or a compressed file containing your code.

## Building from Source

You need JDK 17 (or above) to build the `main` branch. 

```bash
./gradlew clean check
```

## Examples

Please check [spring-data-opensearch-examples](spring-data-opensearch-examples) for examples.

## Code of Conduct

This project has adopted the [Amazon Open Source Code of Conduct](CODE_OF_CONDUCT.md). For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq), or contact [opensource-codeofconduct@amazon.com](mailto:opensource-codeofconduct@amazon.com) with any additional questions or comments.

## License
Spring Data OpenSearch is licensed under the Apache license, version 2.0. Full license text is available in the [LICENSE](LICENSE.txt) file.

Please note that the project explicitly does not require a CLA (Contributor License Agreement) from its contributors.

## Copyright

Copyright OpenSearch Contributors. See [NOTICE](NOTICE.txt) for details.

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

