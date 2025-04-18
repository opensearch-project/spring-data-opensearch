/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data.entity;

import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveProductOpenSearchRepository extends ReactiveElasticsearchRepository<Product, String> {
    Flux<Product> findAll(Sort sort);

    Flux<Product> findByNameLikeAllIgnoringCase(String name, Sort sort);

    Mono<Product> findByNameAllIgnoringCase(String name, String country);
}
