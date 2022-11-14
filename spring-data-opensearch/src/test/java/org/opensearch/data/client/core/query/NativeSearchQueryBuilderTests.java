/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.query;

import static org.assertj.core.api.Assertions.*;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.orhlc.NativeSearchQuery;
import org.opensearch.data.client.orhlc.NativeSearchQueryBuilder;

public class NativeSearchQueryBuilderTests {

    @Test // #2105
    void shouldContainEffectiveSearchAfterValue() {
        Long lastSortValue = 1L;
        List<Object> searchAfter = Lists.newArrayList(lastSortValue);

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withSearchAfter(searchAfter);
        NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        assertThat(nativeSearchQuery.getSearchAfter()).isNotNull();
    }

    @Test // #2105
    void shouldIgnoreNullableSearchAfterValue() {
        List<Object> emptySearchValueByFirstSearch = null;
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withSearchAfter(emptySearchValueByFirstSearch);
        NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        assertThat(nativeSearchQuery.getSearchAfter()).isNull();
    }

    @Test // #2105
    void shouldIgnoreEmptySearchAfterValue() {
        List<Object> emptySearchValueByFirstSearch = Lists.newArrayList();
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withSearchAfter(emptySearchValueByFirstSearch);
        NativeSearchQuery nativeSearchQuery = nativeSearchQueryBuilder.build();

        assertThat(nativeSearchQuery.getSearchAfter()).isNull();
    }
}
