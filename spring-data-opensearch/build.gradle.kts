/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
  jacoco
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
  api(springLibs.data.commons)
  api(springLibs.data.elasticsearch) {
    exclude("co.elastic.clients", "*")
    exclude("org.elasticsearch.client", "*")
  }
  api(opensearchLibs.high.level.client) {
    exclude("commons-logging", "commons-logging")
  }
  
  implementation(jacksonLibs.core)
  implementation(jacksonLibs.databind)
  implementation(springLibs.context)
  implementation(springLibs.tx)
  compileOnly(springLibs.web)

  testImplementation("jakarta.enterprise:jakarta.enterprise.cdi-api:3.0.0")
  testImplementation("org.slf4j:log4j-over-slf4j:2.0.12")
  testImplementation("org.apache.logging.log4j:log4j-core:2.23.0")
  testImplementation("org.apache.logging.log4j:log4j-to-slf4j:2.23.0")
  testImplementation("org.apache.geronimo.specs:geronimo-jcdi_2.0_spec:1.3")
  testImplementation("javax.interceptor:javax.interceptor-api:1.2.2")
  testImplementation(opensearchLibs.testcontainers)
  testImplementation("org.apache.openwebbeans:openwebbeans-impl:2.0.27:jakarta")
  testImplementation("org.apache.openwebbeans:openwebbeans-spi:2.0.27:jakarta")
  testImplementation("org.apache.openwebbeans:openwebbeans-se:2.0.27:jakarta")
  testImplementation("javax.servlet:javax.servlet-api:4.0.1")
  testImplementation("org.apache.xbean:xbean-asm5-shaded:4.5")
  testImplementation("io.specto:hoverfly-java-junit5:0.16.2")
  testImplementation("org.skyscreamer:jsonassert:1.5.1")
  testImplementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
  testImplementation(springLibs.web)
  testImplementation(springLibs.test) {
    exclude("ch.qos.logback", "logback-classic")
  }
  testImplementation("org.wiremock:wiremock:3.4.2") {
    exclude("commons-logging", "commons-logging")
    exclude("org.ow2.asm", "asm")
  }
}


description = "Spring Data Opensearch"

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
        name.set("Spring Data OpenSearch")
        packaging = "jar"
        artifactId = "spring-data-opensearch"
        description.set("Spring Data Implementation for OpenSearch")
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
