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
  implementation(project(":spring-data-opensearch-starter"))
  implementation(libs.jupiter)
  implementation(springLibs.boot.test.autoconfigure)
  implementation(springLibs.boot.testcontainers)
  implementation(springLibs.test)
  implementation(opensearchLibs.testcontainers)
}

description = "Spring Data OpenSearch Spring Boot Testcontainers"

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
        name.set("Spring Data OpenSearch Spring Boot Testcontainers")
        packaging = "jar"
        artifactId = "spring-data-opensearch-testcontainers"
        description.set("Spring Boot autoconfigurations for Spring Data Implementation for OpenSearch to support testing")
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

tasks.withType<Javadoc>() {
  options.memberLevel = JavadocMemberLevel.PACKAGE
}
