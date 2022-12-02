/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.test;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

class DataOpenSearchTestContextBootstrapper extends SpringBootTestContextBootstrapper {
    @Override
    protected String[] getProperties(Class<?> testClass) {
        return MergedAnnotations.from(testClass, SearchStrategy.INHERITED_ANNOTATIONS)
                .get(DataOpenSearchTest.class)
                .getValue("properties", String[].class)
                .orElse(null);
    }
}
