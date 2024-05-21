/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client;

import static org.opensearch.client.opensearch._types.query_dsl.QueryBuilders.bool;
import static org.opensearch.client.opensearch._types.query_dsl.QueryBuilders.nested;
import static org.opensearch.data.client.osc.Queries.termQueryAsQuery;

import org.jetbrains.annotations.NotNull;
import org.opensearch.client.opensearch._types.query_dsl.ChildScoreMode;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.NestedObjectIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {NestedObjectOSCIntegrationTests.Config.class})
public class NestedObjectOSCIntegrationTests extends NestedObjectIntegrationTests {
    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("nestedobject-os");
        }
    }

    @Override
    protected @NotNull Query getNestedQuery1() {
        return NativeQuery.builder().withQuery( //
                nested() //
                        .path("car") //
                        .query(q -> q.bool(b -> b //
                                .must(termQueryAsQuery("car.name", "saturn")) //
                                .must(termQueryAsQuery("car.model", "imprezza")) //
                        )) //
                        .scoreMode(ChildScoreMode.None) //
        )//
                .build();
    }

    @Override
    protected @NotNull Query getNestedQuery2() {
        return NativeQuery.builder().withQuery( //
                bool() //
                        .must(q -> q.nested(n -> n //
                                .path("girlFriends") //
                                .query(termQueryAsQuery("girlFriends.type", "temp")) //
                                .scoreMode(ChildScoreMode.None) //
                            ) //
                        ) //
                        .must(q -> q.nested(n -> n //
                                .path("girlFriends.cars") //
                                .query(termQueryAsQuery("girlFriends.cars.name", "Ford".toLowerCase())) //
                                .scoreMode(ChildScoreMode.None) //
                            ) //
                        ) //
        ) //
            .build();
    }

    @Override
    protected @NotNull Query getNestedQuery3() {
        return NativeQuery.builder().withQuery( //
                nested() //
                        .path("books") //
                        .query(bool().must(termQueryAsQuery("books.name", "java")).build().toQuery() //
                        ) //
                        .scoreMode(ChildScoreMode.None) //
        )//
                .build();
    }

    @Override
    protected @NotNull Query getNestedQuery4() {
        return NativeQuery.builder().withQuery( //
                nested() //
                        .path("buckets") //
                        .query(termQueryAsQuery("buckets.1", "test3")) //
                        .scoreMode(ChildScoreMode.None) //
        )//
                .build();
    }
}
