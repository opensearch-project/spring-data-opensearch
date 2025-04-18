/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data.repository;

import org.opensearch.spring.boot.autoconfigure.data.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Mono;

public interface ReactiveProductRepository extends ReactiveSortingRepository<Product, String> {
    Mono<Page<Product>> findAll(Pageable pageable);

    Mono<Page<Product>> findByNameLikeAllIgnoringCase(String name, Pageable pageable);

    Mono<Product> findByNameAllIgnoringCase(String name, String country);
}
