/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import org.opensearch.data.client.junit.jupiter.OpenSearchRestTemplateConfiguration;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ElasticsearchPartQueryIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.test.context.ContextConfiguration;

/**
 * The base class for these tests lives in the org.opensearch.data.client.core.query package, but we need
 * access to the {@link RequestFactory} class here
 */
@ContextConfiguration(classes = {OpenSearchPartQueryORHLCIntegrationTests.Config.class})
public class OpenSearchPartQueryORHLCIntegrationTests extends ElasticsearchPartQueryIntegrationTests {

    @Configuration
    @Import({OpenSearchRestTemplateConfiguration.class})
    static class Config {}

    protected String buildQueryString(Query criteriaQuery, Class<?> clazz) {
        SearchSourceBuilder source = new RequestFactory(operations.getElasticsearchConverter())
                .searchRequest(criteriaQuery, null, clazz, IndexCoordinates.of("dummy"))
                .source();
        // remove defaultboost values
        return source.toString().replaceAll("(\\^\\d+\\.\\d+)", "");
    }
}
