/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.repository.query.keywords;

import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.query.keywords.ReactiveQueryKeywordsIntegrationTests;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ReactiveQueryKeywordsOSCIntegrationTests.Config.class })
public class ReactiveQueryKeywordsOSCIntegrationTests extends ReactiveQueryKeywordsIntegrationTests {

    @Configuration
    @Import({ ReactiveOpenSearchTemplateConfiguration.class })
    @EnableReactiveElasticsearchRepositories(
            basePackages = {"org.springframework.data.elasticsearch.repository.query.keywords"},
            considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("reactive-query-keywords-os");
        }
    }

}
