/*
 * Copyright 2021-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.data.client.osc;

import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;

/**
 * Implementation of the {@link ClusterOperations} interface using en {@link OpenSearchClusterClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class ClusterTemplate extends ChildTemplate<OpenSearchTransport, OpenSearchClusterClient>
        implements ClusterOperations {

    public ClusterTemplate(OpenSearchClusterClient client, ElasticsearchConverter elasticsearchConverter) {
        super(client, elasticsearchConverter);
    }

    @Override
    public ClusterHealth health() {

        HealthRequest healthRequest = requestConverter.clusterHealthRequest();
        HealthResponse healthResponse = execute(client -> client.health(healthRequest));
        return responseConverter.clusterHealth(healthResponse);
    }

}
