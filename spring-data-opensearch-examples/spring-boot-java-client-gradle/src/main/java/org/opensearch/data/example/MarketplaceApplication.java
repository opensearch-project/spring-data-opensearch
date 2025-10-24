/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.data.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.data.elasticsearch.autoconfigure.DataElasticsearchAutoConfiguration;

@SpringBootApplication(exclude = DataElasticsearchAutoConfiguration.class)
public class MarketplaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketplaceConfiguration.class, args);
    }
}
