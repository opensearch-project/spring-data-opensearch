/*
 * Copyright 2021-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.data.client.osc;

import static org.springframework.util.StringUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opensearch.client.opensearch.indices.*;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.ResourceUtil;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.index.DeleteIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link IndexOperations} interface using en {@link OpenSearchIndicesClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public class IndicesTemplate extends ChildTemplate<OpenSearchTransport, OpenSearchIndicesClient>
        implements IndexOperations {

    // we need a cluster client as well because ES has put some methods from the indices API into the cluster client
    // (component templates)
    private final ClusterTemplate clusterTemplate;
    protected final ElasticsearchConverter elasticsearchConverter;
    protected final MappingParametersCustomizer mappingParametersCustomizer;
    @Nullable protected final Class<?> boundClass;
    @Nullable protected final IndexCoordinates boundIndex;

    public IndicesTemplate(OpenSearchIndicesClient client, ClusterTemplate clusterTemplate,
            ElasticsearchConverter elasticsearchConverter, MappingParametersCustomizer mappingParametersCustomizer, Class<?> boundClass) {
        super(client, elasticsearchConverter);

        Assert.notNull(clusterTemplate, "cluster must not be null");
        Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null");
        Assert.notNull(boundClass, "boundClass may not be null");

        this.clusterTemplate = clusterTemplate;
        this.elasticsearchConverter = elasticsearchConverter;
        this.mappingParametersCustomizer = mappingParametersCustomizer;
        this.boundClass = boundClass;
        this.boundIndex = null;

    }

    public IndicesTemplate(OpenSearchIndicesClient client, ClusterTemplate clusterTemplate, ElasticsearchConverter elasticsearchConverter,
            MappingParametersCustomizer mappingParametersCustomizer, IndexCoordinates boundIndex) {
        super(client, elasticsearchConverter);

        Assert.notNull(clusterTemplate, "cluster must not be null");
        Assert.notNull(elasticsearchConverter, "elasticsearchConverter must not be null");
        Assert.notNull(boundIndex, "boundIndex must not be null");

        this.clusterTemplate = clusterTemplate;
        this.elasticsearchConverter = elasticsearchConverter;
        this.mappingParametersCustomizer = mappingParametersCustomizer;
        this.boundClass = null;
        this.boundIndex = boundIndex;

    }

    protected Class<?> checkForBoundClass() {
        if (boundClass == null) {
            throw new InvalidDataAccessApiUsageException("IndexOperations are not bound");
        }
        return boundClass;
    }

    @Override
    public boolean create() {

        Settings settings = boundClass != null ? createSettings(boundClass) : new Settings();
        return doCreate(getIndexCoordinates(), settings, null);
    }

    @Override
    public boolean create(Map<String, Object> settings) {

        Assert.notNull(settings, "settings must not be null");

        return doCreate(getIndexCoordinates(), settings, null);
    }

    @Override
    public boolean create(Map<String, Object> settings, Document mapping) {

        Assert.notNull(settings, "settings must not be null");
        Assert.notNull(mapping, "mapping must not be null");

        return doCreate(getIndexCoordinates(), settings, mapping);
    }

    @Override
    public boolean createWithMapping() {
        return doCreate(getIndexCoordinates(), createSettings(), createMapping());
    }

    protected boolean doCreate(final IndexCoordinates indexCoordinates, final Map<String, Object> settings,
            @Nullable final Document mapping) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");
        Assert.notNull(settings, "settings must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_CREATE, indexCoordinates, () -> {
            final var createIndexRequest = requestConverter.indicesCreateRequest(indexCoordinates, settings, mapping);
            final var createIndexResponse = execute(client -> client.create(createIndexRequest));
            return Boolean.TRUE.equals(createIndexResponse.acknowledged());
        });
    }

    @Override
    public boolean delete() {
        return observations().observe(OpenSearchOperationName.INDEX_DELETE, getIndexCoordinates(),
                () -> doDelete(getIndexCoordinates()));
    }

    private boolean doDelete(IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        if (doExists(indexCoordinates)) {
            DeleteIndexRequest deleteIndexRequest = requestConverter.indicesDeleteRequest(indexCoordinates);
            DeleteIndexResponse deleteIndexResponse = execute(client -> client.delete(deleteIndexRequest));
            return deleteIndexResponse.acknowledged();
        }

        return false;
    }

    @Override
    public boolean exists() {
        return observations().observe(OpenSearchOperationName.INDEX_EXISTS, getIndexCoordinates(),
                () -> doExists(getIndexCoordinates()));
    }

    private boolean doExists(IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        ExistsRequest existsRequest = requestConverter.indicesExistsRequest(indexCoordinates);
        BooleanResponse existsResponse = execute(client -> client.exists(existsRequest));
        return existsResponse.value();
    }

    @Override
    public void refresh() {

        observations().observe(OpenSearchOperationName.INDEX_REFRESH, getIndexCoordinates(), () -> {
            final var refreshRequest = requestConverter.indicesRefreshRequest(getIndexCoordinates());
            execute(client -> client.refresh(refreshRequest));
            return null;
        });
    }

    @Override
    public Document createMapping() {
        return createMapping(checkForBoundClass());
    }

    @Override
    public Document createMapping(Class<?> clazz) {

        Assert.notNull(clazz, "clazz must not be null");

        // load mapping specified in Mapping annotation if present
        Mapping mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Mapping.class);

        if (mappingAnnotation != null) {
            String mappingPath = mappingAnnotation.mappingPath();

            if (hasText(mappingPath)) {
                String mappings = ResourceUtil.readFileFromClasspath(mappingPath);

                if (hasText(mappings)) {
                    return Document.parse(mappings);
                }
            }
        }

        // build mapping from field annotations
        try {
            String mapping = new MappingBuilder(elasticsearchConverter, mappingParametersCustomizer).buildPropertyMapping(clazz);
            return Document.parse(mapping);
        } catch (Exception e) {
            throw new UncategorizedElasticsearchException("Failed to build mapping for " + clazz.getSimpleName(), e);
        }
    }

    @Override
    public boolean putMapping(final Document mapping) {

        Assert.notNull(mapping, "mapping must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_PUT_MAPPING, getIndexCoordinates(), () -> {
            final var putMappingRequest = requestConverter.indicesPutMappingRequest(getIndexCoordinates(), mapping);
            final var putMappingResponse = execute(client -> client.putMapping(putMappingRequest));
            return putMappingResponse.acknowledged();
        });
    }

    @Override
    public Map<String, Object> getMapping() {

        return observations().observe(OpenSearchOperationName.INDEX_GET_MAPPING, getIndexCoordinates(), () -> {
            final var indexCoordinates = getIndexCoordinates();
            final var getMappingRequest = requestConverter.indicesGetMappingRequest(indexCoordinates);
            final var getMappingResponse = execute(client -> client.getMapping(getMappingRequest));

            final var mappingResponse = responseConverter.indicesGetMapping(getMappingResponse, indexCoordinates);
            return mappingResponse;
        });
    }

    @Override
    public Settings createSettings() {
        return createSettings(checkForBoundClass());
    }

    @Override
    public Settings createSettings(Class<?> clazz) {

        Assert.notNull(clazz, "clazz must not be null");

        ElasticsearchPersistentEntity<?> persistentEntity = getRequiredPersistentEntity(clazz);
        String settingPath = persistentEntity.settingPath();
        return hasText(settingPath) //
                ? Settings.parse(ResourceUtil.readFileFromClasspath(settingPath)) //
                : persistentEntity.getDefaultSettings();

    }

    @Override
    public Settings getSettings() {
        return getSettings(false);
    }

    @Override
    public Settings getSettings(final boolean includeDefaults) {

        return observations().observe(OpenSearchOperationName.INDEX_GET_SETTINGS, getIndexCoordinates(), () -> {
            final var getIndicesSettingsRequest = requestConverter
                    .indicesGetSettingsRequest(getIndexCoordinates(), includeDefaults);
            final var getIndicesSettingsResponse = execute(
                    client -> client.getSettings(getIndicesSettingsRequest));
            return responseConverter.indicesGetSettings(getIndicesSettingsResponse, getIndexCoordinates().getIndexName());
        });
    }

    @Override
    public boolean alias(final AliasActions aliasActions) {

        Assert.notNull(aliasActions, "aliasActions must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_ALIAS, null, () -> {
            final var updateAliasesRequest = requestConverter.indicesUpdateAliasesRequest(aliasActions);
            final var updateAliasesResponse = execute(client -> client.updateAliases(updateAliasesRequest));
            return updateAliasesResponse.acknowledged();
        });
    }

    @Override
    public Map<String, Set<AliasData>> getAliases(final String... aliasNames) {

        Assert.notNull(aliasNames, "aliasNames must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_GET_ALIASES, null, () -> {
            final var getAliasRequest = requestConverter.indicesGetAliasRequest(aliasNames, null);
            final var getAliasResponse = execute(client -> client.getAlias(getAliasRequest));
            return responseConverter.indicesGetAliasData(getAliasResponse);
        });
    }

    @Override
    public Map<String, Set<AliasData>> getAliasesForIndex(final String... indexNames) {

        Assert.notNull(indexNames, "indexNames must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_GET_ALIASES, indexNames.length == 0 ? null : IndexCoordinates.of(indexNames), () -> {
            final var getAliasRequest = requestConverter.indicesGetAliasRequest(null, indexNames);
            final var getAliasResponse = execute(client -> client.getAlias(getAliasRequest));
            return responseConverter.indicesGetAliasData(getAliasResponse);
        });
    }

    @Override
    public boolean putTemplate(final PutTemplateRequest putTemplateRequest) {

        Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_PUT_TEMPLATE, null, () -> {
            final var putTemplateRequestES = requestConverter
                    .indicesPutTemplateRequest(putTemplateRequest);
            return execute(client -> client.putTemplate(putTemplateRequestES)).acknowledged();
        });
    }

    @Override
    public TemplateData getTemplate(final GetTemplateRequest getTemplateRequest) {

        Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_GET_TEMPLATE, null, () -> {
            final var getTemplateRequestES = requestConverter
                    .indicesGetTemplateRequest(getTemplateRequest);
            final var getTemplateResponse = execute(client -> client.getTemplate(getTemplateRequestES));

            return responseConverter.indicesGetTemplateData(getTemplateResponse, getTemplateRequest.getTemplateName());
        });
    }

    @Override
    public boolean existsTemplate(final ExistsTemplateRequest existsTemplateRequest) {

        Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_EXISTS_TEMPLATE, null, () -> {
            final var existsTemplateRequestSO = requestConverter
                    .indicesExistsTemplateRequest(existsTemplateRequest);
            return execute(client -> client.existsTemplate(existsTemplateRequestSO)).value();
        });
    }

    @Override
    public boolean deleteTemplate(final DeleteTemplateRequest deleteTemplateRequest) {

        Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_DELETE_TEMPLATE, null, () -> {
            final var deleteTemplateRequestES = requestConverter
                    .indicesDeleteTemplateRequest(deleteTemplateRequest);
            return execute(client -> client.deleteTemplate(deleteTemplateRequestES)).acknowledged();
        });
    }

    @Override
    public boolean putIndexTemplate(final PutIndexTemplateRequest putIndexTemplateRequest) {

        return observations().observe(OpenSearchOperationName.INDEX_PUT_INDEX_TEMPLATE, null, () -> {
            final var putIndexTemplateRequestES = requestConverter
                    .indicesPutIndexTemplateRequest(putIndexTemplateRequest);

            return execute(client -> client.putIndexTemplate(putIndexTemplateRequestES)).acknowledged();
        });
    }

    @Override
    public boolean existsIndexTemplate(final ExistsIndexTemplateRequest existsIndexTemplateRequest) {

        Assert.notNull(existsIndexTemplateRequest, "existsIndexTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_EXISTS_INDEX_TEMPLATE, null, () -> {
            final var existsTemplateRequestES = requestConverter
                    .indicesExistsIndexTemplateRequest(existsIndexTemplateRequest);
            return execute(client -> client.existsIndexTemplate(existsTemplateRequestES)).value();
        });
    }

    @Override
    public List<TemplateResponse> getIndexTemplate(final GetIndexTemplateRequest getIndexTemplateRequest) {

        Assert.notNull(getIndexTemplateRequest, "getIndexTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_GET_INDEX_TEMPLATE, null, () -> {
            final var getIndexTemplateRequestES = requestConverter
                    .indicesGetIndexTemplateRequest(getIndexTemplateRequest);
            final var getIndexTemplateResponse = execute(client -> client.getIndexTemplate(getIndexTemplateRequestES));
            return responseConverter.getIndexTemplates(getIndexTemplateResponse);
        });
    }

    @Override
    public boolean deleteIndexTemplate(final DeleteIndexTemplateRequest deleteIndexTemplateRequest) {

        Assert.notNull(deleteIndexTemplateRequest, "deleteIndexTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_DELETE_INDEX_TEMPLATE, null, () -> {
            final var deleteIndexTemplateRequestES = requestConverter
                    .indicesDeleteIndexTemplateRequest(deleteIndexTemplateRequest);
            return execute(client -> client.deleteIndexTemplate(deleteIndexTemplateRequestES)).acknowledged();
        });
    }

    @Override
    public boolean putComponentTemplate(final PutComponentTemplateRequest putComponentTemplateRequest) {

        Assert.notNull(putComponentTemplateRequest, "putComponentTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_PUT_COMPONENT_TEMPLATE, null, () -> {
            final var putComponentTemplateRequestES = requestConverter
                    .clusterPutComponentTemplateRequest(putComponentTemplateRequest);
            // the new Elasticsearch client has this call in the cluster index
            return clusterTemplate.execute(client -> client.putComponentTemplate(putComponentTemplateRequestES)).acknowledged();
        });
    }

    @Override
    public boolean existsComponentTemplate(final ExistsComponentTemplateRequest existsComponentTemplateRequest) {

        Assert.notNull(existsComponentTemplateRequest, "existsComponentTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_EXISTS_COMPONENT_TEMPLATE, null, () -> {
            final var existsComponentTemplateRequestES = requestConverter
                    .clusterExistsComponentTemplateRequest(existsComponentTemplateRequest);
            return clusterTemplate.execute(client -> client.existsComponentTemplate(existsComponentTemplateRequestES)).value();
        });
    }

    @Override
    public List<TemplateResponse> getComponentTemplate(final GetComponentTemplateRequest getComponentTemplateRequest) {

        return observations().observe(OpenSearchOperationName.INDEX_GET_COMPONENT_TEMPLATE, null, () -> {
            final var getComponentTemplateRequestES = requestConverter
                    .clusterGetComponentTemplateRequest(getComponentTemplateRequest);
            final var response = clusterTemplate.execute(client -> client.getComponentTemplate(getComponentTemplateRequestES));
            return responseConverter.clusterGetComponentTemplates(response);
        });
    }

    @Override
    public boolean deleteComponentTemplate(final DeleteComponentTemplateRequest deleteComponentTemplateRequest) {

        Assert.notNull(deleteComponentTemplateRequest, "deleteComponentTemplateRequest must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_DELETE_COMPONENT_TEMPLATE, null, () -> {
            final var deleteComponentTemplateRequestES = requestConverter
                    .clusterDeleteComponentTemplateRequest(deleteComponentTemplateRequest);
            return clusterTemplate.execute(client -> client.deleteComponentTemplate(deleteComponentTemplateRequestES))
                    .acknowledged();
        });
    }

    @Override
    public List<IndexInformation> getInformation(final IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return observations().observe(OpenSearchOperationName.INDEX_GET_INFORMATION, indexCoordinates, () -> {
            final var getIndexRequest = requestConverter.indicesGetIndexRequest(indexCoordinates);
            final var getIndexResponse = execute(client -> client.get(getIndexRequest));
            return responseConverter.indicesGetIndexInformations(getIndexResponse);
        });
    }

    // region Helper functions
    ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
        return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
    }

    @Override
    public IndexCoordinates getIndexCoordinates() {
        return (boundClass != null) ? getIndexCoordinatesFor(boundClass) : Objects.requireNonNull(boundIndex);
    }

    public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
        return getRequiredPersistentEntity(clazz).getIndexCoordinates();
    }
    // endregion
}
