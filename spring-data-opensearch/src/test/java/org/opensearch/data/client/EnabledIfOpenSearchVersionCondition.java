/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client;

import java.lang.reflect.AnnotatedElement;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opensearch.Version;
import org.springframework.data.elasticsearch.junit.jupiter.ClusterConnection;

public class EnabledIfOpenSearchVersionCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final AnnotatedElement element = context.getElement().orElseThrow(IllegalStateException::new);

        final EnabledIfOpenSearchVersion annotation = element.getAnnotation(EnabledIfOpenSearchVersion.class);
        if (annotation == null) {
            return ConditionEvaluationResult.enabled("@EnabledIfOpenSearchVersion is not present");
        }

        final Version onOrAfter = Version.fromString(annotation.onOrAfter());
        final String image = ClusterConnection.clusterConnectionInfo()
                .getOpensearchContainer()
                .getDockerImageName();
        final int index = image.lastIndexOf(":");
        if (index > 0) {
            final Version current = Version.fromString(image.substring(index + 1));
            if (current.onOrAfter(onOrAfter)) {
                return ConditionEvaluationResult.enabled(
                        "The OpenSearch version " + current + " is on or after " + onOrAfter);
            } else {
                return ConditionEvaluationResult.disabled(
                        "The OpenSearch version " + current + " is before " + onOrAfter);
            }
        } else {
            return ConditionEvaluationResult.enabled(
                    "Unable to extract OpenSearch version from Docker image: " + image);
        }
    }
}
