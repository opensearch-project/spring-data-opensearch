/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example.repository;

import java.math.BigDecimal;
import java.util.List;
import org.opensearch.data.example.model.Product;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * See please https://github.com/spring-projects/spring-data-elasticsearch/blob/main/src/main/asciidoc/reference/elasticsearch-repository-queries.adoc
 */
@Repository
public interface MarketplaceRepository extends ElasticsearchRepository<Product, String> {
    List<Product> findByNameLikeAndPriceGreaterThan(String name, BigDecimal price);
}
