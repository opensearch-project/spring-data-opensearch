/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.opensearch.client.RestClientBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link org.opensearch.client.RestClient} via a {@link RestClientBuilder} whilst
 * retaining default auto-configuration.
 *
 * Adaptation of the {@link org.springframework.boot.elasticsearch.autoconfigure.Rest5ClientBuilderCustomizer} to
 * the needs of OpenSearch.
 */
@FunctionalInterface
public interface RestClientBuilderCustomizer {
    /**
     * Customize the {@link RestClientBuilder}.
     * <p>
     * Possibly overrides customizations made with the {@code "opensearch.rest"}
     * configuration properties namespace. For more targeted changes, see
     * {@link #customize(HttpAsyncClientBuilder)} and
     * {@link #customize(RequestConfig.Builder)}.
     * @param builder the builder to customize
     */
    void customize(RestClientBuilder builder);

    /**
     * Customize the {@link HttpAsyncClientBuilder}.
     * @param builder the builder
     */
    default void customize(HttpAsyncClientBuilder builder) {}

    /**
     * Customize the {@link Builder}.
     * @param builder the builder
     */
    default void customize(Builder builder) {}
}
