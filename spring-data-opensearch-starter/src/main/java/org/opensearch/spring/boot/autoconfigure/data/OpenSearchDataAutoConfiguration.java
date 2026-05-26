/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data;

import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;
import org.opensearch.data.client.osc.OpenSearchTemplate;
import org.opensearch.spring.boot.autoconfigure.OpenSearchClientAutoConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's OpenSearch support.
 *
 * Adaptation of the {@link org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration} to
 * the needs of OpenSearch.
 */
@AutoConfiguration(after = {OpenSearchClientAutoConfiguration.class, OpenSearchRestClientAutoConfiguration.class})
@ConditionalOnClass({OpenSearchRestTemplate.class, OpenSearchTemplate.class, HealthIndicator.class})
@Import({OpenSearchDataConfiguration.BaseConfiguration.class, OpenSearchDataConfiguration.JavaClientConfiguration.class,
    OpenSearchDataConfiguration.ReactiveRestClientConfiguration.class})
public class OpenSearchDataAutoConfiguration {

    @Bean
    @ConditionalOnBean(ElasticsearchOperations.class)
    @ConditionalOnMissingBean
    OpenSearchHealthIndicator openSearchHealthIndicator(OpenSearchRestTemplate template) {
        return new OpenSearchHealthIndicator(template);
    }
}
