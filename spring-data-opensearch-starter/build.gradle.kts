/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
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
  api(springLibs.boot.autoconfigure)
  api(project(":spring-data-opensearch"))
  implementation(opensearchLibs.client) {
    exclude("commons-logging", "commons-logging")
    exclude("org.slf4j", "slf4j-api")
  }
  implementation(opensearchLibs.sniffer) {
    exclude("commons-logging", "commons-logging")
  }
  compileOnly(opensearchLibs.java.client)
  compileOnly(jakarta.json.bind)
  testImplementation(springLibs.test) {
    exclude("ch.qos.logback", "logback-classic")
  }
  testImplementation(springLibs.boot.test)
  testImplementation(opensearchLibs.testcontainers)
  testImplementation(jacksonLibs.core)
  testImplementation(jacksonLibs.databind)
}

description = "Spring Data OpenSearch Spring Boot Starter"

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

publishing {
  publications {
    create<MavenPublication>("publishMaven") {
      from(components["java"])
      pom {
        name.set("Spring Data OpenSearch Spring Boot Starter")
        packaging = "jar"
        artifactId = "spring-data-opensearch-starter"
        description.set("Spring Boot Starter for Spring Data Implementation for OpenSearch")
        url.set("https://github.com/opensearch-project/spring-data-opensearch/")
        scm {
          connection.set("scm:git@github.com:opensearch-project/spring-data-opensearch.git")
          developerConnection.set("scm:git@github.com:opensearch-project/spring-data-opensearch.git")
          url.set("git@github.com:opensearch-project/spring-data-opensearch.git")
        }
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            name.set("opensearch-project")
            url.set("https://www.opensearch.org")
            inceptionYear.set("2022")
          }
        }
      }
    }
  }
}