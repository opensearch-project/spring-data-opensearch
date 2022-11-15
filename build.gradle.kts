/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

import net.researchgate.release.ReleaseExtension

plugins {
  java
  eclipse
  idea
  `test-report-aggregation`
  `jacoco-report-aggregation`
  alias(pluginLibs.plugins.release)
}

buildscript {
  dependencies {
    classpath(pluginLibs.release)
  }
}

configure<ReleaseExtension> {
  with(git) {
    requireBranch.set("main")
  }
}

dependencies {
  testReportAggregation(project(":spring-data-opensearch")) 
  testReportAggregation(project(":spring-data-opensearch-starter"))
}

tasks.check {
  dependsOn(tasks.named<TestReport>("testAggregateTestReport")) 
}
