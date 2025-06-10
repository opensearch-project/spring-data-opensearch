/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example.repository;

import java.math.BigDecimal;
import org.opensearch.data.example.model.Product;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * See please https://github.com/spring-projects/spring-data-elasticsearch/blob/main/src/main/asciidoc/reference/elasticsearch-repository-queries.adoc
 */
@Repository
public interface ReactiveMarketplaceRepository extends ReactiveElasticsearchRepository<Product, String> {
    Flux<Product> findByNameLikeAndPriceGreaterThan(String name, BigDecimal price);
}
