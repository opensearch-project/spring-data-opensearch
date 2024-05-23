/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opensearch.OpenSearchStatusException;
import org.opensearch.client.ResponseException;
import org.opensearch.common.ValidationException;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.index.engine.VersionConflictEngineException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.VersionConflictException;

/**
 * Simple {@link PersistenceExceptionTranslator} for OpenSearch. Convert the given runtime exception to an
 * appropriate exception from the {@code org.springframework.dao} hierarchy. Return {@literal null} if no translation is
 * appropriate: any other exception may have resulted from user code, and should not be translated.
 * @since 0.1
 */
public class OpenSearchExceptionTranslator implements PersistenceExceptionTranslator {
    /**
     * translates an Exception if possible. Exceptions that are no {@link RuntimeException}s are wrapped in a
     * RuntimeException
     *
     * @param throwable the Exception to map
     * @return the potentially translated RuntimeException.
     */
    public RuntimeException translateException(Throwable throwable) {

        RuntimeException runtimeException = throwable instanceof RuntimeException
                ? (RuntimeException) throwable
                : new RuntimeException(throwable.getMessage(), throwable);
        RuntimeException potentiallyTranslatedException = translateExceptionIfPossible(runtimeException);

        return potentiallyTranslatedException != null ? potentiallyTranslatedException : runtimeException;
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

        if (isSeqNoConflict(ex)) {
            return new OptimisticLockingFailureException(
                    "Cannot index a document due to seq_no+primary_term conflict", ex);
        }

        if (ex instanceof OpenSearchStatusException) {
            OpenSearchStatusException statusException = (OpenSearchStatusException) ex;

            if (statusException.status() == RestStatus.NOT_FOUND) {
                if (statusException.getMessage().contains("index_not_found_exception")) {
                    Pattern pattern = Pattern.compile(".*no such index \\[(.*)\\]");
                    String index = "";
                    Matcher matcher = pattern.matcher(statusException.getMessage());
                    if (matcher.matches()) {
                        index = matcher.group(1);
                    }

                    return new NoSuchIndexException(index);
                } else {
                    return new ResourceNotFoundException(statusException.getMessage());
                }
            }

            if (statusException.getMessage().contains("validation_exception")) {
                return new DataIntegrityViolationException(statusException.getMessage());
            }

            if (statusException.status() != null && statusException.getMessage() != null) {
                final Integer status = statusException.status().getStatus();
                final String message = statusException.getMessage();

                if (status == 409 && message.contains("type=version_conflict_engine_exception")) {
                    if (message.contains("version conflict, current version [")) {
                        throw new VersionConflictException("Version conflict", statusException);
                    }
                }
            }

            return new UncategorizedElasticsearchException(
                    ex.getMessage(), statusException.status().getStatus(), null, ex);
        }

        if (ex instanceof ValidationException) {
            return new DataIntegrityViolationException(ex.getMessage(), ex);
        }

        Throwable cause = ex.getCause();
        if (cause instanceof IOException) {
            return new DataAccessResourceFailureException(ex.getMessage(), ex);
        }

        return null;
    }

    private boolean isSeqNoConflict(Throwable exception) {
        Integer status = null;
        String message = null;

        if (exception instanceof ResponseException) {
            ResponseException responseException = (ResponseException) exception;
            status = responseException.getResponse().getStatusLine().getStatusCode();
            message = responseException.getMessage();
        } else if (exception instanceof OpenSearchStatusException) {
            OpenSearchStatusException statusException = (OpenSearchStatusException) exception;
            status = statusException.status().getStatus();
            message = statusException.getMessage();
        } else if (exception.getCause() != null) {
            return isSeqNoConflict(exception.getCause());
        }

        if (status != null && message != null) {
            return status == 409
                    && message.contains("type=version_conflict_engine_exception")
                    && message.contains("version conflict, required seqNo");
        }

        if (exception instanceof VersionConflictEngineException) {

            VersionConflictEngineException versionConflictEngineException = (VersionConflictEngineException) exception;

            return versionConflictEngineException.getMessage() != null
                    && versionConflictEngineException.getMessage().contains("version conflict, required seqNo");
        }

        return false;
    }
}
