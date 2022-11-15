/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure.data.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.lang.Nullable;

@Document(indexName = "products")
@Setting(shards = 1, replicas = 0, refreshInterval = "-1")
public class Product {
    @Nullable
    @Id
    private String name;

    public Product(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }
}
