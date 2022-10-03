/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;


import org.opensearch.data.client.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.ElasticsearchPartQueryIntegrationTests;
import org.springframework.test.context.ContextConfiguration;

/**
 * The base class for these tests lives in the org.opensearch.data.client.core.query package, but we need
 * access to the {@link RequestFactory} class here
 */
@ContextConfiguration(classes = {OpensearchPartQueryORHLCIntegrationTests.Config.class})
public class OpensearchPartQueryORHLCIntegrationTests extends ElasticsearchPartQueryIntegrationTests {

    @Configuration
    @Import({OpensearchRestTemplateConfiguration.class})
    static class Config {}

    protected String buildQueryString(CriteriaQuery criteriaQuery, Class<?> clazz) {
        SearchSourceBuilder source = new RequestFactory(operations.getElasticsearchConverter())
                .searchRequest(criteriaQuery, clazz, IndexCoordinates.of("dummy"))
                .source();
        // remove defaultboost values
        return source.toString().replaceAll("(\\^\\d+\\.\\d+)", "");
    }
}
