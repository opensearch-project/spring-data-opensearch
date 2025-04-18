/*
 * Copyright 2022-2025 the original author or authors.
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
package org.opensearch.data.client.core.mapping;

import org.opensearch.data.client.junit.jupiter.ReactiveOpenSearchTemplateConfiguration;
import org.opensearch.data.client.osc.NativeQuery;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.mapping.ReactiveFieldNamingStrategyIntegrationTests;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ReactiveFieldNamingStrategyOSCIntegrationTests.Config.class })
public class ReactiveFieldNamingStrategyOSCIntegrationTests extends ReactiveFieldNamingStrategyIntegrationTests {

    @Configuration
    static class Config extends ReactiveOpenSearchTemplateConfiguration {
        @Bean
        IndexNameProvider indexNameProvider() {
            return new IndexNameProvider("reactive-fieldnaming-strategy-os");
        }

        @Override
        protected FieldNamingStrategy fieldNamingStrategy() {
            return new SnakeCaseFieldNamingStrategy();
        }
    }

    @Override
    protected Query nativeMatchQuery(String fieldName, String value) {
        return NativeQuery.builder() //
                .withQuery(q -> q.match(mq -> mq.field(fieldName).query(fv -> fv.stringValue(value)))).build();
    }
}
