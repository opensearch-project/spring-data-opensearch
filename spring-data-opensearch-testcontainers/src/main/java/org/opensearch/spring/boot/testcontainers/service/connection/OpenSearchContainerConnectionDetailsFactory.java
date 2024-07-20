/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.testcontainers.service.connection;

import java.util.List;
import org.opensearch.spring.boot.autoconfigure.OpenSearchConnectionDetails;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.util.StringUtils;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link OpenSearchConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link OpensearchContainer}.
 */
class OpenSearchContainerConnectionDetailsFactory
    extends ContainerConnectionDetailsFactory<OpensearchContainer<?>, OpenSearchConnectionDetails> {

    @Override
    protected OpenSearchConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<OpensearchContainer<?>> source) {
        return new OpenSearchContainerConnectionDetails(source);
    }

    private static final class OpenSearchContainerConnectionDetails
            extends ContainerConnectionDetails<OpensearchContainer<?>> implements OpenSearchConnectionDetails {

        private OpenSearchContainerConnectionDetails(ContainerConnectionSource<OpensearchContainer<?>> source) {
            super(source);
        }

        @Override
        public List<String> getUris() {
            return List.of(getContainer().getHttpHostAddress());
        }

        @Override
        public String getUsername() {
            return isSecured(getContainer()) ? getContainer().getUsername() : null;
        }

        @Override
        public String getPassword() {
            return getPassword(getContainer());
        }

        private static boolean isSecured(OpensearchContainer<?> container) {
            return container.isSecurityEnabled();
        }

        private static String getPassword(OpensearchContainer<?> container) {
            if (isSecured(container)) {
                String initialAdminPassword = container.getEnvMap().get("OPENSEARCH_INITIAL_ADMIN_PASSWORD");
                if (StringUtils.hasText(initialAdminPassword)) {
                    return initialAdminPassword;
                }
                return container.getPassword();
            }
            return null;
        }
    }
}
