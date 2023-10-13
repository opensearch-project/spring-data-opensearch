/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.core.index.shard.ShardId;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.index.engine.VersionConflictEngineException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.elasticsearch.junit.jupiter.Tags;

@Tag(Tags.INTEGRATION_TEST)
class OpenSearchExceptionTranslatorIntegrationTests {
    private final OpenSearchExceptionTranslator translator = new OpenSearchExceptionTranslator();

    @Test // DATAES-799
    void shouldConvertOpenSearchStatusExceptionWithSeqNoConflictToOptimisticLockingFailureException() {
        OpenSearchStatusException ex = new OpenSearchStatusException(
                "OpenSearch exception [type=version_conflict_engine_exception, reason=[WPUUsXEB6uuA6j8_A7AB]: version conflict, required seqNo [34], primary term [16]. current document has seqNo [35] and primary term [16]]",
                RestStatus.CONFLICT);

        DataAccessException translated = translator.translateExceptionIfPossible(ex);

        assertThat(translated).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(translated.getMessage()).startsWith("Cannot index a document due to seq_no+primary_term conflict");
        assertThat(translated.getCause()).isSameAs(ex);
    }

    @Test // DATAES-799
    void shouldConvertVersionConflictEngineExceptionWithSeqNoConflictToOptimisticLockingFailureException() {
        VersionConflictEngineException ex = new VersionConflictEngineException(
                new ShardId("index", "uuid", 1),
                "exception-id",
                "OpenSearch exception [type=version_conflict_engine_exception, reason=[WPUUsXEB6uuA6j8_A7AB]: version conflict, required seqNo [34], primary term [16]. current document has seqNo [35] and primary term [16]]");

        DataAccessException translated = translator.translateExceptionIfPossible(ex);

        assertThat(translated).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(translated.getMessage()).startsWith("Cannot index a document due to seq_no+primary_term conflict");
        assertThat(translated.getCause()).isSameAs(ex);
    }
}
