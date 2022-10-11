Spring Data for Opensearch
=== 

The primary goal of the [Spring Data](https://projects.spring.io/spring-data) project is to make it easier to build Spring-powered applications that use new data access technologies such as non-relational databases, map-reduce frameworks, and cloud based data services.

The Spring Data Opensearch project provides [Spring Data](https://projects.spring.io/spring-data) compatible integration with the [Opensearch](https://opensearch.org/) search engine.
Key functional areas of Spring Data Opensearch are a POJO centric model for interacting with a Opensearch Documents and easily writing a Repository style data access layer. This project is built on top of [Spring Data Elasticsearch](https://spring.io/projects/spring-data-elasticsearch/).

## Features

* Spring configuration support using Java based `@Configuration` classes or an XML namespace for a Opensearch clients instances.
* `ElasticsearchOperations` class and implementations that increases productivity performing common Opensearch operations.
Includes integrated object mapping between documents and POJOs.
* Feature Rich Object Mapping integrated with Spring’s Conversion Service
* Annotation based mapping metadata
* Automatic implementation of `Repository` interfaces including support for custom search methods.
* CDI support for repositories

## About Opensearch versions and clients

### Compatibility Matrix

| Spring Data Release Train | Spring Data Opensearch | Spring Data Elasticsearch | Opensearch     | Spring Framework | Spring Boot |
|---------------------------|------------------------|---------------------------|----------------|------------------|-------------|
| 2022.0 (Turing)           | 5.0.x                  | 5.0.x                     | 1.3.6 / 2.3.0  | 6.0.x            | 3.0.x       |

### Opensearch 2.x / 1.x client libraries

At the moment, Spring Data Opensearch provides the possibility to use the `RestHighLevelCLient` to connect to Opensearch clusters. 

```xml
<dependency>
	<groupId>org.opensearch</groupId>
	<artifactId>spring-data-opensearch</artifactId>
	<version>${version}</version>
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

### Using the Opensearch RestClient

Spring Data Opensearch operates upon an Opensearch client that is connected to a single Opensearch node or a cluster. Although the Opensearch Client can be used directly to work with the cluster, applications using Spring Data Elasticsearch normally use the higher level abstractions of `ElasticsearchOperations` and repositories (please consult [official Spring Data Elasticsearch documentation](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)). Use the builder to provide cluster addresses, set default `HttpHeaders` or enable SSL.

```java
import org.opensearch.data.client.orhlc.AbstractOpensearchConfiguration;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.RestClients;

@Configuration
public class RestClientConfig extends AbstractOpensearchConfiguration {

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
- A `Supplier<Header>` function can be specified which is called every time before a request is sent to Opensearch - here, as an example, the current time is written in a header.
- A function configuring the low level REST client

### Spring Boot integration

If you are using Spring Data Opensearch along with Spring Boot (3.x milestone releases), you may consider excluding the `ElasticsearchDataAutoConfiguration` configuration from automatic discovery. 

```java
@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class})
public class OpensearchDemoApplication {
  public static void main(String[] args) {
    SpringApplication.run(OpensearchDemoApplication.class, args);
  }
}
```

### Apache Maven configuration

Add the Maven dependency:

```xml
<dependency>
  <groupId>org.opensearch</groupId>
  <artifactId>spring-data-opensearch</artifactId>
  <version>${version}</version>
</dependency>
```
If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version:

```xml
<dependency>
  <groupId>org.opensearch</groupId>
  <artifactId>spring-data-opensearch</artifactId>
  <version>${version}-SNAPSHOT</version>
</dependency>

<repository>
  <id>opensearch-libs-snapshot</id>
  <name>AWS Snapshot Repository</name>
  <url>https://aws.oss.sonatype.org/content/repositories/snapshots/</url>
</repository>
```

## Reporting Issues

Spring Data Opensearch uses GitHub as issue tracking system to record bugs and feature requests.
If you want to raise an issue, please follow the recommendations below:

* Before you log a bug, please search the
[issue tracker](https://github.com/opensearch-project/spring-data-opensearch/issues) to see if someone has already reported the problem.
* If the issue doesn’t already exist, [create a new issue](https://github.com/opensearch-project/spring-data-opensearch/issues/new).
* Please provide as much information as possible with the issue report, we like to know the version of Spring Data Opensearch that you are using and JVM version.
* If you need to paste code, or include a stack trace use Markdown +++```+++ escapes before and after your text.
* If possible try to create a test-case or project that replicates the issue.
Attach a link to your code or a compressed file containing your code.

## Building from Source

You need [Apache Maven 3.5.0 or above](https://maven.apache.org/run-maven/index.html) and JDK 17 (or above) to build the `main` branch.

```bash
$ mvn clean install
```

## Code of Conduct

This project has adopted the [Amazon Open Source Code of Conduct](CODE_OF_CONDUCT.md). For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq), or contact [opensource-codeofconduct@amazon.com](mailto:opensource-codeofconduct@amazon.com) with any additional questions or comments.

## License
Spring Data Opensearch is licensed under the Apache license, version 2.0. Full license text is available in the [LICENSE](LICENSE.txt) file.

Please note that the project explicitly does not require a CLA (Contributor License Agreement) from its contributors.

## Copyright

Copyright OpenSearch Contributors. See [NOTICE](NOTICE.txt) for details.

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

