/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import org.opensearch.client.RestClientBuilder;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientConfigurations.RestClientBuilderConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientConfigurations.RestClientConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientConfigurations.RestClientSnifferConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenSearch REST clients.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration} to
 * the needs of OpenSearch.
 */
@AutoConfiguration
@ConditionalOnClass(RestClientBuilder.class)
@EnableConfigurationProperties(OpenSearchProperties.class)
@Import({RestClientBuilderConfiguration.class, RestClientConfiguration.class, RestClientSnifferConfiguration.class})
public class OpenSearchRestClientAutoConfiguration {}
