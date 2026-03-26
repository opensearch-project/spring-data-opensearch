/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.aot;

import static org.springframework.data.elasticsearch.aot.ElasticsearchAotPredicates.isReactorPresent;

import java.util.Arrays;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * Runtime hints for Spring Data OpenSearch repository implementations.
 *
 * @since 3.0.4
 */
public class RepositoryRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(final RuntimeHints hints, @Nullable final ClassLoader classLoader) {
        // Register blocking repository implementation
        hints.reflection().registerTypes(
            Arrays.asList(
                TypeReference.of("org.springframework.data.elasticsearch.repository.support.SimpleElasticsearchRepository")
            ),
            builder -> builder.withMembers(
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
            )
        );

        // Register reactive repository implementation (conditional on reactor presence)
        if (isReactorPresent()) {
            hints.reflection().registerTypes(
                Arrays.asList(
                    TypeReference.of("org.springframework.data.elasticsearch.repository.support.SimpleReactiveElasticsearchRepository")
                ),
                builder -> builder.withMembers(
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS
                )
            );
        }
    }
}
