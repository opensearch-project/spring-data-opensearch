package org.opensearch.data.core;

import java.time.Duration;
import java.util.List;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * The extension over {@link ElasticsearchOperations} with OpenSearch specific operations.
 */
public interface OpenSearchOperations extends ElasticsearchOperations {
    /**
     * Return all active point in time searches
     * @return all active point in time searches
     */
    List<PitInfo> listPointInTime();

    /**
     * Describes the point in time entry
     *
     * @param id the point in time id
     * @param creationTime the time this point in time was created
     * @param keepAlive the new keep alive value for this point in time
     */
    record PitInfo(String id, long creationTime, Duration keepAlive) {
    }
}
