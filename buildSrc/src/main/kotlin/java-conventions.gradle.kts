/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
  `java`
  `java-library`
  `maven-publish`
  jacoco
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    url = uri("https://repo.spring.io/milestone/")
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
java.sourceCompatibility = JavaVersion.VERSION_17

dependencies {
  testImplementation("org.testcontainers:testcontainers:1.17.5")
  testImplementation("org.testcontainers:junit-jupiter:1.17.5")
  testImplementation("org.mockito:mockito-junit-jupiter:4.8.1")
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
  testImplementation("org.assertj:assertj-core:3.23.1")
  testImplementation("ch.qos.logback:logback-classic:1.4.4")
}

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

publishing {
  repositories {
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
  dependsOn(integrationTestTask, "javadoc")
  finalizedBy("jacocoTestReport")
}