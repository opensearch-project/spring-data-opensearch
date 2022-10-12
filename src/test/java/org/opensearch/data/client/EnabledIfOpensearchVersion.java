/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Meta-annotation to enable Opensearch-specific test cases only on or after specific version
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfOpensearchVersionCondition.class)
public @interface EnabledIfOpensearchVersion {
    /**
     * The minimal required version of the Opensearch this test could run on
     */
    String onOrAfter();

    /**
     * The reason (issue reference) why this test is not runnable on previous version
     */
    String reason();
}
