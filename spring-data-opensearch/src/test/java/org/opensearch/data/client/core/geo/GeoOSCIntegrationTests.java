/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.geo;

import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.opensearch.data.client.osc.Queries;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.geo.GeoIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.elasticsearch.utils.geohash.Geohash;
import org.springframework.data.elasticsearch.utils.geohash.Rectangle;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {GeoOSCIntegrationTests.Config.class})
public class GeoOSCIntegrationTests extends GeoIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    @EnableElasticsearchRepositories(considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("geo-integration-os");
        }
    }

    @Override
    protected Query nativeQueryForBoundingBox(String fieldName, double top, double left, double bottom, double right) {
        return NativeQuery.builder() //
                .withQuery(q -> q //
                        .geoBoundingBox(bb -> bb //
                                .field(fieldName) //
                                .boundingBox(gb -> gb //
                                        .tlbr(tlbr -> tlbr //
                                                .topLeft(tl -> tl //
                                                        .latlon(Queries.latLon(top, left)))
                                                .bottomRight(br -> br //
                                                        .latlon(Queries.latLon(bottom, right)))))))
                .build();

    }

    @Override
    protected Query nativeQueryForBoundingBox(String fieldName, String geoHash) {
        Rectangle rect = Geohash.toBoundingBox(geoHash);
        return nativeQueryForBoundingBox(fieldName, rect.getMaxY(), rect.getMinX(), rect.getMinY(), rect.getMaxX());

    }
}
