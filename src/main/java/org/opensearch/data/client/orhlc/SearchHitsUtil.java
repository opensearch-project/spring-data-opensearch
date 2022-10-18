/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;


import org.opensearch.search.SearchHits;

/**
 * Utility class to prevent leaking of Lucene API into Spring Data OpenSearch.
 * @since 0.1
 */
public final class SearchHitsUtil {
    private SearchHitsUtil() {}

    public static long getTotalCount(SearchHits searchHits) {
        return searchHits.getTotalHits().value;
    }
}
