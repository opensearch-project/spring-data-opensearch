/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import org.opensearch.script.Script;

/**
 * Scripted field
 *
 * @since 0.1
 */
public record ScriptField(String fieldName, Script script) {

}
