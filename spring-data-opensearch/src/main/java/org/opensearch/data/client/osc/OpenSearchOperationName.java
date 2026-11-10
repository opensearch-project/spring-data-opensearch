/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.data.client.osc;

/**
 * Operation names used by OpenSearch template observations.
 */
public enum OpenSearchOperationName {
    SAVE("save"),
    INDEX("index"),
    GET("get"),
    MULTI_GET("multiGet"),
    EXISTS("exists"),
    DELETE("delete"),
    DELETE_BY_QUERY("deleteByQuery"),
    BULK("bulk"),
    UPDATE("update"),
    UPDATE_BY_QUERY("updateByQuery"),
    COUNT("count"),
    SEARCH("search"),
    MULTI_SEARCH("multiSearch"),
    SCROLL_START("scrollStart"),
    SCROLL_CONTINUE("scrollContinue"),
    SCROLL_CLEAR("scrollClear"),
    OPEN_POINT_IN_TIME("openPointInTime"),
    CLOSE_POINT_IN_TIME("closePointInTime"),
    LIST_POINT_IN_TIME("listPointInTime"),
    REINDEX("reindex"),
    SUBMIT_REINDEX("submitReindex"),
    PUT_SCRIPT("putScript"),
    GET_SCRIPT("getScript"),
    DELETE_SCRIPT("deleteScript"),
    INDEX_CREATE("indexCreate"),
    INDEX_DELETE("indexDelete"),
    INDEX_EXISTS("indexExists"),
    INDEX_REFRESH("indexRefresh"),
    INDEX_PUT_MAPPING("putMapping"),
    INDEX_GET_MAPPING("getMapping"),
    INDEX_GET_SETTINGS("getSettings"),
    INDEX_ALIAS("alias"),
    INDEX_GET_ALIASES("getAliases"),
    INDEX_PUT_TEMPLATE("putTemplate"),
    INDEX_GET_TEMPLATE("getTemplate"),
    INDEX_EXISTS_TEMPLATE("existsTemplate"),
    INDEX_DELETE_TEMPLATE("deleteTemplate"),
    INDEX_PUT_INDEX_TEMPLATE("putIndexTemplate"),
    INDEX_GET_INDEX_TEMPLATE("getIndexTemplate"),
    INDEX_EXISTS_INDEX_TEMPLATE("existsIndexTemplate"),
    INDEX_DELETE_INDEX_TEMPLATE("deleteIndexTemplate"),
    INDEX_PUT_COMPONENT_TEMPLATE("putComponentTemplate"),
    INDEX_GET_COMPONENT_TEMPLATE("getComponentTemplate"),
    INDEX_EXISTS_COMPONENT_TEMPLATE("existsComponentTemplate"),
    INDEX_DELETE_COMPONENT_TEMPLATE("deleteComponentTemplate"),
    INDEX_GET_INFORMATION("getIndexInformation"),
    CLUSTER_HEALTH("clusterHealth"),
    CLUSTER_INFO("clusterInfo");

    private final String value;

    OpenSearchOperationName(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
