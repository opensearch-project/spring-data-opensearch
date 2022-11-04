/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
 
rootProject.name = "spring-data-opensearch"

dependencyResolutionManagement {
  versionCatalogs {
    create("springLibs") {
      version("spring", "6.0.0-RC3")
      library("data-commons", "org.springframework.data:spring-data-commons:3.0.0-RC2")
      library("data-elasticsearch", "org.springframework.data:spring-data-elasticsearch:5.0.0-RC2")
      library("web", "org.springframework", "spring-web").versionRef("spring")
      library("context", "org.springframework", "spring-context").versionRef("spring")
      library("tx", "org.springframework", "spring-tx").versionRef("spring")
      library("test", "org.springframework", "spring-test").versionRef("spring")
    }
    
    create("opensearchLibs") {
      library("client", "org.opensearch.client:opensearch-rest-high-level-client:2.3.0")
      library("testcontainers", "org.opensearch:opensearch-testcontainers:2.0.0") 
    }
  }
}