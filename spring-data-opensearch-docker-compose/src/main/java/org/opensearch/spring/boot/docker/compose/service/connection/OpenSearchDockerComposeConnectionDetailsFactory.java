/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.docker.compose.service.connection;

import java.util.List;
import org.opensearch.spring.boot.autoconfigure.OpenSearchConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

class OpenSearchDockerComposeConnectionDetailsFactory
        extends DockerComposeConnectionDetailsFactory<OpenSearchConnectionDetails> {

    protected OpenSearchDockerComposeConnectionDetailsFactory() {
        super("opensearchproject/opensearch");
    }

    @Override
    protected OpenSearchConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
        return new OpenSearchDockerComposeConnectionDetails(source.getRunningService());
    }

    static class OpenSearchDockerComposeConnectionDetails extends DockerComposeConnectionDetails
            implements OpenSearchConnectionDetails {

        private final String uri;

        private final OpenSearchEnvironment environment;

        protected OpenSearchDockerComposeConnectionDetails(RunningService runningService) {
            super(runningService);
            this.environment = new OpenSearchEnvironment(runningService.env());
            String protocol = this.environment.isSecurityEnabled() ? "https" : "http";
            this.uri = "%s://%s:%d".formatted(protocol, runningService.host(), runningService.ports().get(9200));
        }

        @Override
        public List<String> getUris() {
            return List.of(this.uri);
        }

        @Override
        public String getUsername() {
            return this.environment.isSecurityEnabled() ? this.environment.getUsername() : null;
        }

        @Override
        public String getPassword() {
            return this.environment.isSecurityEnabled() ? this.environment.getPassword() : null;
        }
    }
}
