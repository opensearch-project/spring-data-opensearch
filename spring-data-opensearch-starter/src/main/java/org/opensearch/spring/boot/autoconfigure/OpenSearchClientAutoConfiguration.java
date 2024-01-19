/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.spring.boot.autoconfigure;

import org.opensearch.client.RestClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.spring.boot.autoconfigure.OpenSearchClientConfigurations.JsonpMapperConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchClientConfigurations.OpenSearchClientConfiguration;
import org.opensearch.spring.boot.autoconfigure.OpenSearchClientConfigurations.OpenSearchTransportConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jsonb.JsonbAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenSearch's Java client.
 */
@AutoConfiguration(after = { JsonbAutoConfiguration.class, OpenSearchRestClientAutoConfiguration.class })
@ConditionalOnBean(RestClient.class)
@ConditionalOnClass(OpenSearchClient.class)
@Import({ JsonpMapperConfiguration.class, OpenSearchTransportConfiguration.class, OpenSearchClientConfiguration.class })
public class OpenSearchClientAutoConfiguration {
}
