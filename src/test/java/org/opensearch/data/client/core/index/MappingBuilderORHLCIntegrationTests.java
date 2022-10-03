/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.index;


import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.opensearch.data.client.junit.jupiter.OpensearchRestTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.MappingBuilderIntegrationTests;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {MappingBuilderORHLCIntegrationTests.Config.class})
public class MappingBuilderORHLCIntegrationTests extends MappingBuilderIntegrationTests {

    @Configuration
    @Import({OpensearchRestTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("mappingbuilder-os");
        }
    }

    @Ignore
    @Override
    public void shouldWriteDenseVectorFieldMapping() {
        // see please https://github.com/opensearch-project/OpenSearch/pull/3659
    }

    @Ignore
    @Override
    public void shouldWriteRuntimeFields() {
        // Not supported by Opensearch
    }

    @Ignore
    @Override
    public void shouldWriteWildcardFieldMapping() {
        // Not supported by Opensearch
    }

    @Override
    public void shouldWriteDynamicMapping() {
        IndexOperations indexOps = operations.indexOps(DynamicMappingEntity.class);
        indexOps.createWithMapping();
    }

    @Document(indexName = "#{@indexNameProvider.indexName()}", dynamic = Dynamic.FALSE)
    static class DynamicMappingEntity {

        @Nullable
        @Field(type = FieldType.Object) //
        private Map<String, Object> objectInherit;

        @Nullable
        @Field(type = FieldType.Object, dynamic = Dynamic.FALSE) //
        private Map<String, Object> objectFalse;

        @Nullable
        @Field(type = FieldType.Object, dynamic = Dynamic.STRICT) //
        private Map<String, Object> objectStrict;

        @Nullable
        @Field(type = FieldType.Object, dynamic = Dynamic.RUNTIME) //
        private Map<String, Object> objectRuntime;

        @Nullable
        @Field(type = FieldType.Nested) //
        private List<Map<String, Object>> nestedObjectInherit;

        @Nullable
        @Field(type = FieldType.Nested, dynamic = Dynamic.FALSE) //
        private List<Map<String, Object>> nestedObjectFalse;

        @Nullable
        @Field(type = FieldType.Nested, dynamic = Dynamic.TRUE) //
        private List<Map<String, Object>> nestedObjectTrue;

        @Nullable
        @Field(type = FieldType.Nested, dynamic = Dynamic.STRICT) //
        private List<Map<String, Object>> nestedObjectStrict;

        @Nullable
        @Field(type = FieldType.Nested, dynamic = Dynamic.RUNTIME) //
        private List<Map<String, Object>> nestedObjectRuntime;
    }
}
