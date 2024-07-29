/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.docker.compose.service.connection;

import java.util.Map;

class OpenSearchEnvironment {

    private final String username = "admin";

    private final boolean securityEnabled;

    private final String password;

    OpenSearchEnvironment(Map<String, String> env) {
        this.securityEnabled = !Boolean.parseBoolean(env.get("DISABLE_SECURITY_PLUGIN"));
        this.password = env.getOrDefault("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "admin");
    }

    public String getUsername() {
        return this.username;
    }

    public boolean isSecurityEnabled() {
        return this.securityEnabled;
    }

    public String getPassword() {
        return this.password;
    }
}
