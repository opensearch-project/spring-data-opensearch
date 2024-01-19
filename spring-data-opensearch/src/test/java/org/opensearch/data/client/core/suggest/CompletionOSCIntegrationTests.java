/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.suggest;

import org.opensearch.client.opensearch.core.search.FieldSuggester;
import org.opensearch.client.opensearch.core.search.SuggestFuzziness;
import org.opensearch.client.opensearch.core.search.Suggester;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.suggest.CompletionIntegrationTests;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {CompletionOSCIntegrationTests.Config.class})
public class CompletionOSCIntegrationTests extends CompletionIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    @EnableElasticsearchRepositories(considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("completion-es7");
        }
    }

    @Override
    protected Query getSuggestQuery(String suggestionName, String fieldName, String prefix) {
        return NativeQuery.builder() //
                .withSuggester(Suggester.of(s -> s //
                        .suggesters(suggestionName, FieldSuggester.of(fs -> fs //
                                .prefix(prefix)//
                                .completion(cs -> cs //
                                        .field(fieldName) //
                                        .fuzzy(SuggestFuzziness.of(f -> f //
                                                .fuzziness("AUTO") //
                                                .minLength(3) //
                                                .prefixLength(1) //
                                                .transpositions(true) //
                                                .unicodeAware(false))))//
                        ))) //
                ).build();
    }
}
