/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

import net.researchgate.release.ReleaseExtension
import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
  java
  `java-library`
  `maven-publish`
  eclipse
  idea
  jacoco
  id("org.ec4j.editorconfig") version "0.0.3"
  id("com.diffplug.spotless") version "6.11.0"
}

buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
  dependencies {
    classpath("gradle.plugin.org.ec4j.gradle:editorconfig-gradle-plugin:0.0.3")
    classpath("com.diffplug.spotless:spotless-plugin-gradle:6.11.0")
    classpath("net.researchgate:gradle-release:3.0.2")
  }
}

apply(plugin = "org.ec4j.editorconfig")
apply(plugin = "com.diffplug.spotless")
apply(plugin = "net.researchgate.release")

repositories {
  mavenLocal()
  maven {
    url = uri("https://repo.maven.apache.org/maven2/")
  }
  maven {
    url = uri("https://repo.spring.io/milestone/")
  }
}

dependencies {
  api(springLibs.data.commons)
  api(springLibs.data.elasticsearch) {
    exclude("co.elastic.clients", "*")
    exclude("org.elasticsearch.client", "*")
  }
  api(opensearchLibs.client) {
    exclude("commons-logging", "commons-logging")
  }
  
  implementation("com.fasterxml.jackson.core:jackson-core:2.14.0-rc2")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc2")
  implementation(springLibs.context)
  implementation(springLibs.tx)
  compileOnly(springLibs.web)

  testImplementation("jakarta.enterprise:jakarta.enterprise.cdi-api:3.0.0")
  testImplementation("org.slf4j:log4j-over-slf4j:2.0.2")
  testImplementation("org.apache.logging.log4j:log4j-core:2.19.0")
  testImplementation("org.apache.logging.log4j:log4j-to-slf4j:2.19.0")
  testImplementation("org.apache.geronimo.specs:geronimo-jcdi_2.0_spec:1.3")
  testImplementation("javax.interceptor:javax.interceptor-api:1.2.2")
  testImplementation(opensearchLibs.testcontainers)
  testImplementation("org.testcontainers:testcontainers:1.17.5")
  testImplementation("org.apache.openwebbeans:openwebbeans-impl:2.0.27:jakarta")
  testImplementation("org.apache.openwebbeans:openwebbeans-spi:2.0.27:jakarta")
  testImplementation("org.apache.openwebbeans:openwebbeans-se:2.0.27:jakarta")
  testImplementation("javax.servlet:javax.servlet-api:4.0.1")
  testImplementation("org.apache.xbean:xbean-asm5-shaded:4.5")
  testImplementation("io.specto:hoverfly-java-junit5:0.14.3")
  testImplementation("org.skyscreamer:jsonassert:1.5.1")
  testImplementation("org.mockito:mockito-junit-jupiter:4.8.1")
  testImplementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
  testImplementation("ch.qos.logback:logback-classic:1.2.11")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation(springLibs.web)
  testImplementation(springLibs.test) {
    exclude("ch.qos.logback", "logback-classic")
  }
  testImplementation("com.github.tomakehurst:wiremock-jre8:2.34.0") {
    exclude("commons-logging", "commons-logging")
    exclude("org.ow2.asm", "asm")
  }
}

group = "org.opensearch.client"
val build = Properties().apply {
  load(FileInputStream(File(rootProject.rootDir, "version.properties")))
}

// Detect version from version.properties and align it with the build settings
var isSnapshot = "true" == System.getProperty("build.snapshot", "true")
var buildVersion = build.getProperty("version")
if (isSnapshot && !buildVersion.endsWith("SNAPSHOT")) {
  buildVersion = buildVersion + "-SNAPSHOT"
} else if (!isSnapshot && buildVersion.endsWith("SNAPSHOT")) {
  throw GradleException("Expecting release (non-SNAPSHOT) build but version is not set accordingly: " + buildVersion)
}
  
// Check if tag release version (if provided) matches the version from build settings 
val tagVersion = System.getProperty("build.version", buildVersion)
if (!buildVersion.equals(tagVersion)) {
  throw GradleException("The tagged version " + tagVersion + " does not match the build version " + buildVersion)
}

version = buildVersion 
description = "Spring Data Opensearch"
java.sourceCompatibility = JavaVersion.VERSION_17

java {
  withSourcesJar()
  withJavadocJar()
}

tasks.withType<JavaCompile>() {
  options.encoding = "UTF-8"
}

tasks.test {
  useJUnitPlatform() {
    excludeTags("integration-test")
  }
  
  reports {
    junitXml.required.set(true)
    html.required.set(true)
  }  
  
  jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
}

spotless {
  java {
    target("src/main/java/**/*.java", "src/test/java/org/opensearch/**/*.java")

    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()

    removeUnusedImports()
    importOrder()
    palantirJavaFormat()
  }
}

configure<ReleaseExtension> {
  with(git) {
    requireBranch.set("main")
  }
}

publishing {
  repositories{
    if (version.toString().endsWith("SNAPSHOT")) {
      maven("https://aws.oss.sonatype.org/content/repositories/snapshots/") {
        name = "snapshotRepo"
        credentials(PasswordCredentials::class)
      }
    }
    maven("${rootProject.buildDir}/repository") {
      name = "localRepo"
    }
  }

publications {
  create<MavenPublication>("publishMaven") {
    from(components["java"])
      pom {
        name.set("Spring Data Opensearch")
        packaging = "jar"
        artifactId = "spring-data-opensearch"
        description.set("Spring Data Implementation for Opensearch")
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

tasks.withType<ProcessResources> {
  filesMatching("**/testcontainers-opensearch.properties") {
    expand(project.properties)
  }
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
  useJUnitPlatform() {
    includeTags("integration-test")
  }

  shouldRunAfter(tasks.test)
  jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
  
  systemProperty("sde.integration-test.environment", "opensearch")
}

tasks.check {
  dependsOn(integrationTestTask)
  finalizedBy("jacocoTestReport")
}
