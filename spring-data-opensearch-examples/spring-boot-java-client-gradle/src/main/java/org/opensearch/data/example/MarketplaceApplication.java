/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;

@SpringBootApplication(exclude = {
    DataElasticsearchAutoConfiguration.class,
    ElasticsearchClientAutoConfiguration.class
})
public class MarketplaceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(MarketplaceConfiguration.class, args);
    }
}
