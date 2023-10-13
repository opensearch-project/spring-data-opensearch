/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.get.MultiGetItemResponse;
import org.opensearch.action.get.MultiGetRequest;
import org.opensearch.action.get.MultiGetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.MultiSearchRequest;
import org.opensearch.action.search.MultiSearchResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.core.common.bytes.BytesArray;
import org.opensearch.data.client.orhlc.OpenSearchRestTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenSearchRestTemplateCallbackTests extends OpenSearchTemplateCallbackTests {

    @Mock
    private RestHighLevelClient client;

    @Mock
    private IndexResponse indexResponse;

    @Mock
    private BulkResponse bulkResponse;

    @Mock
    private BulkItemResponse bulkItemResponse;

    @Mock
    private GetResponse getResponse;

    @Mock
    private MultiGetResponse multiGetResponse;

    @Mock
    private MultiGetItemResponse multiGetItemResponse;

    @Mock
    private MultiSearchResponse.Item multiSearchResponseItem;

    @SuppressWarnings("deprecation") // we know what we test
    @BeforeEach
    public void setUp() throws Exception {
        initTemplate(new OpenSearchRestTemplate(client));

        doReturn(indexResponse).when(client).index(any(IndexRequest.class), any(RequestOptions.class));
        doReturn("response-id").when(indexResponse).getId();

        doReturn(bulkResponse).when(client).bulk(any(BulkRequest.class), any(RequestOptions.class));
        doReturn(new BulkItemResponse[] {bulkItemResponse, bulkItemResponse})
                .when(bulkResponse)
                .getItems();
        doReturn("response-id").when(bulkItemResponse).getId();

        doReturn(getResponse).when(client).get(any(GetRequest.class), any(RequestOptions.class));

        doReturn(true).when(getResponse).isExists();
        doReturn(false).when(getResponse).isSourceEmpty();
        doReturn(new HashMap<String, Object>() {
                    {
                        put("id", "init");
                        put("firstname", "luke");
                    }
                })
                .when(getResponse)
                .getSourceAsMap();

        doReturn(multiGetResponse).when(client).mget(any(MultiGetRequest.class), any(RequestOptions.class));
        doReturn(new MultiGetItemResponse[] {multiGetItemResponse, multiGetItemResponse})
                .when(multiGetResponse)
                .getResponses();
        doReturn(getResponse).when(multiGetItemResponse).getResponse();

        doReturn(searchResponse).when(client).search(any(SearchRequest.class), any(RequestOptions.class));
        doReturn(nSearchHits(2)).when(searchResponse).getHits();
        doReturn("scroll-id").when(searchResponse).getScrollId();
        doReturn(new BytesArray(new byte[8])).when(searchHit).getSourceRef();
        doReturn(new HashMap<String, Object>() {
                    {
                        put("id", "init");
                        put("firstname", "luke");
                    }
                })
                .when(searchHit)
                .getSourceAsMap();

        MultiSearchResponse multiSearchResponse =
                new MultiSearchResponse(new MultiSearchResponse.Item[] {multiSearchResponseItem}, 1L);
        doReturn(multiSearchResponse).when(client).multiSearch(any(MultiSearchRequest.class), any());
        doReturn(multiSearchResponse).when(client).msearch(any(MultiSearchRequest.class), any());
        doReturn(searchResponse).when(multiSearchResponseItem).getResponse();

        doReturn(searchResponse).when(client).scroll(any(SearchScrollRequest.class), any(RequestOptions.class));
    }
}
