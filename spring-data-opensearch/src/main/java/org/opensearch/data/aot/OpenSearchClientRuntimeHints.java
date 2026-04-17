/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.aot;

import org.opensearch.client.opensearch._types.mapping.RuntimeFieldType;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.PutMappingRequest;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * Runtime hints for OpenSearch Java client library.
 * `opensearch-java` does not provide its own hints since this is Spring-specific.
 *
 * @since 2.0.6
 */
public class OpenSearchClientRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, final ClassLoader classLoader) {
        // Reflection hints for JSON-B deserializers
        hints.reflection()
            .registerType(TypeReference.of(IndexSettings.class),
                builder -> builder.withField("_DESERIALIZER"))
            .registerType(TypeReference.of(PutMappingRequest.class),
                builder -> builder.withField("_DESERIALIZER"))
            .registerType(TypeReference.of(RuntimeFieldType.class),
                builder -> builder.withField("_DESERIALIZER"))
            .registerType(TypeReference.of(TypeMapping.class),
                builder -> builder.withField("_DESERIALIZER"));

        // Serialization hints for Apache HttpClient 5 (only if it is used)
        if (ClassUtils.isPresent("org.apache.hc.client5.http.impl.auth.BasicScheme", classLoader)) {
            hints.serialization()
                .registerType(org.apache.hc.client5.http.impl.auth.BasicScheme.class)
                .registerType(java.util.HashMap.class);
        }

        // Resource hints
        hints.resources()
            .registerPattern("org/opensearch/client/version.properties");
    }
}
