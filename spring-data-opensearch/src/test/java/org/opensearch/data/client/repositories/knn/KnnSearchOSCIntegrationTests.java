/*
 * Copyright 2024-present the original author or authors.
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
package org.opensearch.data.client.repositories.knn;

import java.util.List;
import org.opensearch.data.client.junit.jupiter.OpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQueryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.repositories.knn.KnnSearchIntegrationTests;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Haibo Liu
 * @since 5.4
 */
@ContextConfiguration(classes = { KnnSearchOSCIntegrationTests.Config.class })
public class KnnSearchOSCIntegrationTests extends KnnSearchIntegrationTests {

    @Configuration
    @Import({ OpenSearchTemplateConfiguration.class })
    @EnableElasticsearchRepositories(
            basePackages = { "org.springframework.data.elasticsearch.repositories.knn" },
            considerNestedRepositories = true)
    static class Config {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("knn-repository-os");
        }
    }

    @Override
    protected Query getQuery(List<Float> vector) {
        return  new NativeQueryBuilder()
                .withKnnQuery(ksb -> ksb.vector(vector).k(3).field("vector"))
                .withPageable(Pageable.ofSize(2))
                .build();


    }
}
