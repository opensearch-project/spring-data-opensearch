/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example.rest;

import java.math.BigDecimal;
import org.opensearch.data.example.model.Product;
import org.opensearch.data.example.repository.ReactiveMarketplaceRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/marketplace")
public class MarketplaceRestController {
    private final ReactiveMarketplaceRepository repository;

    public MarketplaceRestController(ReactiveMarketplaceRepository repository) {
        this.repository = repository;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Product> search(
            @RequestParam(value = "name", required = false, defaultValue = "") String name,
            @RequestParam(value = "price", required = false, defaultValue = "0.0") BigDecimal price) {
        return repository.findByNameLikeAndPriceGreaterThan(name, price);
    }
}
