/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.indices;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.TemplateResponse;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

@SpringIntegrationTest
@ContextConfiguration(classes = {IndexTemplateOperationsOSCIntegrationTests.Config.class})
public class IndexTemplateOperationsOSCIntegrationTests {

    @Configuration
    @Import({OpenSearchTemplateConfiguration.class})
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("indextemplateoperations-es7");
        }
    }

    @Autowired private ElasticsearchOperations operations;
    private IndexOperations indexOperations;

    @Autowired protected IndexNameProvider indexNameProvider;
    private String indexTemplateName;

    @BeforeEach
    void setUp() {
        indexNameProvider.increment();
        indexTemplateName = indexNameProvider.indexName() + "-template";

        indexOperations = operations.indexOps(Entity.class);
    }

    @AfterEach
    void cleanup() {
        indexOperations.deleteIndexTemplate(indexTemplateName);
    }

    @Test
    @DisplayName("should return TemplateResponseData with getIndexTemplate method")
    void shouldReturnIndexTemplate() {
        var mapping = indexOperations.createMapping();

        PutIndexTemplateRequest putIndexTemplateRequest = PutIndexTemplateRequest.builder()
                .withName(indexTemplateName)
                .withIndexPatterns(indexNameProvider.getPrefix() + "-*")
                .withMapping(mapping)
                .build();
        indexOperations.putIndexTemplate(putIndexTemplateRequest);

        List<TemplateResponse> templateResponses = indexOperations.getIndexTemplate(indexTemplateName);
        assertThat(templateResponses).hasSize(1);

        TemplateResponse templateResponse = templateResponses.getFirst();

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(templateResponse).isNotNull();
        softly.assertThat(templateResponse.name()).isEqualTo(indexTemplateName);
        softly.assertThat(templateResponse.templateData()).isNotNull();
        softly.assertThat(templateResponse.templateData().mapping()).isEqualTo(mapping);
    }

    @Document(indexName = "#{@indexNameProvider.indexName()}")
    protected static class Entity {
        @Nullable
        private @Id String id;

        @Nullable
        public String getId() {
            return id;
        }

        public void setId(@Nullable String id) {
            this.id = id;
        }
    }
}
