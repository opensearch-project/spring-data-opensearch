/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.index;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.DynamicTemplates;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

@SpringIntegrationTest
public abstract class DynamicTemplatesContextBaseTests {
    @Autowired protected ElasticsearchOperations operations;
    @Autowired protected IndexNameProvider indexNameProvider;

    @BeforeEach
    void setUp() {
        indexNameProvider.increment();
    }

    @AfterEach
    void cleanup() {
        operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
    }

    @Test
    void shouldCreateDynamicTemplateOne() {
        IndexOperations indexOperations = operations.indexOps(SampleDynamicTemplatesEntity.class);
        assertThat(indexOperations.createWithMapping()).isTrue();

        operations.save(new SampleDynamicTemplatesEntity(Map.of("John", "Smith")));
        assertThat(operations.search(Query.findAll(), SampleDynamicTemplatesEntity.class).get().count()).isEqualTo(1L);
    }

    @Test
    void shouldCreateDynamicTemplateTwo() {
        IndexOperations indexOperations = operations.indexOps(SampleDynamicTemplatesEntityTwo.class);
        assertThat(indexOperations.createWithMapping()).isTrue();

        operations.save(new SampleDynamicTemplatesEntityTwo(Map.of("first.last", "Smith")));
        assertThat(operations.search(Query.findAll(), SampleDynamicTemplatesEntityTwo.class).get().count()).isEqualTo(1L);
    }

    /**
     * @author Petr Kukral
     */
    @Document(indexName = "#{@indexNameProvider.indexName()}")
    @DynamicTemplates(mappingPath = "/mappings/test-dynamic_templates_mappings.json")
    static class SampleDynamicTemplatesEntity {

        @Nullable
        @Id private String id;

        @Nullable
        @Field(type = FieldType.Object) private final Map<String, String> names;

        public SampleDynamicTemplatesEntity() {
            this(new HashMap<>());
        }

        public SampleDynamicTemplatesEntity(final Map<String, String> names) {
            this.names = names;
        }
    }

    /**
     * @author Petr Kukral
     */
    @Document(indexName = "#{@indexNameProvider.indexName()}")
    @DynamicTemplates(mappingPath = "/mappings/test-dynamic_templates_mappings_two.json")
    static class SampleDynamicTemplatesEntityTwo {

        @Nullable
        @Id private String id;

        @Nullable
        @Field(type = FieldType.Object) private final Map<String, String> names;

        public SampleDynamicTemplatesEntityTwo() {
            this(new HashMap<>());
        }

        public SampleDynamicTemplatesEntityTwo(final Map<String, String> names) {
            this.names = names;
        }
    }
}
