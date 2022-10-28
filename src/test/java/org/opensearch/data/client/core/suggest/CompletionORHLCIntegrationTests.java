/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.suggest;

import org.opensearch.common.unit.Fuzziness;
import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.search.suggest.SuggestBuilder;
import org.opensearch.search.suggest.SuggestBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.suggest.CompletionIntegrationTests;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CompletionORHLCIntegrationTests.Config.class})
public class CompletionORHLCIntegrationTests extends CompletionIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    @EnableElasticsearchRepositories(considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("completion-es7");
        }
    }

    @Override
    protected Query getSuggestQuery(String suggestionName, String fieldName, String prefix) {
        return new NativeSearchQueryBuilder() //
                .withSuggestBuilder(
                        new SuggestBuilder() //
                                .addSuggestion(
                                        suggestionName, //
                                        SuggestBuilders.completionSuggestion(fieldName) //
                                                .prefix(prefix, Fuzziness.AUTO))) //
                .build(); //
    }
}
