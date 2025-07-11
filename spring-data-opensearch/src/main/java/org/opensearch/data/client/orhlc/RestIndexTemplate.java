/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.action.admin.indices.refresh.RefreshRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.opensearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.GetIndexResponse;
import org.opensearch.client.indices.GetIndexTemplatesRequest;
import org.opensearch.client.indices.GetIndexTemplatesResponse;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.client.indices.IndexTemplatesExistRequest;
import org.opensearch.client.indices.PutIndexTemplateRequest;
import org.opensearch.client.indices.PutMappingRequest;
import org.opensearch.cluster.metadata.MappingMetadata;
import org.opensearch.data.core.AbstractIndexTemplate;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.DeleteComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.index.TemplateResponse;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link IndexOperations} implementation using the RestClient.
 * @since 0.1
 */
class RestIndexTemplate extends AbstractIndexTemplate implements IndexOperations {

    private static final Log LOGGER = LogFactory.getLog(RestIndexTemplate.class);

    private final OpenSearchRestTemplate restTemplate;
    protected final RequestFactory requestFactory;

    public RestIndexTemplate(OpenSearchRestTemplate restTemplate, Class<?> boundClass) {
        super(restTemplate.getElasticsearchConverter(), boundClass);
        this.restTemplate = restTemplate;
        requestFactory = new RequestFactory(elasticsearchConverter);
    }

    public RestIndexTemplate(OpenSearchRestTemplate restTemplate, IndexCoordinates boundIndex) {
        super(restTemplate.getElasticsearchConverter(), boundIndex);
        this.restTemplate = restTemplate;
        requestFactory = new RequestFactory(elasticsearchConverter);
    }

    @Override
    protected boolean doCreate(IndexCoordinates index, Map<String, Object> settings, @Nullable Document mapping) {
        CreateIndexRequest request = requestFactory.createIndexRequest(index, settings, mapping);
        return restTemplate.execute(client ->
                client.indices().create(request, RequestOptions.DEFAULT).isAcknowledged());
    }

    @Override
    protected boolean doDelete(IndexCoordinates index) {

        Assert.notNull(index, "index must not be null");

        if (doExists(index)) {
            DeleteIndexRequest deleteIndexRequest = requestFactory.deleteIndexRequest(index);
            return restTemplate.execute(client -> client.indices()
                    .delete(deleteIndexRequest, RequestOptions.DEFAULT)
                    .isAcknowledged());
        }
        return false;
    }

    @Override
    protected boolean doExists(IndexCoordinates index) {

        GetIndexRequest getIndexRequest = requestFactory.getIndexRequest(index);
        return restTemplate.execute(client -> client.indices().exists(getIndexRequest, RequestOptions.DEFAULT));
    }

    @Override
    protected boolean doPutMapping(IndexCoordinates index, Document mapping) {

        Assert.notNull(index, "No index defined for putMapping()");

        PutMappingRequest request = requestFactory.putMappingRequest(index, mapping);
        return restTemplate.execute(client ->
                client.indices().putMapping(request, RequestOptions.DEFAULT).isAcknowledged());
    }

    @Override
    protected Map<String, Object> doGetMapping(IndexCoordinates index) {

        Assert.notNull(index, "No index defined for doGetMapping()");

        GetMappingsRequest mappingsRequest = requestFactory.getMappingsRequest(index);

        return restTemplate.execute(client -> {
            Map<String, MappingMetadata> mappings = client.indices() //
                    .getMapping(mappingsRequest, RequestOptions.DEFAULT) //
                    .mappings(); //

            if (mappings == null || mappings.size() == 0) {
                return Collections.emptyMap();
            }

            if (mappings.size() > 1) {
                LOGGER.warn("more than one mapping returned for " + index.getIndexName());
            }
            // we have at least one, take the first from the iterator
            return mappings.entrySet().iterator().next().getValue().getSourceAsMap();
        });
    }

    @Override
    protected Map<String, Set<AliasData>> doGetAliases(@Nullable String[] aliasNames, @Nullable String[] indexNames) {

        GetAliasesRequest getAliasesRequest = requestFactory.getAliasesRequest(aliasNames, indexNames);

        return restTemplate.execute(client -> ResponseConverter.aliasDatas(client.indices()
                .getAlias(getAliasesRequest, RequestOptions.DEFAULT)
                .getAliases()));
    }

    @Override
    public boolean alias(AliasActions aliasActions) {

        IndicesAliasesRequest request = requestFactory.indicesAliasesRequest(aliasActions);
        return restTemplate.execute(client ->
                client.indices().updateAliases(request, RequestOptions.DEFAULT).isAcknowledged());
    }

    @Override
    protected Settings doGetSettings(IndexCoordinates index, boolean includeDefaults) {

        Assert.notNull(index, "index must not be null");

        GetSettingsRequest getSettingsRequest = requestFactory.getSettingsRequest(index, includeDefaults);
        GetSettingsResponse response = restTemplate.execute(client -> client.indices() //
                .getSettings(getSettingsRequest, RequestOptions.DEFAULT));

        return ResponseConverter.fromSettingsResponse(
                response, getSettingsRequest.indices()[0]);
    }

