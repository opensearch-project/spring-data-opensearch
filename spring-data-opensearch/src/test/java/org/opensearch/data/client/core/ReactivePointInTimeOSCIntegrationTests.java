/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;

import org.opensearch.data.client.EnabledIfOpenSearchVersion;
import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ReactivePointInTimeIntegrationTests;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = ReactivePointInTimeOSCIntegrationTests.Config.class)
@EnabledIfOpenSearchVersion(
        onOrAfter = "2.3.0",
        reason = "https://github.com/opensearch-project/OpenSearch/issues/1147")
public class ReactivePointInTimeOSCIntegrationTests extends ReactivePointInTimeIntegrationTests {

    @Configuration
    @Import({ ReactiveOpenSearchTemplateConfiguration.class })
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("reactive-point-in-time-os");
        }
    }
}
