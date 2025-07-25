/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.get.MultiGetItemResponse;
import org.opensearch.action.get.MultiGetResponse;
import org.opensearch.client.indices.GetIndexResponse;
import org.opensearch.client.indices.GetIndexTemplatesResponse;
import org.opensearch.client.indices.IndexTemplateMetadata;
import org.opensearch.cluster.metadata.AliasMetadata;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.common.compress.CompressedXContent;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.ScrollableHitSource;
import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.index.TemplateResponse;
import org.springframework.data.elasticsearch.core.index.TemplateResponseData;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.reindex.ReindexResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Factory class to convert OpenSearch responses to different type of data classes.
 * @since 0.1
 */
public class ResponseConverter {
    private ResponseConverter() {}

    // region alias

    public static Map<String, Set<AliasData>> aliasDatas(Map<String, Set<AliasMetadata>> aliasesMetadatas) {
        Map<String, Set<AliasData>> converted = new LinkedHashMap<>();
        aliasesMetadatas.forEach((index, aliasMetaDataSet) -> {
            Set<AliasData> aliasDataSet = new LinkedHashSet<>();
            aliasMetaDataSet.forEach(aliasMetaData -> aliasDataSet.add(toAliasData(aliasMetaData)));
            converted.put(index, aliasDataSet);
        });
        return converted;
    }

    public static AliasData toAliasData(AliasMetadata aliasMetaData) {
        Query filterQuery = null;
        CompressedXContent aliasMetaDataFilter = aliasMetaData.getFilter();

        if (aliasMetaDataFilter != null) {
            filterQuery = StringQuery.builder(aliasMetaDataFilter.string()).build();
        }
        return AliasData.of(
                aliasMetaData.alias(),
                filterQuery,
                aliasMetaData.indexRouting(),
                aliasMetaData.getSearchRouting(),
                aliasMetaData.writeIndex(),
                aliasMetaData.isHidden());
    }
    // endregion

    // region index informations
    /**
     * get the index informations from a {@link GetIndexResponse}
     *
     * @param getIndexResponse the index response, must not be {@literal null}
     * @return list of {@link IndexInformation}s for the different indices
     */
    public static List<IndexInformation> getIndexInformations(GetIndexResponse getIndexResponse) {

        Assert.notNull(getIndexResponse, "getIndexResponse must not be null");

        List<IndexInformation> indexInformationList = new ArrayList<>();

        for (String indexName : getIndexResponse.getIndices()) {
            Settings settings = settingsFromGetIndexResponse(getIndexResponse, indexName);
            Document mappings = mappingsFromGetIndexResponse(getIndexResponse, indexName);
            List<AliasData> aliases = aliasDataFromIndexResponse(getIndexResponse, indexName);

            indexInformationList.add(IndexInformation.of(indexName, settings, mappings, aliases));
        }

        return indexInformationList;
    }

    /**
     * extract the index settings information from a given index
     *
     * @param getIndexResponse the OpenSearch GetIndexResponse
     * @param indexName the index name
     * @return a document that represents {@link Settings}
     */
    private static Settings settingsFromGetIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
        Settings settings = new Settings();

        org.opensearch.common.settings.Settings indexSettings =
                getIndexResponse.getSettings().get(indexName);

        if (!indexSettings.isEmpty()) {
            for (String key : indexSettings.keySet()) {
                settings.put(key, indexSettings.get(key));
            }
        }

