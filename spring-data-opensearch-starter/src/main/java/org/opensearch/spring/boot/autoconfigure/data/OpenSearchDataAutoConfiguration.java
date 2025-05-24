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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's OpenSearch support.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration} to
 * the needs of OpenSearch.
 */
@AutoConfiguration(after = {OpenSearchClientAutoConfiguration.class, OpenSearchRestClientAutoConfiguration.class})
@ConditionalOnClass({OpenSearchRestTemplate.class, OpenSearchTemplate.class})
@Import({OpenSearchDataConfiguration.BaseConfiguration.class, OpenSearchDataConfiguration.JavaClientConfiguration.class,
    OpenSearchDataConfiguration.ReactiveRestClientConfiguration.class})
public class OpenSearchDataAutoConfiguration {}
