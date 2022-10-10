/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client;

import static org.opensearch.index.query.QueryBuilders.*;

import org.apache.lucene.search.join.ScoreMode;
import org.jetbrains.annotations.NotNull;
import org.opensearch.data.client.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.NestedObjectIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {NestedObjectORHLCIntegrationTests.Config.class})
public class NestedObjectORHLCIntegrationTests extends NestedObjectIntegrationTests {
    @Configuration
    @Import({OpensearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("nestedobject-os");
        }
    }

    @NotNull
    protected Query getNestedQuery1() {
        return new NativeSearchQueryBuilder()
                .withQuery( //
                        nestedQuery(
                                "car", //
                                boolQuery() //
                                        .must(termQuery("car.name", "saturn")) //
                                        .must(termQuery("car.model", "imprezza")), //
                                ScoreMode.None)) //
                .build();
    }

    @NotNull
    protected Query getNestedQuery2() {
        return new NativeSearchQueryBuilder()
                .withQuery( //
                        boolQuery() //
                                .must(nestedQuery(
                                        "girlFriends", //
                                        termQuery("girlFriends.type", "temp"), //
                                        ScoreMode.None)) //
                                .must(nestedQuery(
                                        "girlFriends.cars", //
                                        termQuery("girlFriends.cars.name", "Ford".toLowerCase()), //
                                        ScoreMode.None))) //
                .build();
    }

    @NotNull
    protected Query getNestedQuery3() {
        return new NativeSearchQueryBuilder()
                .withQuery( //
                        nestedQuery(
                                "books", //
                                boolQuery() //
                                        .must(termQuery("books.name", "java")), //
                                ScoreMode.None)) //
                .build();
    }

    @NotNull
    protected Query getNestedQuery4() {
        return new NativeSearchQueryBuilder()
                .withQuery( //
                        nestedQuery(
                                "buckets", //
                                termQuery("buckets.1", "test3"), //
                                ScoreMode.None)) //
                .build();
    }
}
