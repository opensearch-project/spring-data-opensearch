/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.osc;

import static org.springframework.util.StringUtils.hasText;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.opensearch.client.opensearch._types.AcknowledgedResponseBase;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteTemplateResponse;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetAliasResponse;
import org.opensearch.client.opensearch.indices.PutIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.PutMappingResponse;
import org.opensearch.client.opensearch.indices.PutTemplateResponse;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.core.IndexInformation;
import org.springframework.data.elasticsearch.core.ReactiveIndexOperations;
import org.springframework.data.elasticsearch.core.ReactiveResourceUtil;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.index.DeleteComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.DeleteIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutComponentTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ReactiveMappingBuilder;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.index.TemplateData;
import org.springframework.data.elasticsearch.core.index.TemplateResponse;
import org.springframework.data.elasticsearch.core.mapping.Alias;
import org.springframework.data.elasticsearch.core.mapping.CreateIndexSettings;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Indices Template
 */
public class ReactiveIndicesTemplate
        extends ReactiveChildTemplate<OpenSearchTransport, ReactiveOpenSearchIndicesClient>
        implements ReactiveIndexOperations {

    // we need a cluster client as well because ES has put some methods from the indices API into the cluster client
    // (component templates)
    private final ReactiveClusterTemplate clusterTemplate;

    @Nullable private final Class<?> boundClass;
    private final IndexCoordinates boundIndexCoordinates;

    public ReactiveIndicesTemplate(ReactiveOpenSearchIndicesClient client, ReactiveClusterTemplate clusterTemplate,
            ElasticsearchConverter elasticsearchConverter, IndexCoordinates index) {

        super(client, elasticsearchConverter);

        Assert.notNull(index, "index must not be null");
        Assert.notNull(clusterTemplate, "clusterTemplate must not be null");

        this.clusterTemplate = clusterTemplate;
        this.boundClass = null;
        this.boundIndexCoordinates = index;
    }

    public ReactiveIndicesTemplate(ReactiveOpenSearchIndicesClient client, ReactiveClusterTemplate clusterTemplate,
            ElasticsearchConverter elasticsearchConverter, Class<?> clazz) {

        super(client, elasticsearchConverter);

        Assert.notNull(clazz, "clazz must not be null");
        Assert.notNull(clusterTemplate, "clusterTemplate must not be null");

        this.clusterTemplate = clusterTemplate;
        this.boundClass = clazz;
        this.boundIndexCoordinates = getIndexCoordinatesFor(clazz);
    }

    @Override
    public Mono<Boolean> create() {

        IndexCoordinates indexCoordinates = getIndexCoordinates();

        if (boundClass != null) {
            return createSettings(boundClass).flatMap(settings -> doCreate(indexCoordinates, settings, null));
        } else {
            return doCreate(indexCoordinates, new Settings(), null);
        }
    }

    @Override
    public Mono<Boolean> create(Map<String, Object> settings) {

        Assert.notNull(settings, "settings must not be null");

        return doCreate(getIndexCoordinates(), settings, null);
    }

    @Override
    public Mono<Boolean> create(Map<String, Object> settings, Document mapping) {

        Assert.notNull(settings, "settings must not be null");
        Assert.notNull(mapping, "mapping must not be null");

        return doCreate(getIndexCoordinates(), settings, mapping);
    }

    @Override
    public Mono<Boolean> createWithMapping() {
        return createSettings() //
                .flatMap(settings -> //
                createMapping().flatMap(mapping -> //
                doCreate(getIndexCoordinates(), settings, mapping))); //
    }

    private Mono<Boolean> doCreate(final IndexCoordinates indexCoordinates, final Map<String, Object> settings,
            @Nullable final Document mapping) {
        return observations().mono(OpenSearchOperationName.INDEX_CREATE, indexCoordinates, () -> {
            final var aliases = (boundClass != null) ? getAliasesFor(boundClass) : new HashSet<Alias>();
            final var indexSettings = CreateIndexSettings.builder(indexCoordinates)
                    .withAliases(aliases)
                    .withSettings(settings)
                    .withMapping(mapping)
                    .build();

            final var createIndexRequest = requestConverter.indicesCreateRequest(indexSettings);
            final var createIndexResponse = Mono.from(execute(client -> client.create(createIndexRequest)));
            return createIndexResponse.map(CreateIndexResponse::acknowledged);
        });
    }

    @Override
    public Mono<Boolean> delete() {
        return observations().mono(OpenSearchOperationName.INDEX_DELETE, getIndexCoordinates(),
                () -> doExists().flatMap(exists -> {

                    if (exists) {
                        final var deleteIndexRequest = requestConverter.indicesDeleteRequest(getIndexCoordinates());
                        return Mono.from(execute(client -> client.delete(deleteIndexRequest))) //
                                .map(DeleteIndexResponse::acknowledged) //
                                .onErrorResume(NoSuchIndexException.class, e -> Mono.just(false));
                    } else {
                        return Mono.just(false);
                    }
                }));
    }

    @Override
    public Mono<Boolean> exists() {
        return observations().mono(OpenSearchOperationName.INDEX_EXISTS, getIndexCoordinates(), this::doExists);
    }

    private Mono<Boolean> doExists() {
        final var existsRequest = requestConverter.indicesExistsRequest(getIndexCoordinates());
        return Mono.from(execute(client -> client.exists(existsRequest))).map(BooleanResponse::value);
    }

    @Override
    public Mono<Void> refresh() {
        return observations().mono(OpenSearchOperationName.INDEX_REFRESH, getIndexCoordinates(), () -> {
            final var refreshRequest = requestConverter.indicesRefreshRequest(getIndexCoordinates());
            return Mono.from(execute(client -> client.refresh(refreshRequest))).then();
        });
    }

    @Override
    public Mono<Document> createMapping() {
        return createMapping(checkForBoundClass());
    }

    @Override
    public Mono<Document> createMapping(Class<?> clazz) {

        Assert.notNull(clazz, "clazz must not be null");

        Mapping mappingAnnotation = AnnotatedElementUtils.findMergedAnnotation(clazz, Mapping.class);

        if (mappingAnnotation != null) {
            String mappingPath = mappingAnnotation.mappingPath();

            if (hasText(mappingPath)) {
                return ReactiveResourceUtil.loadDocument(mappingAnnotation.mappingPath(), "@Mapping");
            }
        }

        return new ReactiveMappingBuilder(elasticsearchConverter).buildReactivePropertyMapping(clazz).map(Document::parse);
    }

    @Override
    public Mono<Boolean> putMapping(final Mono<Document> mapping) {

        Assert.notNull(mapping, "mapping must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_PUT_MAPPING, getIndexCoordinates(), () -> {
            final var putMappingResponse = mapping
                    .map(document -> requestConverter.indicesPutMappingRequest(getIndexCoordinates(), document)) //
                    .flatMap(putMappingRequest -> Mono.from(client.putMapping(putMappingRequest)));
            return putMappingResponse.map(PutMappingResponse::acknowledged);
        });
    }

    @Override
    public Mono<Document> getMapping() {

        return observations().mono(OpenSearchOperationName.INDEX_GET_MAPPING, getIndexCoordinates(), () -> {
            final var indexCoordinates = getIndexCoordinates();
            final var getMappingRequest = requestConverter.indicesGetMappingRequest(indexCoordinates);
            final var getMappingResponse = Mono.from(execute(client -> client.getMapping(getMappingRequest)));
            return getMappingResponse.map(response -> responseConverter.indicesGetMapping(response, indexCoordinates));
        });
    }

    @Override
    public Mono<Settings> createSettings() {
        return createSettings(checkForBoundClass());
    }

    @Override
    public Mono<Settings> createSettings(Class<?> clazz) {

        Assert.notNull(clazz, "clazz must not be null");

        ElasticsearchPersistentEntity<?> persistentEntity = elasticsearchConverter.getMappingContext()
                .getRequiredPersistentEntity(clazz);
        String settingPath = persistentEntity.settingPath();
        return hasText(settingPath) //
                ? ReactiveResourceUtil.loadDocument(settingPath, "@Setting") //
                        .map(Settings::new) //
                : Mono.just(persistentEntity.getDefaultSettings());
    }

    @Override
    public Mono<Settings> getSettings(final boolean includeDefaults) {

        return observations().mono(OpenSearchOperationName.INDEX_GET_SETTINGS, getIndexCoordinates(), () -> {
            final var getSettingsRequest = requestConverter.indicesGetSettingsRequest(getIndexCoordinates(),
                    includeDefaults);
            final var getSettingsResponse = Mono
                    .from(execute(client -> client.getSettings(getSettingsRequest)));
            return getSettingsResponse
                    .map(response -> responseConverter.indicesGetSettings(response, getIndexCoordinates().getIndexName()));
        });
    }

    @Override
    public Mono<Boolean> alias(final AliasActions aliasActions) {

        Assert.notNull(aliasActions, "aliasActions must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_ALIAS, null, () -> {
            final var updateAliasesRequest = requestConverter.indicesUpdateAliasesRequest(aliasActions);
            final var updateAliasesResponse = Mono
                    .from(execute(client -> client.updateAliases(updateAliasesRequest)));
            return updateAliasesResponse.map(AcknowledgedResponseBase::acknowledged);
        });
    }

    @Override
    public Mono<Map<String, Set<AliasData>>> getAliases(final String... aliasNames) {
        return observations().mono(OpenSearchOperationName.INDEX_GET_ALIASES, null,
                () -> getAliases(aliasNames, null));
    }

    @Override
    public Mono<Map<String, Set<AliasData>>> getAliasesForIndex(final String... indexNames) {
        Assert.notNull(indexNames, "indexNames must not be null");
        return observations().mono(OpenSearchOperationName.INDEX_GET_ALIASES,
                indexNames.length == 0 ? null : IndexCoordinates.of(indexNames), () -> getAliases(null, indexNames));
    }

    private Mono<Map<String, Set<AliasData>>> getAliases(@Nullable String[] aliasNames, @Nullable String[] indexNames) {

        GetAliasRequest getAliasRequest = requestConverter.indicesGetAliasRequest(aliasNames, indexNames);
        Mono<GetAliasResponse> getAliasResponse = Mono.from(execute(client -> client.getAlias(getAliasRequest)));
        return getAliasResponse.map(responseConverter::indicesGetAliasData);
    }

    @Override
    public Mono<Boolean> putTemplate(final PutTemplateRequest putTemplateRequest) {

        Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_PUT_TEMPLATE, null, () -> {
            final var putTemplateRequestES = requestConverter
                    .indicesPutTemplateRequest(putTemplateRequest);
            final var putTemplateResponse = Mono
                    .from(execute(client -> client.putTemplate(putTemplateRequestES)));
            return putTemplateResponse.map(PutTemplateResponse::acknowledged);
        });
    }

    @Override
    public Mono<Boolean> putComponentTemplate(final PutComponentTemplateRequest putComponentTemplateRequest) {

        Assert.notNull(putComponentTemplateRequest, "putComponentTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_PUT_COMPONENT_TEMPLATE, null, () -> {
            final var putComponentTemplateRequestES = requestConverter
                    .clusterPutComponentTemplateRequest(putComponentTemplateRequest);
            return Mono.from(clusterTemplate.execute(client -> client.putComponentTemplate(putComponentTemplateRequestES)))
                    .map(AcknowledgedResponseBase::acknowledged);
        });
    }

    @Override
    public Flux<TemplateResponse> getComponentTemplate(final GetComponentTemplateRequest getComponentTemplateRequest) {

        Assert.notNull(getComponentTemplateRequest, "getComponentTemplateRequest must not be null");

        return observations().flux(OpenSearchOperationName.INDEX_GET_COMPONENT_TEMPLATE, null, null, () -> {
            final var getComponentTemplateRequestES = requestConverter
                    .clusterGetComponentTemplateRequest(getComponentTemplateRequest);
            return Flux.from(clusterTemplate.execute(client -> client.getComponentTemplate(getComponentTemplateRequestES)))
                    .flatMapIterable(responseConverter::clusterGetComponentTemplates);
        });
    }

    @Override
    public Mono<Boolean> existsComponentTemplate(final ExistsComponentTemplateRequest existsComponentTemplateRequest) {

        Assert.notNull(existsComponentTemplateRequest, "existsComponentTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_EXISTS_COMPONENT_TEMPLATE, null, () -> {
            final var existsComponentTemplateRequestES = requestConverter
                    .clusterExistsComponentTemplateRequest(existsComponentTemplateRequest);

            return Mono
                    .from(clusterTemplate.execute(client -> client.existsComponentTemplate(existsComponentTemplateRequestES)))
                    .map(BooleanResponse::value);
        });
    }

    @Override
    public Mono<Boolean> deleteComponentTemplate(final DeleteComponentTemplateRequest deleteComponentTemplateRequest) {

        Assert.notNull(deleteComponentTemplateRequest, "deleteComponentTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_DELETE_COMPONENT_TEMPLATE, null, () -> {
            final var deleteComponentTemplateRequestES = requestConverter
                    .clusterDeleteComponentTemplateRequest(deleteComponentTemplateRequest);
            return Mono
                    .from(clusterTemplate.execute(client -> client.deleteComponentTemplate(deleteComponentTemplateRequestES)))
                    .map(AcknowledgedResponseBase::acknowledged);
        });
    }

    @Override
    public Mono<Boolean> putIndexTemplate(final PutIndexTemplateRequest putIndexTemplateRequest) {

        Assert.notNull(putIndexTemplateRequest, "putIndexTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_PUT_INDEX_TEMPLATE, null, () -> {
            final var putIndexTemplateRequestES = requestConverter
                    .indicesPutIndexTemplateRequest(putIndexTemplateRequest);

            return Mono.from(execute(client -> client.putIndexTemplate(putIndexTemplateRequestES)))
                    .map(PutIndexTemplateResponse::acknowledged);
        });
    }

    @Override
    public Mono<Boolean> existsIndexTemplate(final ExistsIndexTemplateRequest existsIndexTemplateRequest) {

        Assert.notNull(existsIndexTemplateRequest, "existsIndexTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_EXISTS_INDEX_TEMPLATE, null, () -> {
            final var existsIndexTemplateRequestES = requestConverter
                    .indicesExistsIndexTemplateRequest(existsIndexTemplateRequest);
            return Mono.from(execute(client -> client.existsIndexTemplate(existsIndexTemplateRequestES)))
                    .map(BooleanResponse::value);
        });
    }

    @Override
    public Flux<TemplateResponse> getIndexTemplate(final GetIndexTemplateRequest getIndexTemplateRequest) {

        Assert.notNull(getIndexTemplateRequest, "getIndexTemplateRequest must not be null");

        return observations().flux(OpenSearchOperationName.INDEX_GET_INDEX_TEMPLATE, null, null, () -> {
            final var getIndexTemplateRequestES = requestConverter
                    .indicesGetIndexTemplateRequest(getIndexTemplateRequest);
            return Mono.from(execute(client -> client.getIndexTemplate(getIndexTemplateRequestES)))
                    .flatMapIterable(responseConverter::getIndexTemplates);
        });
    }

    @Override
    public Mono<Boolean> deleteIndexTemplate(final DeleteIndexTemplateRequest deleteIndexTemplateRequest) {

        Assert.notNull(deleteIndexTemplateRequest, "deleteIndexTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_DELETE_INDEX_TEMPLATE, null, () -> {
            final var deleteIndexTemplateRequestES = requestConverter
                    .indicesDeleteIndexTemplateRequest(deleteIndexTemplateRequest);
            return Mono.from(execute(client -> client.deleteIndexTemplate(deleteIndexTemplateRequestES)))
                    .map(AcknowledgedResponseBase::acknowledged);
        });
    }

    @Override
    public Mono<TemplateData> getTemplate(final GetTemplateRequest getTemplateRequest) {

        Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_GET_TEMPLATE, null, () -> {
            final var getTemplateRequestES = requestConverter
                    .indicesGetTemplateRequest(getTemplateRequest);
            final var getTemplateResponse = Mono
                    .from(execute(client -> client.getTemplate(getTemplateRequestES)));

            return getTemplateResponse.flatMap(response -> {
                if (response != null) {
                    final var templateData = responseConverter.indicesGetTemplateData(response,
                            getTemplateRequest.getTemplateName());
                    if (templateData != null) {
                        return Mono.just(templateData);
                    }
                }
                return Mono.empty();
            });
        });
    }

    @Override
    public Mono<Boolean> existsTemplate(final ExistsTemplateRequest existsTemplateRequest) {

        Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_EXISTS_TEMPLATE, null, () -> {
            final var existsTemplateRequestES = requestConverter
                    .indicesExistsTemplateRequest(existsTemplateRequest);
            return Mono.from(execute(client -> client.existsTemplate(existsTemplateRequestES))).map(BooleanResponse::value);
        });
    }

    @Override
    public Mono<Boolean> deleteTemplate(final DeleteTemplateRequest deleteTemplateRequest) {

        Assert.notNull(deleteTemplateRequest, "deleteTemplateRequest must not be null");

        return observations().mono(OpenSearchOperationName.INDEX_DELETE_TEMPLATE, null, () -> {
            final var deleteTemplateRequestES = requestConverter
                    .indicesDeleteTemplateRequest(deleteTemplateRequest);
            return Mono.from(execute(client -> client.deleteTemplate(deleteTemplateRequestES)))
                    .map(DeleteTemplateResponse::acknowledged);
        });
    }

    @Override
    public Flux<IndexInformation> getInformation(final IndexCoordinates index) {

        return observations().flux(OpenSearchOperationName.INDEX_GET_INFORMATION, index, null, () -> {
            final var request = requestConverter.indicesGetIndexRequest(index);

            return Mono.from(execute(client -> client.get(request))) //
                    .map(responseConverter::indicesGetIndexInformations) //
                    .flatMapMany(Flux::fromIterable);
        });
    }

    @Override
    public IndexCoordinates getIndexCoordinates() {
        return (boundClass != null) ? getIndexCoordinatesFor(boundClass) : Objects.requireNonNull(boundIndexCoordinates);
    }

    // region helper functions
    private IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
        return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz).getIndexCoordinates();
    }

    /**
     * Get the {@link Alias} of the provided class.
     *
     * @param clazz provided class that can be used to extract aliases.
     */
    private Set<Alias> getAliasesFor(Class<?> clazz) {
        return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz).getAliases();
    }

    private Class<?> checkForBoundClass() {
        if (boundClass == null) {
            throw new InvalidDataAccessApiUsageException("IndexOperations are not bound");
        }
        return boundClass;
    }
    // endregion

}
