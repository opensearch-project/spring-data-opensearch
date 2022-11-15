/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.spring.boot.autoconfigure;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Tag;
import org.testcontainers.utility.DockerImageName;

@Tag("integration-test")
public abstract class AbstractOpenSearchIntegrationTest {
    private static final Log LOGGER = LogFactory.getLog(OpenSearchRestClientAutoConfigurationIntegrationTests.class);

    private static final String SDE_TESTCONTAINER_IMAGE_NAME = "sde.testcontainers.image-name";
    private static final String SDE_TESTCONTAINER_IMAGE_VERSION = "sde.testcontainers.image-version";

    protected static DockerImageName getDockerImageName() {
        return getDockerImageName(testcontainersProperties());
    }

    protected static String getOpenSearchVersion() {
        return testcontainersProperties().get(SDE_TESTCONTAINER_IMAGE_VERSION);
    }

    private static DockerImageName getDockerImageName(Map<String, String> testcontainersProperties) {
        final String imageName = testcontainersProperties.get(SDE_TESTCONTAINER_IMAGE_NAME);
        final String imageVersion = testcontainersProperties.get(SDE_TESTCONTAINER_IMAGE_VERSION);

        if (imageName == null) {
            throw new IllegalArgumentException("property " + SDE_TESTCONTAINER_IMAGE_NAME + " not configured");
        }
        testcontainersProperties.remove(SDE_TESTCONTAINER_IMAGE_NAME);

        if (imageVersion == null) {
            throw new IllegalArgumentException("property " + SDE_TESTCONTAINER_IMAGE_VERSION + " not configured");
        }
        testcontainersProperties.remove(SDE_TESTCONTAINER_IMAGE_VERSION);

        String configuredImageName = imageName + ':' + imageVersion;
        DockerImageName dockerImageName = DockerImageName.parse(configuredImageName);

        LOGGER.info("Docker image: " + dockerImageName);
        return dockerImageName;
    }

    private static Map<String, String> testcontainersProperties() {
        final String propertiesFile = "testcontainers-opensearch.properties";
        LOGGER.info("load configuration from " + propertiesFile);

        try (InputStream inputStream = OpenSearchRestClientAutoConfigurationIntegrationTests.class
                .getClassLoader()
                .getResourceAsStream(propertiesFile)) {
            final Properties props = new Properties();

            if (inputStream != null) {
                props.load(inputStream);
            }

            Map<String, String> properties = new LinkedHashMap<>();
            props.forEach((key, value) -> properties.put(key.toString(), value.toString()));
            return properties;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
