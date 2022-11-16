/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
 
rootProject.name = "spring-data-opensearch-parent"

dependencyResolutionManagement {
  versionCatalogs {
    create("springLibs") {
      version("spring", "6.0.0")
      version("spring-boot", "3.0.0-RC2")
      library("data-commons", "org.springframework.data:spring-data-commons:3.0.0-RC2")
      library("data-elasticsearch", "org.springframework.data:spring-data-elasticsearch:5.0.0-RC2")
      library("web", "org.springframework", "spring-web").versionRef("spring")
      library("context", "org.springframework", "spring-context").versionRef("spring")
      library("tx", "org.springframework", "spring-tx").versionRef("spring")
      library("test", "org.springframework", "spring-test").versionRef("spring")
      library("boot-autoconfigure", "org.springframework.boot", "spring-boot-autoconfigure").versionRef("spring-boot")
      library("boot-test", "org.springframework.boot", "spring-boot-test").versionRef("spring-boot")
    }
    
    create("opensearchLibs") {
      version("opensearch", "2.3.0")
      library("client", "org.opensearch.client", "opensearch-rest-client").versionRef("opensearch")
      library("high-level-client", "org.opensearch.client", "opensearch-rest-high-level-client").versionRef("opensearch")
      library("sniffer", "org.opensearch.client", "opensearch-rest-client-sniffer").versionRef("opensearch")
      library("testcontainers", "org.opensearch:opensearch-testcontainers:2.0.0") 
    }
    
    create("jacksonLibs") {
      version("jackson", "2.14.0")
      library("core", "com.fasterxml.jackson.core", "jackson-core").versionRef("jackson")
      library("databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")
    }
    
    create("pluginLibs") {
      version("spotless", "6.11.0")
      version("editorconfig", "0.0.3")
      version("release", "3.0.2")
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
  }
}

include("spring-data-opensearch")
include("spring-data-opensearch-starter")
