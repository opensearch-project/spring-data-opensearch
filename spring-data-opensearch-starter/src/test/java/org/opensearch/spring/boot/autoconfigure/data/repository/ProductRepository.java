/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data.repository;

import org.opensearch.spring.boot.autoconfigure.data.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface ProductRepository extends Repository<Product, String> {
    Page<Product> findAll(Pageable pageable);

    Page<Product> findByNameLikeAllIgnoringCase(String name, Pageable pageable);

    Product findByNameAllIgnoringCase(String name, String country);
}
