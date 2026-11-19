/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Observation metadata for OpenSearch template operations.
 */
public enum OpenSearchObservation implements ObservationDocumentation {
    OPENSEARCH_COMMAND_OBSERVATION;

    @Override
    public String getName() {
        return "spring.data.opensearch.command";
    }

    @Override
    public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
        return DefaultOpenSearchObservationConvention.class;
    }

    @Override
    public KeyName[] getLowCardinalityKeyNames() {
        return LowCardinalityKeyNames.values();
    }

    @Override
    public KeyName[] getHighCardinalityKeyNames() {
        return HighCardinalityKeyNames.values();
    }

    enum LowCardinalityKeyNames implements KeyName {
        OPERATION("spring.data.operation");

        private final String value;

        LowCardinalityKeyNames(final String value) {
            this.value = value;
        }

        @Override
        public String asString() {
            return value;
        }
    }

    enum HighCardinalityKeyNames implements KeyName {
        COLLECTION("spring.data.collection"),
        BATCH_SIZE("spring.data.batch.size");

        private final String value;

        HighCardinalityKeyNames(final String value) {
            this.value = value;
        }

        @Override
        public String asString() {
            return value;
        }
    }
}
