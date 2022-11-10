/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties.Restclient;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenSearch client configuration properties.
 *
 * Adaptation of the {@link org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties} to
 * the needs of OpenSearch.
 */
@ConfigurationProperties("opensearch")
public class OpenSearchProperties {
    /**
     * Comma-separated list of the OpenSearch instances to use.
     */
    private List<String> uris = new ArrayList<>(Collections.singletonList("http://localhost:9200"));

    /**
     * Username for authentication with OpenSearch.
     */
    private String username;

    /**
     * Password for authentication with OpenSearch.
     */
    private String password;

    /**
     * Connection timeout used when communicating with OpenSearch.
     */
    private Duration connectionTimeout = Duration.ofSeconds(1);

    /**
     * Socket timeout used when communicating with OpenSearch.
     */
    private Duration socketTimeout = Duration.ofSeconds(30);

    /**
     * Whether to enable socket keep alive between client and OpenSearch.
     */
    private boolean socketKeepAlive = false;

    /**
     * Prefix added to the path of every request sent to OpenSearch.
     */
    private String pathPrefix;

    private final Restclient restclient = new Restclient();

    public List<String> getUris() {
        return this.uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Duration getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Duration getSocketTimeout() {
        return this.socketTimeout;
    }

    public void setSocketTimeout(Duration socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public boolean isSocketKeepAlive() {
        return this.socketKeepAlive;
    }

    public void setSocketKeepAlive(boolean socketKeepAlive) {
        this.socketKeepAlive = socketKeepAlive;
    }

    public String getPathPrefix() {
        return this.pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public Restclient getRestclient() {
        return this.restclient;
    }
}
