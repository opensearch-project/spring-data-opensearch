/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.repository.support;

import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repositories.custommethod.QueryParameter;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.support.SimpleReactiveElasticsearchRepositoryIntegrationTests;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { SimpleReactiveElasticsearchRepositoryOSCIntegrationTests.Config.class })
public class SimpleReactiveElasticsearchRepositoryOSCIntegrationTests
        extends SimpleReactiveElasticsearchRepositoryIntegrationTests {

    @Configuration
    @Import({ ReactiveOpenSearchTemplateConfiguration.class })
    @EnableReactiveElasticsearchRepositories(
            basePackages = { "org.springframework.data.elasticsearch.repository.support" },
            considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("simple-reactive-repository-os");
        }

        /**
         * a normal bean referenced by SpEL in query
         */
        @Bean
        QueryParameter queryParameter() {
            return new QueryParameter("message");
        }
    }
}