        return settings;
    }

    /**
     * extract the mappings information from a given index
     *
     * @param getIndexResponse the OpenSearch GetIndexResponse
     * @param indexName the index name
     * @return a document that represents {@link MappingMetadata}
     */
    private static Document mappingsFromGetIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
        Document document = Document.create();

        if (getIndexResponse.getMappings().containsKey(indexName)) {
            MappingMetadata mappings = getIndexResponse.getMappings().get(indexName);
            document = Document.from(mappings.getSourceAsMap());
        }

        return document;
    }

    private static List<AliasData> aliasDataFromIndexResponse(GetIndexResponse getIndexResponse, String indexName) {
        List<AliasData> aliases = Collections.emptyList();

        if (getIndexResponse.getAliases().get(indexName) != null) {
            aliases = getIndexResponse.getAliases().get(indexName).stream()
                    .map(ResponseConverter::toAliasData)
                    .collect(Collectors.toList());
        }
        return aliases;
    }

    /**
     * get the index informations from a {@link org.opensearch.action.admin.indices.get.GetIndexResponse} (transport
     * client)
     *
     * @param getIndexResponse the index response, must not be {@literal null}
     * @return list of {@link IndexInformation}s for the different indices
     */
    public static List<IndexInformation> getIndexInformations(
            org.opensearch.action.admin.indices.get.GetIndexResponse getIndexResponse) {
        List<IndexInformation> indexInformationList = new ArrayList<>();

        for (String indexName : getIndexResponse.getIndices()) {
            Settings settings = settingsFromGetIndexResponse(getIndexResponse, indexName);
            Document mappings = mappingsFromGetIndexResponse(getIndexResponse, indexName);
            List<AliasData> aliases = aliasDataFromIndexResponse(getIndexResponse, indexName);

            indexInformationList.add(IndexInformation.of(indexName, settings, mappings, aliases));
        }

        return indexInformationList;
    }

    private static Settings settingsFromGetIndexResponse(
            org.opensearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {

        Settings settings = new Settings();

        if (getIndexResponse.getSettings().containsKey(indexName)) {
            org.opensearch.common.settings.Settings indexSettings =
                    getIndexResponse.getSettings().get(indexName);

            for (String key : indexSettings.keySet()) {
                settings.put(key, indexSettings.get(key));
            }
        }

        return settings;
    }

    private static Document mappingsFromGetIndexResponse(
            org.opensearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {
        Document document = Document.create();

        boolean responseHasMappings = getIndexResponse.getMappings().containsKey(indexName);

        if (responseHasMappings) {
            final MappingMetadata mappings = getIndexResponse.getMappings().get(indexName);
            if (mappings != null) {
                document = Document.from(mappings.getSourceAsMap());
            }
        }

        return document;
    }

    private static List<AliasData> aliasDataFromIndexResponse(
            org.opensearch.action.admin.indices.get.GetIndexResponse getIndexResponse, String indexName) {
        List<AliasData> aliases = Collections.emptyList();

        if (getIndexResponse.getAliases().get(indexName) != null) {
            aliases = getIndexResponse.getAliases().get(indexName).stream()
                    .map(ResponseConverter::toAliasData)
                    .collect(Collectors.toList());
        }
        return aliases;
    }

    // endregion

    // region templates
    @Nullable
    public static TemplateData getTemplateData(
            GetIndexTemplatesResponse getIndexTemplatesResponse, String templateName) {
        for (IndexTemplateMetadata indexTemplateMetadata : getIndexTemplatesResponse.getIndexTemplates()) {

            if (indexTemplateMetadata.name().equals(templateName)) {

                Settings settings = extracSettingsFromMetaData(indexTemplateMetadata);

                Map<String, AliasData> aliases = new LinkedHashMap<>();

                Map<String, AliasMetadata> aliasesResponse = indexTemplateMetadata.aliases();
                Iterator<String> keysIt = aliasesResponse.keySet().iterator();
                while (keysIt.hasNext()) {
                    String key = keysIt.next();
                    aliases.put(key, ResponseConverter.toAliasData(aliasesResponse.get(key)));
                }

                return TemplateData.builder()
                        .withIndexPatterns(indexTemplateMetadata.patterns().toArray(new String[0])) //
                        .withSettings(settings) //
                        .withMapping(
                                Document.from(indexTemplateMetadata.mappings().getSourceAsMap())) //
                        .withAliases(aliases) //
                        .withOrder(indexTemplateMetadata.order()) //
                        .withVersion(indexTemplateMetadata.version())
                        .build();
            }
        }
        return null;
    }

    @Nullable
    public static TemplateResponse getTemplateResponse(GetIndexTemplatesResponse getIndexTemplatesResponse, String templateName) {
        for (IndexTemplateMetadata indexTemplateMetadata : getIndexTemplatesResponse.getIndexTemplates()) {

            if (indexTemplateMetadata.name().equals(templateName)) {

                Settings settings = extracSettingsFromMetaData(indexTemplateMetadata);
                Map<String, AliasData> aliases = extractAliasesFromMetaData(indexTemplateMetadata);

                return TemplateResponse.builder()
                        .withName(indexTemplateMetadata.name())
                        .withTemplateData(TemplateResponseData.builder()
                                .withAliases(aliases)
                                .withSettings(settings)
                                .withMapping(Document.from(indexTemplateMetadata.mappings().getSourceAsMap()))
                                .build())
                        .build();
            }
        }
        return null;
    }

    private static Settings extracSettingsFromMetaData(IndexTemplateMetadata indexTemplateMetadata) {
        Settings esSettings = new Settings();
        org.opensearch.common.settings.Settings osSettings = indexTemplateMetadata.settings();
        osSettings.keySet().forEach(key -> esSettings.put(key, osSettings.get(key)));
        return esSettings;
    }

    private static Map<String, AliasData> extractAliasesFromMetaData(IndexTemplateMetadata indexTemplateMetadata) {
        Map<String, AliasData> aliases = new LinkedHashMap<>();

        Map<String, AliasMetadata> aliasesResponse = indexTemplateMetadata.aliases();
        Iterator<String> keysIt = aliasesResponse.keySet().iterator();
        while (keysIt.hasNext()) {
            String key = keysIt.next();
            aliases.put(key, ResponseConverter.toAliasData(aliasesResponse.get(key)));
        }

        return aliases;
    }

    // endregion

    // region settings
    /**
     * extract the index settings information for a given index
     *
     * @param response the OpenSearch response
     * @param indexName the index name
     * @return settings
     */
    public static Settings fromSettingsResponse(GetSettingsResponse response, String indexName) {

        Settings settings = new Settings();

        if (!response.getIndexToDefaultSettings().isEmpty()) {
            org.opensearch.common.settings.Settings defaultSettings =
                    response.getIndexToDefaultSettings().get(indexName);
            for (String key : defaultSettings.keySet()) {
                settings.put(key, defaultSettings.get(key));
            }
        }

        if (!response.getIndexToSettings().isEmpty()) {
            org.opensearch.common.settings.Settings customSettings =
                    response.getIndexToSettings().get(indexName);
            for (String key : customSettings.keySet()) {
                settings.put(key, customSettings.get(key));
            }
        }

        return settings;
    }
    // endregion

    // region multiget

    @Nullable
    public static MultiGetItem.Failure getFailure(MultiGetItemResponse itemResponse) {

        MultiGetResponse.Failure responseFailure = itemResponse.getFailure();
        return responseFailure != null
                ? MultiGetItem.Failure.of(
                        responseFailure.getIndex(), "_doc", responseFailure.getId(), responseFailure.getFailure(), null)
                : null;
    }
    // endregion

    // region cluster operations
    public static ClusterHealth clusterHealth(ClusterHealthResponse clusterHealthResponse) {
        return ClusterHealth.builder() //
                .withActivePrimaryShards(clusterHealthResponse.getActivePrimaryShards()) //
                .withActiveShards(clusterHealthResponse.getActiveShards()) //
                .withActiveShardsPercent(clusterHealthResponse.getActiveShardsPercent()) //
                .withClusterName(clusterHealthResponse.getClusterName()) //
                .withDelayedUnassignedShards(clusterHealthResponse.getDelayedUnassignedShards()) //
                .withInitializingShards(clusterHealthResponse.getInitializingShards()) //
                .withNumberOfDataNodes(clusterHealthResponse.getNumberOfDataNodes()) //
                .withNumberOfInFlightFetch(clusterHealthResponse.getNumberOfInFlightFetch()) //
                .withNumberOfNodes(clusterHealthResponse.getNumberOfNodes()) //
                .withNumberOfPendingTasks(clusterHealthResponse.getNumberOfPendingTasks()) //
                .withRelocatingShards(clusterHealthResponse.getRelocatingShards()) //
                .withStatus(clusterHealthResponse.getStatus().toString()) //
                .withTaskMaxWaitingTimeMillis(
                        clusterHealthResponse.getTaskMaxWaitingTime().millis()) //
                .withTimedOut(clusterHealthResponse.isTimedOut()) //
                .withUnassignedShards(clusterHealthResponse.getUnassignedShards()) //
                .build(); //
    }
    // endregion

    // region byQueryResponse
    public static ByQueryResponse byQueryResponseOf(BulkByScrollResponse bulkByScrollResponse) {
        final List<ByQueryResponse.Failure> failures = bulkByScrollResponse
                .getBulkFailures() //
                .stream() //
                .map(ResponseConverter::byQueryResponseFailureOf) //
                .collect(Collectors.toList()); //

        final List<ByQueryResponse.SearchFailure> searchFailures = bulkByScrollResponse
                .getSearchFailures() //
                .stream() //
                .map(ResponseConverter::byQueryResponseSearchFailureOf) //
                .collect(Collectors.toList()); //

        return ByQueryResponse.builder() //
                .withTook(bulkByScrollResponse.getTook().getMillis()) //
                .withTimedOut(bulkByScrollResponse.isTimedOut()) //
                .withTotal(bulkByScrollResponse.getTotal()) //
                .withUpdated(bulkByScrollResponse.getUpdated()) //
                .withDeleted(bulkByScrollResponse.getDeleted()) //
                .withBatches(bulkByScrollResponse.getBatches()) //
                .withVersionConflicts(bulkByScrollResponse.getVersionConflicts()) //
                .withNoops(bulkByScrollResponse.getNoops()) //
                .withBulkRetries(bulkByScrollResponse.getBulkRetries()) //
                .withSearchRetries(bulkByScrollResponse.getSearchRetries()) //
                .withReasonCancelled(bulkByScrollResponse.getReasonCancelled()) //
                .withFailures(failures) //
                .withSearchFailure(searchFailures) //
                .build(); //
    }

    /**
     * Create a new {@link ByQueryResponse.Failure} from {@link BulkItemResponse.Failure}
     *
     * @param failure {@link BulkItemResponse.Failure} to translate
     * @return a new {@link ByQueryResponse.Failure}
     */
    public static ByQueryResponse.Failure byQueryResponseFailureOf(BulkItemResponse.Failure failure) {
        return ByQueryResponse.Failure.builder() //
                .withIndex(failure.getIndex()) //
                .withId(failure.getId()) //
                .withStatus(failure.getStatus().getStatus()) //
                .withAborted(failure.isAborted()) //
                .withCause(failure.getCause()) //
                .withSeqNo(failure.getSeqNo()) //
                .withTerm(failure.getTerm()) //
                .build(); //
    }

    /**
     * Create a new {@link ByQueryResponse.SearchFailure} from {@link ScrollableHitSource.SearchFailure}
     *
     * @param searchFailure {@link ScrollableHitSource.SearchFailure} to translate
     * @return a new {@link ByQueryResponse.SearchFailure}
     */
    public static ByQueryResponse.SearchFailure byQueryResponseSearchFailureOf(
            ScrollableHitSource.SearchFailure searchFailure) {
        return ByQueryResponse.SearchFailure.builder() //
                .withReason(searchFailure.getReason()) //
                .withIndex(searchFailure.getIndex()) //
                .withNodeId(searchFailure.getNodeId()) //
                .withShardId(searchFailure.getShardId()) //
                .withStatus(searchFailure.getStatus().getStatus()) //
                .build(); //
    }

    // endregion

    // region reindex
    public static ReindexResponse reindexResponseOf(BulkByScrollResponse bulkByScrollResponse) {
        final List<ReindexResponse.Failure> failures = bulkByScrollResponse
                .getBulkFailures() //
                .stream() //
                .map(ResponseConverter::reindexResponseFailureOf) //
                .collect(Collectors.toList()); //

        return ReindexResponse.builder() //
                .withTook(bulkByScrollResponse.getTook().getMillis()) //
                .withTimedOut(bulkByScrollResponse.isTimedOut()) //
                .withTotal(bulkByScrollResponse.getTotal()) //
                .withCreated(bulkByScrollResponse.getCreated()) //
                .withUpdated(bulkByScrollResponse.getUpdated()) //
                .withDeleted(bulkByScrollResponse.getDeleted()) //
                .withBatches(bulkByScrollResponse.getBatches()) //
                .withVersionConflicts(bulkByScrollResponse.getVersionConflicts()) //
                .withNoops(bulkByScrollResponse.getNoops()) //
                .withBulkRetries(bulkByScrollResponse.getBulkRetries()) //
                .withSearchRetries(bulkByScrollResponse.getSearchRetries()) //
                .withThrottledMillis(
                        bulkByScrollResponse.getStatus().getThrottled().getMillis()) //
                .withRequestsPerSecond(bulkByScrollResponse.getStatus().getRequestsPerSecond()) //
                .withThrottledUntilMillis(
                        bulkByScrollResponse.getStatus().getThrottledUntil().getMillis()) //
                .withFailures(failures) //
                .build(); //
    }

    /**
     * @since 4.4
     */
    public static ReindexResponse.Failure reindexResponseFailureOf(BulkItemResponse.Failure failure) {
        return ReindexResponse.Failure.builder() //
                .withIndex(failure.getIndex()) //
                .withId(failure.getId()) //
                .withStatus(failure.getStatus().getStatus()) //
                .withAborted(failure.isAborted()) //
                .withCause(failure.getCause()) //
                .withSeqNo(failure.getSeqNo()) //
                .withTerm(failure.getTerm()) //
                .build(); //
    }

    // endregion

    @Nullable
    static ElasticsearchErrorCause toErrorCause(String reason, @Nullable Throwable errorCause) {

        if (errorCause != null) {
            try (PrintWriter writer = new PrintWriter(new StringWriter())) {
                errorCause.printStackTrace(writer);
                writer.flush();

                return new ElasticsearchErrorCause( //
                    null, //
                    reason, //
                    writer.toString(), //
                    toErrorCause(reason, errorCause.getCause()), //
                    List.of(), //
                    Arrays.stream(errorCause.getSuppressed()).map(s -> toErrorCause(reason, s)).collect(Collectors.toList()));
            }
        } else {
            return null;
        }
    }

}
