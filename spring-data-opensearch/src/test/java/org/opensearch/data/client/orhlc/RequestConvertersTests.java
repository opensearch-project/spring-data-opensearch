/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.Request;

/**
 * Unit tests for {@link RequestConverters}.
 */
public class RequestConvertersTests {

    @Test // DATAES-652
    public void shouldNotAddIfSeqNoAndIfPrimaryTermToResultIfInputDoesNotcontainThemWhenConvertingIndexRequest() {
        IndexRequest request = createMinimalIndexRequest();

        Request result = RequestConverters.index(request);

        assertThat(result.getParameters()).doesNotContainKeys("if_seq_no", "if_primary_term");
    }

    private IndexRequest createMinimalIndexRequest() {

        IndexRequest request = new IndexRequest("the-index").id("id");
        request.source(Collections.singletonMap("test", "test"));
        return request;
    }

    @Test // DATAES-652
    public void shouldAddIfSeqNoAndIfPrimaryTermToResultIfInputcontainsThemWhenConvertingIndexRequest() {

        IndexRequest request = createMinimalIndexRequest();
        request.setIfSeqNo(3);
        request.setIfPrimaryTerm(4);

        Request result = RequestConverters.index(request);

        assertThat(result.getParameters()).containsEntry("if_seq_no", "3").containsEntry("if_primary_term", "4");
    }

    @Test // DATAES-652
    public void shouldNotAddIfSeqNoAndIfPrimaryTermToResultIfInputDoesNotcontainThemWhenConvertingDeleteRequest() {

        DeleteRequest request = createMinimalDeleteRequest();

        Request result = RequestConverters.delete(request);

        assertThat(result.getParameters()).doesNotContainKeys("if_seq_no", "if_primary_term");
    }

    private DeleteRequest createMinimalDeleteRequest() {
        return new DeleteRequest("the-index", "id");
    }

    @Test // DATAES-652
    public void shouldAddIfSeqNoAndIfPrimaryTermToResultIfInputcontainsThemWhenConvertingDeleteRequest() {

        DeleteRequest request = createMinimalDeleteRequest();
        request.setIfSeqNo(3);
        request.setIfPrimaryTerm(4);

        Request result = RequestConverters.delete(request);

        assertThat(result.getParameters()).containsEntry("if_seq_no", "3");
        assertThat(result.getParameters()).containsEntry("if_primary_term", "4");
    }
}
