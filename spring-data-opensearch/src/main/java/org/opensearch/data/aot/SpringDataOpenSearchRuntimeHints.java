/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.aot;

import static org.springframework.data.elasticsearch.aot.ElasticsearchAotPredicates.isReactorPresent;

import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import org.opensearch.data.client.osc.EntityAsMap;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.elasticsearch.core.event.AfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.AfterLoadCallback;
import org.springframework.data.elasticsearch.core.event.AfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.BeforeConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterConvertCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterLoadCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveAfterSaveCallback;
import org.springframework.data.elasticsearch.core.event.ReactiveBeforeConvertCallback;

/**
 * Runtime hints for Spring Data OpenSearch core functionality.
 * Registers Spring Data callbacks and entity mapping classes.
 *
 * @since 2.0.6
 */
public class SpringDataOpenSearchRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, @Nullable final ClassLoader classLoader) {
        // Register core callbacks (always present)
        hints.reflection().registerTypes(
            Arrays.asList(
                TypeReference.of(AfterConvertCallback.class),
                TypeReference.of(AfterLoadCallback.class),
                TypeReference.of(AfterSaveCallback.class),
                TypeReference.of(BeforeConvertCallback.class),
                TypeReference.of(EntityAsMap.class)
            ),
            builder -> builder.withMembers(
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
            )
        );

        // Register reactive callbacks (conditional on reactor presence)
        if (isReactorPresent()) {
            hints.reflection().registerTypes(
                Arrays.asList(
                    TypeReference.of(ReactiveAfterConvertCallback.class),
                    TypeReference.of(ReactiveAfterLoadCallback.class),
                    TypeReference.of(ReactiveAfterSaveCallback.class),
                    TypeReference.of(ReactiveBeforeConvertCallback.class)
                ),
                builder -> builder.withMembers(
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS
                )
            );
        }
    }
}
