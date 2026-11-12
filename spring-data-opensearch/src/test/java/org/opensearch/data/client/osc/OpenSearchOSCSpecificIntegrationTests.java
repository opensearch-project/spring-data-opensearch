/*
 * Copyright 2022-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.data.client.osc;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.core.OpenSearchSpecificIntegrationTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterListener;
import org.springframework.data.elasticsearch.core.bulk.BulkIngesterOptions;
import org.springframework.data.elasticsearch.core.bulk.BulkOperationResult;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Farid Faoudi
 * @author Sascha Woo
 * @since 4.4
 */
@ContextConfiguration(classes = { OpenSearchOSCSpecificIntegrationTests.Config.class })
@DisplayName("Using OpenSearch Client")
public class OpenSearchOSCSpecificIntegrationTests extends OpenSearchSpecificIntegrationTests {

    @Configuration
    @Import({ OpenSearchTemplateConfiguration.class })
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("integration-specific-os");
        }
    }

    @Override
    protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
        return Queries.getBuilderWithMatchAllQuery();
    }

    @Test
    void shouldIngestMixedOperationsAndReportItemFailures() {
        operations.save(SampleEntity.builder().id("update").message("old").build());
        operations.save(SampleEntity.builder().id("delete").message("obsolete").build());
        final var entity = SampleEntity.builder().message("new").build();
        final var results = new CopyOnWriteArrayList<BulkOperationResult<String>>();
        final var failures = new CopyOnWriteArrayList<RuntimeException>();
        final var listener = new BulkIngesterListener<String>() {
            @Override
            public void afterBulk(final long executionId, final List<BulkOperationResult<String>> batch) {
                results.addAll(batch);
            }

            @Override
            public void afterBulk(final long executionId, final List<String> contexts, final RuntimeException failure) {
                failures.add(failure);
            }
        };
        final var options = BulkIngesterOptions.builder().withMaxOperations(2).build();
        final var ingester = operations.bulkIngester(options, SampleEntity.class, listener);

        try (ingester) {
            ingester.add(new IndexQueryBuilder().withObject(entity).build(), "index");
            ingester.add(new IndexQueryBuilder().withId("update").withSource("{}")
                    .withOpType(IndexQuery.OpType.CREATE).build(), "conflict");
            ingester.add(UpdateQuery.builder("update")
                    .withDocument(org.springframework.data.elasticsearch.core.document.Document.from(Map.of("message", "updated")))
                    .build(), "update");
            ingester.addDelete("delete", null, "delete");
        }

        assertThat(failures).isEmpty();
        assertThat(results).hasSize(4);
        assertThat(results.stream().filter(BulkOperationResult::isSuccessful))
                .extracting(BulkOperationResult::context).containsExactlyInAnyOrder("index", "update", "delete");
        assertThat(results.stream().filter(result -> !result.isSuccessful())).singleElement().satisfies(result -> {
            assertThat(result.context()).isEqualTo("conflict");
            assertThat(result.status()).isEqualTo(409);
        });
        assertThat(entity.getId()).isNotBlank();
        assertThat(entity.getVersion()).isNotNull();
        assertThat(operations.get(entity.getId(), SampleEntity.class).getMessage()).isEqualTo("new");
        assertThat(operations.get("update", SampleEntity.class).getMessage()).isEqualTo("updated");
        assertThat(operations.exists("delete", SampleEntity.class)).isFalse();
        assertThat(ingester.getOperationsCount()).isEqualTo(4);
        assertThat(ingester.getRequestCount()).isEqualTo(2);
    }
}