    @Override
    protected void doRefresh(IndexCoordinates index) {

        Assert.notNull(index, "No index defined for refresh()");

        RefreshRequest refreshRequest = requestFactory.refreshRequest(index);
        restTemplate.execute(client -> client.indices().refresh(refreshRequest, RequestOptions.DEFAULT));
    }

    @Override
    public boolean putTemplate(PutTemplateRequest putTemplateRequest) {

        Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

        PutIndexTemplateRequest putIndexTemplateRequest = requestFactory.putIndexTemplateRequest(putTemplateRequest);
        return restTemplate.execute(client -> client.indices()
                .putTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT)
                .isAcknowledged());
    }

    @Override
    public TemplateData getTemplate(GetTemplateRequest getTemplateRequest) {

        Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

        // getIndexTemplate throws an error on non-existing template names
        if (!existsTemplate(new ExistsTemplateRequest(getTemplateRequest.getTemplateName()))) {
            return null;
        }

        GetIndexTemplatesRequest getIndexTemplatesRequest = requestFactory.getIndexTemplatesRequest(getTemplateRequest);
        GetIndexTemplatesResponse getIndexTemplatesResponse = restTemplate.execute(
                client -> client.indices().getIndexTemplate(getIndexTemplatesRequest, RequestOptions.DEFAULT));
        return ResponseConverter.getTemplateData(getIndexTemplatesResponse, getTemplateRequest.getTemplateName());
    }

    @Override
    public boolean existsTemplate(ExistsTemplateRequest existsTemplateRequest) {

        Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

        IndexTemplatesExistRequest putIndexTemplateRequest =
                requestFactory.indexTemplatesExistsRequest(existsTemplateRequest);
        return restTemplate.execute(
                client -> client.indices().existsTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT));
    }

    @Override
    public boolean deleteTemplate(DeleteTemplateRequest deleteTemplateRequest) {

        Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

        DeleteIndexTemplateRequest deleteIndexTemplateRequest = requestFactory.deleteIndexTemplateRequest(deleteTemplateRequest);
        return restTemplate
                .execute(client -> client.indices()
                .deleteTemplate(deleteIndexTemplateRequest, RequestOptions.DEFAULT)
                .isAcknowledged());
    }

    @Override
    public boolean putIndexTemplate(
            org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest esPutIndexTemplateRequest)
    {

        Assert.notNull(esPutIndexTemplateRequest, "putIndexTemplateRequest must not be null");

        PutIndexTemplateRequest putIndexTemplateRequest = requestFactory.putIndexTemplateRequest(esPutIndexTemplateRequest);
        return restTemplate.execute(client -> client.indices()
                .putTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT)
                .isAcknowledged());
    }

    @Override
    public boolean existsIndexTemplate(ExistsIndexTemplateRequest existsTemplateRequest) {

        Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

        IndexTemplatesExistRequest putIndexTemplateRequest =
                requestFactory.indexTemplatesExistsRequest(existsTemplateRequest);
        return restTemplate.execute(
                client -> client.indices().existsTemplate(putIndexTemplateRequest, RequestOptions.DEFAULT));

    }

    @Override
    public List<TemplateResponse> getIndexTemplate(GetIndexTemplateRequest getIndexTemplateRequest) {

        Assert.notNull(getIndexTemplateRequest, "getIndexTemplateRequest must not be null");

        if (!existsIndexTemplate(new ExistsIndexTemplateRequest(getIndexTemplateRequest.templateName()))) {
            return null;
        }

        GetIndexTemplatesRequest getIndexTemplatesRequest = requestFactory.getIndexTemplatesRequest(getIndexTemplateRequest);
        GetIndexTemplatesResponse getIndexTemplatesResponse = restTemplate.execute(client -> client.indices().getIndexTemplate(getIndexTemplatesRequest, RequestOptions.DEFAULT));

        TemplateResponse templateResponse = ResponseConverter.getTemplateResponse(getIndexTemplatesResponse, getIndexTemplateRequest.templateName());
        return Collections.singletonList(templateResponse);
    }

    @Override
    public boolean deleteIndexTemplate(org.springframework.data.elasticsearch.core.index.DeleteIndexTemplateRequest deleteIndexTemplateRequest) {

        Assert.notNull(deleteIndexTemplateRequest, "deleteTemplateIndexRequest must not be null");

        DeleteIndexTemplateRequest request = requestFactory.deleteIndexTemplateRequest(deleteIndexTemplateRequest);
        return restTemplate
                .execute(client -> client.indices()
                        .deleteTemplate(request, RequestOptions.DEFAULT)
                        .isAcknowledged());
    }

    @Override
    public boolean putComponentTemplate(PutComponentTemplateRequest putComponentTemplateRequest) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean existsComponentTemplate(ExistsComponentTemplateRequest existsComponentTemplateRequest) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<TemplateResponse> getComponentTemplate(GetComponentTemplateRequest getComponentTemplateRequest) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean deleteComponentTemplate(DeleteComponentTemplateRequest deleteComponentTemplateRequest) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<IndexInformation> getInformation(IndexCoordinates index) {

        Assert.notNull(index, "index must not be null");

        GetIndexRequest request = requestFactory.getIndexRequest(index);
        return restTemplate.execute(client -> {
            GetIndexResponse getIndexResponse = client.indices().get(request, RequestOptions.DEFAULT);
            return ResponseConverter.getIndexInformations(getIndexResponse);
        });
    }
    // endregion
}
