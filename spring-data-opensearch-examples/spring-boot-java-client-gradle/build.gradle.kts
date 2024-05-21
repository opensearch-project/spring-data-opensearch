/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
  alias(springLibs.plugins.spring.boot)
  alias(pluginLibs.plugins.spotless)
  alias(pluginLibs.plugins.editorconfig)
  id("java-conventions")
}

buildscript {
  dependencies {
    classpath(pluginLibs.editorconfig)
    classpath(pluginLibs.spotless)
  }
}

dependencies {
  api(project(":spring-data-opensearch")) {
    exclude("org.opensearch.client", "opensearch-rest-high-level-client")
  }
  api(project(":spring-data-opensearch-starter")) {
    exclude("org.opensearch.client", "opensearch-rest-high-level-client")
  }
  implementation(springLibs.boot.web)
  implementation(jacksonLibs.core)
  implementation(jacksonLibs.databind)
  implementation(opensearchLibs.client)
  implementation(opensearchLibs.java.client)
  testImplementation(springLibs.test)
  testImplementation(springLibs.boot.test)
  testImplementation(springLibs.boot.test.autoconfigure)
  testImplementation(opensearchLibs.testcontainers)
  testImplementation(project(":spring-data-opensearch-test-autoconfigure")) {
    exclude("org.opensearch.client", "opensearch-rest-high-level-client")
  }

  constraints {
    implementation("ch.qos.logback:logback-classic") {
      version {
        require("1.4.12")
      }
      because("Fixes CVE-2023-6378")
    }
  }
}

description = "Spring Data OpenSearch Spring Boot Example Project"

spotless {
  java {
    target("src/main/java/**/*.java", "src/test/java/org/opensearch/**/*.java")

    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()

    removeUnusedImports()
    importOrder()
  }
}
