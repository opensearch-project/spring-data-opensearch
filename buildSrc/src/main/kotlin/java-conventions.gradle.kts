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
  `version-catalog`
  `maven-publish`
  jacoco
  eclipse
  idea
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
 
versionCatalogs
  .named("libs")
  .findLibrary("jupiter")
  .ifPresent { jupiter -> 
    dependencies { 
      testImplementation(jupiter) 
    }
  }
  
versionCatalogs
  .named("libs")
  .findLibrary("junit-platform-launcher")
  .ifPresent { junitPlatformLauncher -> 
    dependencies { 
      testRuntimeOnly(junitPlatformLauncher)
    }
  }

versionCatalogs
  .named("libs")
  .findLibrary("jupiter-params")
  .ifPresent { jupiterParams -> 
    dependencies { 
      testImplementation(jupiterParams) 
    }
  }

dependencies {
  testImplementation("org.testcontainers:testcontainers:2.0.1")
  testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.1")
  testImplementation("org.mockito:mockito-junit-jupiter:5.20.0")
  testImplementation("org.assertj:assertj-core:3.27.4")
  testImplementation("ch.qos.logback:logback-classic:1.5.20")
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
  withSourcesJar()
  withJavadocJar()
}

tasks.withType<JavaCompile>() {
  options.encoding = "UTF-8"
  options.compilerArgs.add("-parameters")
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
      maven("https://central.sonatype.com/repository/maven-snapshots/") {
        name = "Snapshots"
        credentials {
            username = System.getenv("SONATYPE_USERNAME")
            password = System.getenv("SONATYPE_PASSWORD")
        }
      }
    }
    maven(rootProject.layout.buildDirectory.dir("repository")) {
      name = "localRepo"
    }
  }
}

val properties = project.properties
tasks.withType<ProcessResources> {
  filesMatching("**/testcontainers-opensearch.properties") {
    expand(properties)
  }
}

val integrationTestTask = tasks.register<Test>("integrationTest") {
  useJUnitPlatform() {
    includeTags("integration-test")
  }

  shouldRunAfter(tasks.test)
  jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")

  testClassesDirs = sourceSets["test"].output.classesDirs
  classpath = sourceSets["test"].runtimeClasspath

  systemProperty("sde.integration-test.environment", "opensearch")
}

tasks.check {
  dependsOn(integrationTestTask, "javadoc")
  finalizedBy("jacocoTestReport")
}