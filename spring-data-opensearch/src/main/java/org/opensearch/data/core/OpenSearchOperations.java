package org.opensearch.data.core;

import java.util.List;
import org.opensearch.action.search.ListPitInfo;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * The extension over {@link ElasticsearchOperations} with OpenSearch specific operations.
 */
public interface OpenSearchOperations extends ElasticsearchOperations {
    /**
     * Return all active point in time searches
     * @return all active point in time searches
     */
    List<ListPitInfo> listPointInTime();
}
