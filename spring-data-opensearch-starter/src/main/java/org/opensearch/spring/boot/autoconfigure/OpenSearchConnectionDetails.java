/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import java.util.List;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface OpenSearchConnectionDetails extends ConnectionDetails {

    List<String> getUris();

    String getUsername();

    String getPassword();

    default String getPathPrefix() {
        return null;
    }

}
