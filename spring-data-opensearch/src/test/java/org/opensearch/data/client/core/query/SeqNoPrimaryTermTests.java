/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.core.query;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.opensearch.index.seqno.SequenceNumbers;
import org.springframework.data.elasticsearch.core.query.SeqNoPrimaryTerm;

class SeqNoPrimaryTermTests {
    @Test
    void shouldConstructInstanceWithAssignedSeqNoAndPrimaryTerm() {
        SeqNoPrimaryTerm instance = new SeqNoPrimaryTerm(1, 2);

        assertThat(instance.sequenceNumber()).isEqualTo(1);
        assertThat(instance.primaryTerm()).isEqualTo(2);
    }

    @Test
    void shouldThrowAnExceptionWhenTryingToConstructWithUnassignedSeqNo() {
        assertThatThrownBy(() -> new SeqNoPrimaryTerm(SequenceNumbers.UNASSIGNED_SEQ_NO, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowAnExceptionWhenTryingToConstructWithSeqNoForNoOpsPerformed() {
        assertThatThrownBy(() -> new SeqNoPrimaryTerm(SequenceNumbers.NO_OPS_PERFORMED, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowAnExceptionWhenTryingToConstructWithUnassignedPrimaryTerm() {
        assertThatThrownBy(() -> new SeqNoPrimaryTerm(1, SequenceNumbers.UNASSIGNED_PRIMARY_TERM))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
