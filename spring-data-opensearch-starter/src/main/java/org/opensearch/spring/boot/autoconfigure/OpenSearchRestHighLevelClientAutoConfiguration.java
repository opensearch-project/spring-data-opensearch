/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import org.opensearch.client.RestHighLevelClient;
import org.opensearch.spring.boot.autoconfigure.OpenSearchRestHighLevelClientConfigurations.RestHighLevelClientConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OpenSearch REST High Level client.
 */
@AutoConfiguration(after = OpenSearchRestClientAutoConfiguration.class)
@ConditionalOnClass(RestHighLevelClient.class)
@EnableConfigurationProperties(OpenSearchProperties.class)
@Import({RestHighLevelClientConfiguration.class})
public class OpenSearchRestHighLevelClientAutoConfiguration {}
