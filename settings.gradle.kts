/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
 
rootProject.name = "spring-data-opensearch-parent"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("jupiter", "6.0.2")
      library("jupiter", "org.junit.jupiter", "junit-jupiter").versionRef("jupiter")
      library("jupiter-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("jupiter")
      library("junit-platform-launcher", "org.junit.platform", "junit-platform-launcher").versionRef("jupiter")
    }

    create("springLibs") {
      version("spring", "7.0.3")
      version("spring-boot", "4.0.1")
      library("data-commons", "org.springframework.data:spring-data-commons:4.0.2")
      library("data-elasticsearch", "org.springframework.data:spring-data-elasticsearch:6.0.2")
      library("web", "org.springframework", "spring-web").versionRef("spring")
      library("webflux", "org.springframework", "spring-webflux").versionRef("spring")
      library("context", "org.springframework", "spring-context").versionRef("spring")
      library("tx", "org.springframework", "spring-tx").versionRef("spring")
      library("test", "org.springframework", "spring-test").versionRef("spring")
      library("boot-web", "org.springframework.boot", "spring-boot-starter-web").versionRef("spring-boot")
      library("boot-webflux", "org.springframework.boot", "spring-boot-starter-webflux").versionRef("spring-boot")
      library("boot-autoconfigure", "org.springframework.boot", "spring-boot-autoconfigure").versionRef("spring-boot")
      library("boot-docker-compose", "org.springframework.boot", "spring-boot-docker-compose").versionRef("spring-boot")
      library("boot-elasticsearch", "org.springframework.boot", "spring-boot-elasticsearch").versionRef("spring-boot")
      library("boot-data-elasticsearch", "org.springframework.boot", "spring-boot-data-elasticsearch").versionRef("spring-boot")
      library("boot-test", "org.springframework.boot", "spring-boot-test").versionRef("spring-boot")
      library("boot-test-autoconfigure", "org.springframework.boot", "spring-boot-test-autoconfigure").versionRef("spring-boot")
      library("boot-test-cache", "org.springframework.boot", "spring-boot-cache-test").versionRef("spring-boot")
      library("boot-testcontainers", "org.springframework.boot", "spring-boot-testcontainers").versionRef("spring-boot")
      library("projectreactor", "io.projectreactor:reactor-test:3.8.2")
      plugin("spring-boot", "org.springframework.boot").versionRef("spring-boot")
    }
    
    create("opensearchLibs") {
      version("opensearch", "3.4.0")
      library("java-client", "org.opensearch.client:opensearch-java:3.5.0")
      library("client", "org.opensearch.client", "opensearch-rest-client").versionRef("opensearch")
      library("high-level-client", "org.opensearch.client", "opensearch-rest-high-level-client").versionRef("opensearch")
      library("sniffer", "org.opensearch.client", "opensearch-rest-client-sniffer").versionRef("opensearch")
      library("testcontainers", "org.opensearch:opensearch-testcontainers:4.1.0")
    }
    
    create("jacksonLibs") {
      version("jackson", "2.20.1")
      library("core", "com.fasterxml.jackson.core", "jackson-core").versionRef("jackson")
      library("databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")
    }
    
    create("jakarta") {
      library("json-bind", "jakarta.json.bind:jakarta.json.bind-api:2.0.0")
    }
    
    create("pluginLibs") {
      version("spotless", "6.25.0")
      version("editorconfig", "0.0.3")
      version("release", "3.1.0")
      plugin("editorconfig", "org.ec4j.editorconfig").versionRef("editorconfig")
      plugin("spotless", "com.diffplug.spotless").versionRef("spotless")
      plugin("release", "net.researchgate.release").versionRef("release")
      library("editorconfig", "gradle.plugin.org.ec4j.gradle", "editorconfig-gradle-plugin").versionRef("editorconfig")
      library("spotless", "com.diffplug.spotless", "spotless-plugin-gradle").versionRef("spotless")
      library("release", "net.researchgate", "gradle-release").versionRef("release")
    }
  }
}

pluginManagement {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
    maven {
      url = uri("https://repo.spring.io/release/")
    }
    maven {
      url = uri("https://repo.spring.io/milestone/")
    }
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven {
      url = uri("https://repo.spring.io/milestone/")
    }
  }
}

include("spring-data-opensearch")
include("spring-data-opensearch-docker-compose")
include("spring-data-opensearch-starter")
include("spring-data-opensearch-test-autoconfigure")
include("spring-data-opensearch-testcontainers")
include("spring-data-opensearch-examples:spring-boot-gradle")
include("spring-data-opensearch-examples:spring-boot-java-client-gradle")
include("spring-data-opensearch-examples:spring-boot-reactive-client-gradle")
