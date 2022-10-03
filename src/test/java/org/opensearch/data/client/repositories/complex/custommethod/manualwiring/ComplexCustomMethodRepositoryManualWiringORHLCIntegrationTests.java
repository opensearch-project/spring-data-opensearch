/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.repositories.complex.custommethod.manualwiring;


import org.opensearch.data.client.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repositories.complex.custommethod.manualwiring.ComplexCustomMethodRepositoryManualWiringIntegrationTests;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {ComplexCustomMethodRepositoryManualWiringORHLCIntegrationTests.Config.class})
public class ComplexCustomMethodRepositoryManualWiringORHLCIntegrationTests
        extends ComplexCustomMethodRepositoryManualWiringIntegrationTests {

    @Configuration
    @Import({OpensearchRestTemplateConfiguration.class})
    @EnableElasticsearchRepositories(
            basePackages = {"org.springframework.data.elasticsearch.repositories.complex.custommethod.manualwiring"},
            considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("complex-custom-method-os");
        }
    }
}
