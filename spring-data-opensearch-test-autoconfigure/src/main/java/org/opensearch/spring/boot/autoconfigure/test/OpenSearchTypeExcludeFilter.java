/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.test;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link DataElasticsearchTest}.
 */
class OpenSearchTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<DataOpenSearchTest> {
    OpenSearchTypeExcludeFilter(Class<?> testClass) {
        super(testClass);
    }
}
