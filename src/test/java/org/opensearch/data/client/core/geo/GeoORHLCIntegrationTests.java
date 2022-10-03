/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.geo;


import org.opensearch.data.client.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.geo.GeoIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {GeoORHLCIntegrationTests.Config.class})
public class GeoORHLCIntegrationTests extends GeoIntegrationTests {

    @Configuration
    @Import({OpensearchRestTemplateConfiguration.class})
    @EnableElasticsearchRepositories(considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("geo-integration-os");
        }
    }

    @Override
    protected Query nativeQueryForBoundingBox(String fieldName, double top, double left, double bottom, double right) {
        return new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.geoBoundingBoxQuery(fieldName).setCorners(top, left, bottom, right))
                .build();
    }

    @Override
    protected Query nativeQueryForBoundingBox(String fieldName, String geoHash) {
        return new NativeSearchQueryBuilder()
                .withFilter(QueryBuilders.geoBoundingBoxQuery(fieldName).setCorners(geoHash))
                .build();
    }
}
