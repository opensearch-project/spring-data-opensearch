/*
 * Copyright 2021-2023 the original author or authors.
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

import static org.opensearch.data.client.osc.TypeUtils.*;
import static org.springframework.util.CollectionUtils.*;

import jakarta.json.stream.JsonParser;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.ObjectDeserializer;
import org.opensearch.client.opensearch._types.Conflicts;
import org.opensearch.client.opensearch._types.ExpandWildcard;
import org.opensearch.client.opensearch._types.FieldSort;
import org.opensearch.client.opensearch._types.InlineScript;
import org.opensearch.client.opensearch._types.NestedSortValue;
import org.opensearch.client.opensearch._types.OpType;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch._types.ScriptLanguage;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.VersionType;
import org.opensearch.client.opensearch._types.WaitForActiveShardOptions;
import org.opensearch.client.opensearch._types.mapping.DynamicMapping;
import org.opensearch.client.opensearch._types.mapping.DynamicTemplate;
import org.opensearch.client.opensearch._types.mapping.FieldNamesField;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.RoutingField;
import org.opensearch.client.opensearch._types.mapping.SourceField;
import org.opensearch.client.opensearch._types.query_dsl.FieldAndFormat;
import org.opensearch.client.opensearch._types.query_dsl.Like;
import org.opensearch.client.opensearch.cluster.DeleteComponentTemplateRequest;
import org.opensearch.client.opensearch.cluster.ExistsComponentTemplateRequest;
import org.opensearch.client.opensearch.cluster.GetComponentTemplateRequest;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.PutComponentTemplateRequest;
import org.opensearch.client.opensearch.core.*;
import org.opensearch.client.opensearch.core.CreatePitRequest;
import org.opensearch.client.opensearch.core.DeletePitRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.CreateOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.core.bulk.UpdateOperation;
import org.opensearch.client.opensearch.core.mget.MultiGetOperation;
import org.opensearch.client.opensearch.core.msearch.MultisearchBody;
import org.opensearch.client.opensearch.core.msearch.MultisearchHeader;
import org.opensearch.client.opensearch.core.search.Highlight;
import org.opensearch.client.opensearch.core.search.Pit;
import org.opensearch.client.opensearch.core.search.Rescore;
import org.opensearch.client.opensearch.core.search.SourceConfig;
import org.opensearch.client.opensearch.indices.Alias;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.GetAliasRequest;
import org.opensearch.client.opensearch.indices.GetIndexRequest;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsRequest;
import org.opensearch.client.opensearch.indices.GetMappingRequest;
import org.opensearch.client.opensearch.indices.PutMappingRequest;
import org.opensearch.client.opensearch.indices.PutMappingRequest.Builder;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.opensearch.client.opensearch.indices.UpdateAliasesRequest;
import org.opensearch.client.opensearch.indices.update_aliases.Action;
import org.opensearch.client.util.ObjectBuilder;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.*;
import org.springframework.data.elasticsearch.core.index.DeleteIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.PutTemplateRequest;
import org.springframework.data.elasticsearch.core.mapping.CreateIndexSettings;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentEntity;
import org.springframework.data.elasticsearch.core.mapping.ElasticsearchPersistentProperty;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.data.elasticsearch.core.reindex.Remote;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.data.elasticsearch.support.DefaultStringObjectMap;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Class to create OpenSearch request and request builders.
 *
 * @author Peter-Josef Meisch
 * @author Sascha Woo
 * @author cdalxndr
 * @author scoobyzhang
 * @author Haibo Liu
 * @since 4.4
 */
@SuppressWarnings("ClassCanBeRecord")
class RequestConverter {

    private static final Log LOGGER = LogFactory.getLog(RequestConverter.class);

    // the default max result window size of Elasticsearch
    public static final Integer INDEX_MAX_RESULT_WINDOW = 10_000;

    protected final JsonpMapper jsonpMapper;
    protected final ElasticsearchConverter elasticsearchConverter;

    public RequestConverter(ElasticsearchConverter elasticsearchConverter, JsonpMapper jsonpMapper) {
        this.elasticsearchConverter = elasticsearchConverter;

        Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

        this.jsonpMapper = jsonpMapper;
    }

    // region Cluster client
    public org.opensearch.client.opensearch.cluster.HealthRequest clusterHealthRequest() {
        return new HealthRequest.Builder().build();
    }

    public org.opensearch.client.opensearch.cluster.PutComponentTemplateRequest clusterPutComponentTemplateRequest(
            org.springframework.data.elasticsearch.core.index.PutComponentTemplateRequest putComponentTemplateRequest) {

        Assert.notNull(putComponentTemplateRequest, "putComponentTemplateRequest must not be null");

        return PutComponentTemplateRequest.of(b -> b //
                .name(putComponentTemplateRequest.name()) //
                .create(putComponentTemplateRequest.create()) //
                .version(putComponentTemplateRequest.version()) //
                .masterTimeout(time(putComponentTemplateRequest.masterTimeout())) //
                .template(isb -> {
                    var componentTemplateData = putComponentTemplateRequest.template();
                    isb //
                            .mappings(typeMapping(componentTemplateData.mapping())) //
                            .settings(indexSettings(componentTemplateData.settings()));
                    // same code schema, but different Elasticsearch builder types
                    // noinspection DuplicatedCode
                    var aliasActions = componentTemplateData.aliasActions();
                    if (aliasActions != null) {
                        aliasActions.getActions().forEach(aliasAction -> {
                            if (aliasAction instanceof AliasAction.Add add) {
                                var parameters = add.getParameters();
                                // noinspection DuplicatedCode
                                String[] parametersAliases = parameters.getAliases();
                                if (parametersAliases != null) {
                                    for (String aliasName : parametersAliases) {
                                        isb.aliases(aliasName, aliasBuilder -> buildAlias(parameters, aliasBuilder));
                                    }
                                }
                            }
                        });
                    }
                    return isb;
                }));
    }

    private Alias.Builder buildAlias(AliasActionParameters parameters, Alias.Builder aliasBuilder) {

        // noinspection DuplicatedCode
        if (parameters.getRouting() != null) {
            aliasBuilder.routing(parameters.getRouting());
        }

        if (parameters.getIndexRouting() != null) {
            aliasBuilder.indexRouting(parameters.getIndexRouting());
        }

        if (parameters.getSearchRouting() != null) {
            aliasBuilder.searchRouting(parameters.getSearchRouting());
        }

        if (parameters.getHidden() != null) {
            aliasBuilder.isHidden(parameters.getHidden());
        }

        if (parameters.getWriteIndex() != null) {
            aliasBuilder.isWriteIndex(parameters.getWriteIndex());
        }

        Query filterQuery = parameters.getFilterQuery();

        if (filterQuery != null) {
            org.opensearch.client.opensearch._types.query_dsl.Query esQuery = getQuery(filterQuery, null);

            if (esQuery != null) {
                aliasBuilder.filter(esQuery);
            }
        }
        return aliasBuilder;
    }

    public ExistsComponentTemplateRequest clusterExistsComponentTemplateRequest(
            org.springframework.data.elasticsearch.core.index.ExistsComponentTemplateRequest existsComponentTemplateRequest) {

        Assert.notNull(existsComponentTemplateRequest, "existsComponentTemplateRequest must not be null");

        return ExistsComponentTemplateRequest.of(b -> b.name(existsComponentTemplateRequest.templateName()));
    }

    public GetComponentTemplateRequest clusterGetComponentTemplateRequest(
            org.springframework.data.elasticsearch.core.index.GetComponentTemplateRequest getComponentTemplateRequest) {

        Assert.notNull(getComponentTemplateRequest, "getComponentTemplateRequest must not be null");

        return GetComponentTemplateRequest.of(b -> b.name(getComponentTemplateRequest.templateName()));
    }

    public DeleteComponentTemplateRequest clusterDeleteComponentTemplateRequest(
            org.springframework.data.elasticsearch.core.index.DeleteComponentTemplateRequest deleteComponentTemplateRequest) {
        return DeleteComponentTemplateRequest.of(b -> b.name(deleteComponentTemplateRequest.templateName()));
    }
    // endregion

    // region Indices client
    public ExistsRequest indicesExistsRequest(IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return new ExistsRequest.Builder().index(Arrays.asList(indexCoordinates.getIndexNames())).build();
    }

    public CreateIndexRequest indicesCreateRequest(IndexCoordinates indexCoordinates, Map<String, Object> settings,
            @Nullable Document mapping) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");
        Assert.notNull(settings, "settings must not be null");

        // note: the new client does not support the index.storeType anymore
        return new CreateIndexRequest.Builder() //
                .index(indexCoordinates.getIndexName()) //
                .settings(indexSettings(settings)) //
                .mappings(typeMapping(mapping)) //
                .build();
    }

    public CreateIndexRequest indicesCreateRequest(CreateIndexSettings indexSettings) {
        Map<String, org.opensearch.client.opensearch.indices.Alias> aliases = new HashMap<>();
        for (org.springframework.data.elasticsearch.core.mapping.Alias alias : indexSettings.getAliases()) {
            org.opensearch.client.opensearch.indices.Alias esAlias = org.opensearch.client.opensearch.indices.Alias
                    .of(ab -> ab.filter(getQuery(alias.getFilter(), null))
                            .routing(alias.getRouting())
                            .indexRouting(alias.getIndexRouting())
                            .searchRouting(alias.getSearchRouting())
                            .isHidden(alias.getHidden())
                            .isWriteIndex(alias.getWriteIndex()));
            aliases.put(alias.getAlias(), esAlias);
        }

        // note: the new client does not support the index.storeType anymore
        return new CreateIndexRequest.Builder() //
                .index(indexSettings.getIndexCoordinates().getIndexName()) //
                .aliases(aliases)
                .settings(indexSettings(indexSettings.getSettings())) //
                .mappings(typeMapping(indexSettings.getMapping())) //
                .build();
    }

    public RefreshRequest indicesRefreshRequest(IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return new RefreshRequest.Builder().index(Arrays.asList(indexCoordinates.getIndexNames())).build();
    }

    public DeleteIndexRequest indicesDeleteRequest(IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return new DeleteIndexRequest.Builder().index(Arrays.asList(indexCoordinates.getIndexNames())).build();
    }

    public UpdateAliasesRequest indicesUpdateAliasesRequest(AliasActions aliasActions) {

        Assert.notNull(aliasActions, "aliasActions must not be null");

        UpdateAliasesRequest.Builder updateAliasRequestBuilder = new UpdateAliasesRequest.Builder();

        List<Action> actions = new ArrayList<>();
        aliasActions.getActions().forEach(aliasAction -> {

            var actionBuilder = getBuilder(aliasAction);

            if (aliasAction instanceof AliasAction.Remove remove) {
                AliasActionParameters parameters = remove.getParameters();
                actionBuilder.remove(removeActionBuilder -> {
                    removeActionBuilder.indices(Arrays.asList(parameters.getIndices()));

                    if (parameters.getAliases() != null) {
                        removeActionBuilder.aliases(Arrays.asList(parameters.getAliases()));
                    }

                    return removeActionBuilder;
                });
            }

            if (aliasAction instanceof AliasAction.RemoveIndex removeIndex) {
                AliasActionParameters parameters = removeIndex.getParameters();
                actionBuilder.removeIndex(
                        removeIndexActionBuilder -> removeIndexActionBuilder.indices(Arrays.asList(parameters.getIndices())));
            }

            actions.add(actionBuilder.build());
        });

        updateAliasRequestBuilder.actions(actions);

        return updateAliasRequestBuilder.build();
    }

    @NonNull
    private Action.Builder getBuilder(AliasAction aliasAction) {
        Action.Builder actionBuilder = new Action.Builder();

        if (aliasAction instanceof AliasAction.Add add) {
            AliasActionParameters parameters = add.getParameters();
            actionBuilder.add(addActionBuilder -> {
                addActionBuilder //
                        .indices(Arrays.asList(parameters.getIndices())) //
                        .isHidden(parameters.getHidden()) //
                        .isWriteIndex(parameters.getWriteIndex()) //
                        .routing(parameters.getRouting()) //
                        .indexRouting(parameters.getIndexRouting()) //
                        .searchRouting(parameters.getSearchRouting()); //

                if (parameters.getAliases() != null) {
                    addActionBuilder.aliases(Arrays.asList(parameters.getAliases()));
                }

                Query filterQuery = parameters.getFilterQuery();

                if (filterQuery != null) {
                    elasticsearchConverter.updateQuery(filterQuery, parameters.getFilterQueryClass());
                    org.opensearch.client.opensearch._types.query_dsl.Query esQuery = getQuery(filterQuery, null);
                    if (esQuery != null) {
                        addActionBuilder.filter(esQuery);
                    }
                }
                return addActionBuilder;
            });
        }
        return actionBuilder;
    }

    public PutMappingRequest indicesPutMappingRequest(IndexCoordinates indexCoordinates, Document mapping) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");
        Assert.notNull(mapping, "mapping must not be null");

        final ObjectDeserializer<Builder> deserializer = new ObjectDeserializer<>(PutMappingRequest.Builder::new);
        deserializer.add(Builder::fieldNames, FieldNamesField._DESERIALIZER, "_field_names");
        deserializer.add(Builder::meta, JsonpDeserializer.stringMapDeserializer(JsonData._DESERIALIZER), "_meta");
        deserializer.add(Builder::routing, RoutingField._DESERIALIZER, "_routing");
        deserializer.add(Builder::source, SourceField._DESERIALIZER, "_source");
        deserializer.add(Builder::dateDetection, JsonpDeserializer.booleanDeserializer(), "date_detection");
        deserializer.add(Builder::dynamic, DynamicMapping._DESERIALIZER, "dynamic");
        deserializer.add(
            Builder::dynamicDateFormats,
            JsonpDeserializer.arrayDeserializer(JsonpDeserializer.stringDeserializer()),
            "dynamic_date_formats"
        );
        deserializer.add(
            Builder::dynamicTemplates,
            JsonpDeserializer.arrayDeserializer(JsonpDeserializer.stringMapDeserializer(DynamicTemplate._DESERIALIZER)),
            "dynamic_templates"
        );
        deserializer.add(Builder::numericDetection, JsonpDeserializer.booleanDeserializer(), "numeric_detection");
        deserializer.add(Builder::properties, JsonpDeserializer.stringMapDeserializer(Property._DESERIALIZER), "properties");

        final PutMappingRequest.Builder builder = JsonpUtils.fromJson(mapping, deserializer);
        builder.index(Arrays.asList(indexCoordinates.getIndexNames()));

        return builder.build();
    }

    public GetMappingRequest indicesGetMappingRequest(IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return new GetMappingRequest.Builder().index(List.of(indexCoordinates.getIndexNames())).build();
    }

    public GetIndicesSettingsRequest indicesGetSettingsRequest(IndexCoordinates indexCoordinates,
            boolean includeDefaults) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return new GetIndicesSettingsRequest.Builder() //
                .index(Arrays.asList(indexCoordinates.getIndexNames())) //
                .includeDefaults(includeDefaults) //
                .build();
    }

    public GetIndexRequest indicesGetIndexRequest(IndexCoordinates indexCoordinates) {

        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return new GetIndexRequest.Builder() //
                .index(Arrays.asList(indexCoordinates.getIndexNames())) //
                .includeDefaults(true) //
                .build(); //
    }

    public GetAliasRequest indicesGetAliasRequest(@Nullable String[] aliasNames, @Nullable String[] indexNames) {
        GetAliasRequest.Builder builder = new GetAliasRequest.Builder();

        if (aliasNames != null) {
            builder.name(Arrays.asList(aliasNames));
        }

        if (indexNames != null) {
            builder.index(Arrays.asList(indexNames));
        }

        return builder.build();
    }

    public org.opensearch.client.opensearch.indices.PutTemplateRequest indicesPutTemplateRequest(
            PutTemplateRequest putTemplateRequest) {

        Assert.notNull(putTemplateRequest, "putTemplateRequest must not be null");

        org.opensearch.client.opensearch.indices.PutTemplateRequest.Builder builder = new org.opensearch.client.opensearch.indices.PutTemplateRequest.Builder();

        builder.name(putTemplateRequest.getName()).indexPatterns(Arrays.asList(putTemplateRequest.getIndexPatterns()))
                .order(putTemplateRequest.getOrder());

        if (putTemplateRequest.getSettings() != null) {
            Map<String, JsonData> settings = getTemplateParams(putTemplateRequest.getSettings().entrySet());
            builder.settings(settings);
        }

        if (putTemplateRequest.getMappings() != null) {
            builder.mappings(typeMapping(putTemplateRequest.getMappings()));
        }

        if (putTemplateRequest.getVersion() != null) {
            builder.version(Long.valueOf(putTemplateRequest.getVersion()));
        }
        AliasActions aliasActions = putTemplateRequest.getAliasActions();

        if (aliasActions != null) {
            aliasActions.getActions().forEach(aliasAction -> {
                AliasActionParameters parameters = aliasAction.getParameters();
                // noinspection DuplicatedCode
                String[] parametersAliases = parameters.getAliases();

                if (parametersAliases != null) {
                    for (String aliasName : parametersAliases) {
                        builder.aliases(aliasName, aliasBuilder -> buildAlias(parameters, aliasBuilder));
                    }
                }
            });
        }

        return builder.build();
    }

    public org.opensearch.client.opensearch.indices.PutIndexTemplateRequest indicesPutIndexTemplateRequest(
            PutIndexTemplateRequest putIndexTemplateRequest) {

        Assert.notNull(putIndexTemplateRequest, "putIndexTemplateRequest must not be null");

        var builder = new org.opensearch.client.opensearch.indices.PutIndexTemplateRequest.Builder()
                .name(putIndexTemplateRequest.name()) //
                .indexPatterns(Arrays.asList(putIndexTemplateRequest.indexPatterns())) //
                .template(t -> {
                    t //
                            .settings(indexSettings(putIndexTemplateRequest.settings())) //
                            .mappings(typeMapping(putIndexTemplateRequest.mapping()));

                    // same code schema, but different Elasticsearch builder types
                    // noinspection DuplicatedCode
                    var aliasActions = putIndexTemplateRequest.aliasActions();
                    if (aliasActions != null) {
                        aliasActions.getActions().forEach(aliasAction -> {
                            if (aliasAction instanceof AliasAction.Add add) {
                                var parameters = add.getParameters();
                                // noinspection DuplicatedCode
                                String[] parametersAliases = parameters.getAliases();
                                if (parametersAliases != null) {
                                    for (String aliasName : parametersAliases) {
                                        t.aliases(aliasName, aliasBuilder -> buildAlias(parameters, aliasBuilder));
                                    }
                                }
                            }
                        });
                    }
                    return t;
                });

        if (!putIndexTemplateRequest.composedOf().isEmpty()) {
            builder.composedOf(putIndexTemplateRequest.composedOf());
        }

        return builder.build();
    }

    public ExistsIndexTemplateRequest indicesExistsIndexTemplateRequest(
            org.springframework.data.elasticsearch.core.index.ExistsIndexTemplateRequest existsIndexTemplateRequest) {

        Assert.notNull(existsIndexTemplateRequest, "existsIndexTemplateRequest must not be null");

        return org.opensearch.client.opensearch.indices.ExistsIndexTemplateRequest
                .of(b -> b.name(existsIndexTemplateRequest.templateName()));
    }

    public org.opensearch.client.opensearch.indices.ExistsTemplateRequest indicesExistsTemplateRequest(
            ExistsTemplateRequest existsTemplateRequest) {

        Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

        return org.opensearch.client.opensearch.indices.ExistsTemplateRequest
                .of(etr -> etr.name(existsTemplateRequest.getTemplateName()));
    }

    public org.opensearch.client.opensearch.indices.GetIndexTemplateRequest indicesGetIndexTemplateRequest(
            GetIndexTemplateRequest getIndexTemplateRequest) {

        Assert.notNull(getIndexTemplateRequest, "getIndexTemplateRequest must not be null");

        return org.opensearch.client.opensearch.indices.GetIndexTemplateRequest
                .of(gitr -> gitr.name(getIndexTemplateRequest.templateName()));
    }

    public org.opensearch.client.opensearch.indices.DeleteIndexTemplateRequest indicesDeleteIndexTemplateRequest(
            DeleteIndexTemplateRequest deleteIndexTemplateRequest) {

        Assert.notNull(deleteIndexTemplateRequest, "deleteIndexTemplateRequest must not be null");

        return org.opensearch.client.opensearch.indices.DeleteIndexTemplateRequest
                .of(ditr -> ditr.name(deleteIndexTemplateRequest.templateName()));
    }

    public org.opensearch.client.opensearch.indices.DeleteTemplateRequest indicesDeleteTemplateRequest(
            DeleteTemplateRequest existsTemplateRequest) {

        Assert.notNull(existsTemplateRequest, "existsTemplateRequest must not be null");

        return org.opensearch.client.opensearch.indices.DeleteTemplateRequest
                .of(dtr -> dtr.name(existsTemplateRequest.getTemplateName()));
    }

    public org.opensearch.client.opensearch.indices.GetTemplateRequest indicesGetTemplateRequest(
            GetTemplateRequest getTemplateRequest) {

        Assert.notNull(getTemplateRequest, "getTemplateRequest must not be null");

        return org.opensearch.client.opensearch.indices.GetTemplateRequest
                .of(gtr -> gtr.name(getTemplateRequest.getTemplateName()).flatSettings(true));
    }

    // endregion

    // region documents
    /*
     * the methods documentIndexRequest, bulkIndexOperation and bulkCreateOperation have nearly
     * identical code, but the client builders do not have a common accessible base or some reusable parts
     * so the code needs to be duplicated.
     */

    public IndexRequest<?> documentIndexRequest(IndexQuery query, IndexCoordinates indexCoordinates,
            @Nullable RefreshPolicy refreshPolicy) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        IndexRequest.Builder<Object> builder = new IndexRequest.Builder<>();

        builder.index(query.getIndexName() != null ? query.getIndexName() : indexCoordinates.getIndexName());

        Object queryObject = query.getObject();

        if (queryObject != null) {
            String id = StringUtils.hasText(query.getId()) ? query.getId() : getPersistentEntityId(queryObject);
            builder //
                    .id(id) //
                    .document(elasticsearchConverter.mapObject(queryObject));
        } else if (query.getSource() != null) {
            builder //
                    .id(query.getId()) //
                    .document(new DefaultStringObjectMap<>().fromJson(query.getSource()));
        } else {
            throw new InvalidDataAccessApiUsageException(
                    "object or source is null, failed to index the document [id: " + query.getId() + ']');
        }

        if (query.getVersion() != null) {
            VersionType versionType = retrieveVersionTypeFromPersistentEntity(
                    queryObject != null ? queryObject.getClass() : null);
            builder.version(query.getVersion()).versionType(versionType);
        }

        builder //
                .ifSeqNo(query.getSeqNo()) //
                .ifPrimaryTerm(query.getPrimaryTerm()) //
                .routing(query.getRouting()); //

        if (query.getOpType() != null) {
            switch (query.getOpType()) {
                case INDEX -> builder.opType(OpType.Index);
                case CREATE -> builder.opType(OpType.Create);
            }
        }

        builder.refresh(refresh(refreshPolicy));

        return builder.build();
    }
    /*
     * the methods documentIndexRequest, bulkIndexOperation and bulkCreateOperation have nearly
     * identical code, but the client builders do not have a common accessible base or some reusable parts
     * so the code needs to be duplicated.
     */

    @SuppressWarnings("DuplicatedCode")
    private IndexOperation<?> bulkIndexOperation(IndexQuery query, IndexCoordinates indexCoordinates,
            @Nullable RefreshPolicy refreshPolicy) {

        IndexOperation.Builder<Object> builder = new IndexOperation.Builder<>();

        builder.index(query.getIndexName() != null ? query.getIndexName() : indexCoordinates.getIndexName());

        Object queryObject = query.getObject();

        if (queryObject != null) {
            String id = StringUtils.hasText(query.getId()) ? query.getId() : getPersistentEntityId(queryObject);
            builder //
                    .id(id) //
                    .document(elasticsearchConverter.mapObject(queryObject));
        } else if (query.getSource() != null) {
            builder.document(new DefaultStringObjectMap<>().fromJson(query.getSource()));
        } else {
            throw new InvalidDataAccessApiUsageException(
                    "object or source is null, failed to index the document [id: " + query.getId() + ']');
        }

        if (query.getVersion() != null) {
            VersionType versionType = retrieveVersionTypeFromPersistentEntity(
                    queryObject != null ? queryObject.getClass() : null);
            builder.version(query.getVersion()).versionType(versionType);
        }

        builder //
                .ifSeqNo(query.getSeqNo()) //
                .ifPrimaryTerm(query.getPrimaryTerm()) //
                .routing(query.getRouting()); //

        return builder.build();
    }
    /*
     * the methods documentIndexRequest, bulkIndexOperation and bulkCreateOperation have nearly
     * identical code, but the client builders do not have a common accessible base or some reusable parts
     * so the code needs to be duplicated.
     */

    @SuppressWarnings("DuplicatedCode")
    private CreateOperation<?> bulkCreateOperation(IndexQuery query, IndexCoordinates indexCoordinates,
            @Nullable RefreshPolicy refreshPolicy) {

        CreateOperation.Builder<Object> builder = new CreateOperation.Builder<>();

        builder.index(query.getIndexName() != null ? query.getIndexName() : indexCoordinates.getIndexName());

        Object queryObject = query.getObject();

        if (queryObject != null) {
            String id = StringUtils.hasText(query.getId()) ? query.getId() : getPersistentEntityId(queryObject);
            builder //
                    .id(id) //
                    .document(elasticsearchConverter.mapObject(queryObject));
        } else if (query.getSource() != null) {
            builder.document(new DefaultStringObjectMap<>().fromJson(query.getSource()));
        } else {
            throw new InvalidDataAccessApiUsageException(
                    "object or source is null, failed to index the document [id: " + query.getId() + ']');
        }

        if (query.getVersion() != null) {
            VersionType versionType = retrieveVersionTypeFromPersistentEntity(
                    queryObject != null ? queryObject.getClass() : null);
            builder.version(query.getVersion()).versionType(versionType);
        }

        builder //
                .ifSeqNo(query.getSeqNo()) //
                .ifPrimaryTerm(query.getPrimaryTerm()) //
                .routing(query.getRouting()); //

        return builder.build();
    }

    private UpdateOperation<?> bulkUpdateOperation(UpdateQuery query, IndexCoordinates index,
            @Nullable RefreshPolicy refreshPolicy) {

        UpdateOperation.Builder<Object> uob = new UpdateOperation.Builder<>();
        String indexName = query.getIndexName() != null ? query.getIndexName() : index.getIndexName();

        uob.index(indexName).id(query.getId());
        uob.script(getScript(query.getScriptData())) //
            .document(query.getDocument()) //
            .upsert(query.getUpsert()) //
            .scriptedUpsert(query.getScriptedUpsert()) //
            .docAsUpsert(query.getDocAsUpsert()) //
        ;

        if (query.getFetchSource() != null) {
            uob.source(sc -> sc.fetch(query.getFetchSource()));
        }

        if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
            List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
                    : Collections.emptyList();
            List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
                    : Collections.emptyList();
            uob.source(sc -> sc.filter(sf -> sf.includes(includes).excludes(excludes)));
        }

        uob //
                .routing(query.getRouting()) //
                .ifSeqNo(query.getIfSeqNo() != null ? Long.valueOf(query.getIfSeqNo()) : null) //
                .ifPrimaryTerm(query.getIfPrimaryTerm() != null ? Long.valueOf(query.getIfPrimaryTerm()) : null) //
                .retryOnConflict(query.getRetryOnConflict()) //
        ;

        // no refresh, timeout, waitForActiveShards on UpdateOperation or UpdateAction

        return uob.build();
    }

    @Nullable
    private org.opensearch.client.opensearch._types.Script getScript(@Nullable ScriptData scriptData) {

        if (scriptData == null) {
            return null;
        }

        Map<String, JsonData> params = new HashMap<>();

        if (scriptData.params() != null) {
            scriptData.params().forEach((key, value) -> params.put(key, JsonData.of(value, jsonpMapper)));
        }
        return org.opensearch.client.opensearch._types.Script.of(sb -> {
            if (scriptData.type() == ScriptType.INLINE) {
                sb.inline(is -> is //
                        .lang(fn -> fn.custom(scriptData.language())) //
                        .source(scriptData.script()) //
                        .params(params)); //
            } else if (scriptData.type() == ScriptType.STORED) {
                sb.stored(ss -> ss //
                        .id(scriptData.script()) //
                        .params(params) //
                );
            }
            return sb;
        });
    }

    public BulkRequest documentBulkRequest(List<?> queries, BulkOptions bulkOptions, IndexCoordinates indexCoordinates,
            @Nullable RefreshPolicy refreshPolicy) {

        BulkRequest.Builder builder = new BulkRequest.Builder();

        if (bulkOptions.getTimeout() != null) {
            builder.timeout(tb -> tb.time(Long.valueOf(bulkOptions.getTimeout().toMillis()).toString() + "ms"));
        }

        builder.refresh(refresh(refreshPolicy));
        if (bulkOptions.getRefreshPolicy() != null) {
            builder.refresh(refresh(bulkOptions.getRefreshPolicy()));
        }

        if (bulkOptions.getWaitForActiveShards() != null) {
            builder.waitForActiveShards(wasb -> wasb.count(bulkOptions.getWaitForActiveShards().value()));
        }

        if (bulkOptions.getPipeline() != null) {
            builder.pipeline(bulkOptions.getPipeline());
        }

        if (bulkOptions.getRoutingId() != null) {
            builder.routing(bulkOptions.getRoutingId());
        }

        List<BulkOperation> operations = queries.stream().map(query -> {
            BulkOperation.Builder ob = new BulkOperation.Builder();
            if (query instanceof IndexQuery indexQuery) {

                if (indexQuery.getOpType() == IndexQuery.OpType.CREATE) {
                    ob.create(bulkCreateOperation(indexQuery, indexCoordinates, refreshPolicy));
                } else {
                    ob.index(bulkIndexOperation(indexQuery, indexCoordinates, refreshPolicy));
                }
            } else if (query instanceof UpdateQuery updateQuery) {
                ob.update(bulkUpdateOperation(updateQuery, indexCoordinates, refreshPolicy));
            }
            return ob.build();
        }).collect(Collectors.toList());

        builder.operations(operations);

        return builder.build();
    }

    public GetRequest documentGetRequest(String id, @Nullable String routing, IndexCoordinates indexCoordinates) {

        Assert.notNull(id, "id must not be null");
        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return GetRequest.of(grb -> grb //
                .index(indexCoordinates.getIndexName()) //
                .id(id) //
                .routing(routing));
    }

    public org.opensearch.client.opensearch.core.ExistsRequest documentExistsRequest(String id, @Nullable String routing,
            IndexCoordinates indexCoordinates) {

        Assert.notNull(id, "id must not be null");
        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        return org.opensearch.client.opensearch.core.ExistsRequest.of(erb -> erb
                .index(indexCoordinates.getIndexName())
                .id(id)
                .routing(routing));
    }

    public <T> MgetRequest documentMgetRequest(Query query, Class<T> clazz, IndexCoordinates index) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(clazz, "clazz must not be null");
        Assert.notNull(index, "index must not be null");

        if (query.getIdsWithRouting().isEmpty()) {
            throw new IllegalArgumentException("query does not contain any ids");
        }

        elasticsearchConverter.updateQuery(query, clazz); // to get the SourceConfig right

        SourceConfig sourceConfig = getSourceConfig(query);

        List<MultiGetOperation> multiGetOperations = query.getIdsWithRouting().stream()
                .map(idWithRouting -> MultiGetOperation.of(mgo -> mgo //
                        .index(index.getIndexName()) //
                        .id(idWithRouting.id()) //
                        .routing(idWithRouting.routing()) //
                        .source(sourceConfig)))
                .collect(Collectors.toList());

        return MgetRequest.of(mg -> mg//
                .docs(multiGetOperations));
    }

    public org.opensearch.client.opensearch.core.ReindexRequest reindex(ReindexRequest reindexRequest,
            boolean waitForCompletion) {

        Assert.notNull(reindexRequest, "reindexRequest must not be null");

        org.opensearch.client.opensearch.core.ReindexRequest.Builder builder = new org.opensearch.client.opensearch.core.ReindexRequest.Builder();
        builder //
                .source(s -> {
                    ReindexRequest.Source source = reindexRequest.getSource();
                    s.index(Arrays.asList(source.getIndexes().getIndexNames())) //
                            .size(source.getSize());

                    ReindexRequest.Slice slice = source.getSlice();
                    if (slice != null) {
                        s.slice(sl -> sl.id(slice.getId()).max(slice.getMax()));
                    }

                    if (source.getQuery() != null) {
                        s.query(getQuery(source.getQuery(), null));
                    }

                    if (source.getRemote() != null) {
                        Remote remote = source.getRemote();

                        s.remote(rs -> {
                            StringBuilder sb = new StringBuilder(remote.getScheme());
                            sb.append("://");
                            sb.append(remote.getHost());
                            sb.append(':');
                            sb.append(remote.getPort());

                            if (remote.getPathPrefix() != null) {
                                sb.append(remote.getPathPrefix());
                            }

                            String socketTimeoutSecs = remote.getSocketTimeout() != null
                                    ? remote.getSocketTimeout().getSeconds() + "s"
                                    : "30s";
                            String connectTimeoutSecs = remote.getConnectTimeout() != null
                                    ? remote.getConnectTimeout().getSeconds() + "s"
                                    : "30s";
                            return rs //
                                    .host(sb.toString()) //
                                    .username(remote.getUsername()) //
                                    .password(remote.getPassword()) //
                                    .socketTimeout(tv -> tv.time(socketTimeoutSecs)) //
                                    .connectTimeout(tv -> tv.time(connectTimeoutSecs));
                        });
                    }

                    SourceFilter sourceFilter = source.getSourceFilter();
                    if (sourceFilter != null && sourceFilter.getIncludes() != null) {
                        s.sourceFields(Arrays.asList(sourceFilter.getIncludes()));
                    }
                    return s;
                }) //
                .dest(d -> {
                    ReindexRequest.Dest dest = reindexRequest.getDest();
                    return d //
                            .index(dest.getIndex().getIndexName()) //
                            .versionType(versionType(dest.getVersionType())) //
                            .opType(opType(dest.getOpType()));
                } //
                );

        if (reindexRequest.getConflicts() != null) {
            builder.conflicts(conflicts(reindexRequest.getConflicts()));
        }

        ReindexRequest.Script script = reindexRequest.getScript();
        if (script != null) {
            builder.script(s -> s.inline(InlineScript.of(i -> i.lang(fn -> fn.custom(script.getLang()))
                    .source(script.getSource()))));
        }

        builder.timeout(time(reindexRequest.getTimeout())) //
                .scroll(time(reindexRequest.getScroll()));

        if (reindexRequest.getWaitForActiveShards() != null) {
            builder.waitForActiveShards(wfas -> wfas //
                    .count(waitForActiveShardsCount(reindexRequest.getWaitForActiveShards())));
        }

        builder //
                .maxDocs(toInt(reindexRequest.getMaxDocs()))
                .waitForCompletion(waitForCompletion) //
                .requireAlias(reindexRequest.getRequireAlias()) //
                .requestsPerSecond(toFloat(reindexRequest.getRequestsPerSecond()));

        if (reindexRequest.getSlices() != null) {
            builder.slices(fn -> fn.count(reindexRequest.getSlices().intValue()));
        }

        if (reindexRequest.getRefresh() != null) {
            builder.refresh(reindexRequest.getRefresh() ? Refresh.True : Refresh.False);
        }

        return builder.build();
    }

    public DeleteRequest documentDeleteRequest(String id, @Nullable String routing, IndexCoordinates index,
            @Nullable RefreshPolicy refreshPolicy) {

        Assert.notNull(id, "id must not be null");
        Assert.notNull(index, "index must not be null");

        return DeleteRequest.of(r -> {
            r.id(id).index(index.getIndexName());

            if (routing != null) {
                r.routing(routing);
            }
            r.refresh(refresh(refreshPolicy));
            return r;
        });
    }

    public DeleteByQueryRequest documentDeleteByQueryRequest(DeleteQuery query, @Nullable String routing, Class<?> clazz,
            IndexCoordinates index, @Nullable RefreshPolicy refreshPolicy) {
        Assert.notNull(query, "query must not be null");
        Assert.notNull(index, "index must not be null");

        return DeleteByQueryRequest.of(dqb -> {
            dqb.index(Arrays.asList(index.getIndexNames())) //
                    .query(getQuery(query.getQuery(), clazz))//
                    .refresh(deleteByQueryRefresh(refreshPolicy))
                    .requestsPerSecond(query.getRequestsPerSecond())
                    .maxDocs(toInt(query.getMaxDocs()))
                    .scroll(time(query.getScroll()))
                    .scrollSize(toInt(query.getScrollSize()));

            if (query.getRouting() != null) {
                dqb.routing(query.getRouting());
            } else if (StringUtils.hasText(routing)) {
                dqb.routing(routing);
            }

            if (query.getQ() != null) {
                dqb.q(query.getQ())
                        .analyzer(query.getAnalyzer())
                        .analyzeWildcard(query.getAnalyzeWildcard())
                        .defaultOperator(operator(query.getDefaultOperator()))
                        .df(query.getDf())
                        .lenient(query.getLenient());
            }

            if (query.getExpandWildcards() != null && !query.getExpandWildcards().isEmpty()) {
                dqb.expandWildcards(expandWildcards(query.getExpandWildcards()));
            }
            if (query.getStats() != null && !query.getStats().isEmpty()) {
                dqb.stats(query.getStats());
            }
            if (query.getSlices() != null) {
                dqb.slices(fn -> fn.count(query.getSlices()));
            }
            if (query.getSort() != null) {
                ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(clazz);
                List<SortOptions> sortOptions = getSortOptions(query.getSort(), persistentEntity);

                if (!sortOptions.isEmpty()) {
                    dqb.sort(
                            sortOptions.stream()
                                    .map(sortOption -> {
                                        String order = "asc";
                                        var sortField = sortOption.field();
                                        if (sortField.order() != null) {
                                            order = sortField.order().jsonValue();
                                        }

                                        return sortField.field() + ":" + order;
                                    })
                                    .collect(Collectors.toList()));
                }
            }
            if (query.getRefresh() != null) {
                dqb.refresh(query.getRefresh() ? Refresh.True : Refresh.False);
            }
            dqb.allowNoIndices(query.getAllowNoIndices())
                    .conflicts(conflicts(query.getConflicts()))
                    .ignoreUnavailable(query.getIgnoreUnavailable())
                    .preference(query.getPreference())
                    .requestCache(query.getRequestCache())
                    .searchType(searchType(query.getSearchType()))
                    .searchTimeout(time(query.getSearchTimeout()))
                    .terminateAfter(toInt(query.getTerminateAfter()))
                    .timeout(time(query.getTimeout()))
                    .version(query.getVersion());

            return dqb;
        });
    }

    public DeleteByQueryRequest documentDeleteByQueryRequest(Query query, @Nullable String routing, Class<?> clazz,
            IndexCoordinates index, @Nullable RefreshPolicy refreshPolicy) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(index, "index must not be null");

        return DeleteByQueryRequest.of(b -> {
            b.index(Arrays.asList(index.getIndexNames())) //
                    .query(getQuery(query, clazz))//
                    .refresh(deleteByQueryRefresh(refreshPolicy));

            if (query.isLimiting()) {
                // noinspection ConstantConditions
                b.maxDocs(query.getMaxResults());
            }

            b.scroll(time(query.getScrollTime()));

            if (query.getRoute() != null) {
                b.routing(query.getRoute());
            } else if (StringUtils.hasText(routing)) {
                b.routing(routing);
            }

            return b;
        });
    }

    public UpdateRequest<Document, ?> documentUpdateRequest(UpdateQuery query, IndexCoordinates index,
            @Nullable RefreshPolicy refreshPolicy, @Nullable String routing) {

        String indexName = query.getIndexName() != null ? query.getIndexName() : index.getIndexName();
        return UpdateRequest.of(uqb -> {
            uqb.index(indexName).id(query.getId());

            if (query.getScript() != null) {
                Map<String, JsonData> params = new HashMap<>();

                if (query.getParams() != null) {
                    query.getParams().forEach((key, value) -> params.put(key, JsonData.of(value, jsonpMapper)));
                }

                uqb.script(sb -> {
                    if (query.getScriptType() == ScriptType.INLINE) {
                        sb.inline(is -> is //
                                .lang(fn -> fn.custom(query.getLang())) //
                                .source(query.getScript()) //
                                .params(params)); //
                    } else if (query.getScriptType() == ScriptType.STORED) {
                        sb.stored(ss -> ss //
                                .id(query.getScript()) //
                                .params(params) //
                        );
                    }
                    return sb;
                }

                );
            }

            uqb //
                    .doc(query.getDocument()) //
                    .upsert(query.getUpsert()) //
                    .routing(query.getRouting() != null ? query.getRouting() : routing) //
                    .scriptedUpsert(query.getScriptedUpsert()) //
                    .docAsUpsert(query.getDocAsUpsert()) //
                    .ifSeqNo(query.getIfSeqNo() != null ? Long.valueOf(query.getIfSeqNo()) : null) //
                    .ifPrimaryTerm(query.getIfPrimaryTerm() != null ? Long.valueOf(query.getIfPrimaryTerm()) : null) //
                    .refresh(query.getRefreshPolicy() != null ? refresh(query.getRefreshPolicy()) : refresh(refreshPolicy)) //
                    .retryOnConflict(query.getRetryOnConflict()) //
            ;

            if (query.getFetchSource() != null) {
                uqb.source(sc -> sc.fetch(query.getFetchSource()));
            }

            if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
                List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
                        : Collections.emptyList();
                List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
                        : Collections.emptyList();
                uqb.source(sc -> sc.filter(sf -> sf.includes(includes).excludes(excludes)));
            }

            if (query.getTimeout() != null) {
                uqb.timeout(tv -> tv.time(query.getTimeout()));
            }

            String waitForActiveShards = query.getWaitForActiveShards();
            if (waitForActiveShards != null) {
                if ("all".equalsIgnoreCase(waitForActiveShards)) {
                    uqb.waitForActiveShards(wfa -> wfa.option(WaitForActiveShardOptions.All));
                } else {
                    int val;
                    try {
                        val = Integer.parseInt(waitForActiveShards);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("cannot parse ActiveShardCount[" + waitForActiveShards + ']', e);
                    }
                    uqb.waitForActiveShards(wfa -> wfa.count(val));
                }
            }

            return uqb;
        } //
        );
    }

    public UpdateByQueryRequest documentUpdateByQueryRequest(UpdateQuery updateQuery, IndexCoordinates index,
            @Nullable RefreshPolicy refreshPolicy) {

        return UpdateByQueryRequest.of(ub -> {
            ub //
                    .index(Arrays.asList(index.getIndexNames())) //
                    .refresh(refreshPolicy == RefreshPolicy.IMMEDIATE ? Refresh.True : Refresh.False) //
                    .routing(updateQuery.getRouting()) //
                    .script(getScript(updateQuery.getScriptData())) //
                    .maxDocs(updateQuery.getMaxDocs()) //
                    .pipeline(updateQuery.getPipeline()) //
                    .requestsPerSecond(updateQuery.getRequestsPerSecond());

            if (updateQuery.getSlices() != null) {
                 ub.slices(fn -> fn.count(updateQuery.getSlices()));
            }

            if (updateQuery.getAbortOnVersionConflict() != null) {
                ub.conflicts(updateQuery.getAbortOnVersionConflict() ? Conflicts.Abort : Conflicts.Proceed);
            }

            if (updateQuery.getQuery() != null) {
                Query queryQuery = updateQuery.getQuery();

                if (updateQuery.getBatchSize() != null) {
                    ((BaseQuery) queryQuery).setMaxResults(updateQuery.getBatchSize());
                }
                ub.query(getQuery(queryQuery, null));

                // no indicesOptions available like in old client

                ub.scroll(time(queryQuery.getScrollTime()));

            }

            // no maxRetries available like in old client
            // no shouldStoreResult

            if (updateQuery.getRefreshPolicy() != null) {
                ub.refresh(updateQuery.getRefreshPolicy() == RefreshPolicy.IMMEDIATE ? Refresh.True : Refresh.False);
            }

            if (updateQuery.getTimeout() != null) {
                ub.timeout(tb -> tb.time(updateQuery.getTimeout()));
            }

            if (updateQuery.getWaitForActiveShards() != null) {
                ub.waitForActiveShards(w -> w.count(waitForActiveShardsCount(updateQuery.getWaitForActiveShards())));
            }

            return ub;
        });
    }

    // endregion

    // region search

    public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
            IndexCoordinates indexCoordinates, boolean forCount) {
        return searchRequest(query, routing, clazz, indexCoordinates, forCount, false, null);
    }

    public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
            IndexCoordinates indexCoordinates, boolean forCount, long scrollTimeInMillis) {
        return searchRequest(query, routing, clazz, indexCoordinates, forCount, true, scrollTimeInMillis);
    }

    public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
            IndexCoordinates indexCoordinates, boolean forCount, boolean forBatchedSearch) {
        return searchRequest(query, routing, clazz, indexCoordinates, forCount, forBatchedSearch, null);
    }

    public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
            IndexCoordinates indexCoordinates, boolean forCount, boolean forBatchedSearch,
            @Nullable Long scrollTimeInMillis) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

        elasticsearchConverter.updateQuery(query, clazz);
        SearchRequest.Builder builder = new SearchRequest.Builder();
        prepareSearchRequest(query, routing, clazz, indexCoordinates, builder, forCount, forBatchedSearch);

        if (scrollTimeInMillis != null) {
            builder.scroll(t -> t.time(scrollTimeInMillis + "ms"));
        }

        builder.query(getQuery(query, clazz));

        if (StringUtils.hasText(query.getRoute())) {
            builder.routing(query.getRoute());
        }
        if (StringUtils.hasText(routing)) {
            builder.routing(routing);
        }

        addFilter(query, builder);

        return builder.build();
    }

    public MsearchTemplateRequest searchMsearchTemplateRequest(
            List<OpenSearchTemplate.MultiSearchTemplateQueryParameter> multiSearchTemplateQueryParameters,
            @Nullable String routing) {

        // basically the same stuff as in template search
        return MsearchTemplateRequest.of(mtrb -> {
            multiSearchTemplateQueryParameters.forEach(param -> {
                var query = param.query();
                mtrb.searchTemplates(stb -> stb
                        .header(msearchHeaderBuilder(query, param.index(), routing))
                        .body(bb -> {
                            bb //
                                    .explain(query.getExplain()) //
                                    .id(query.getId()) //
                                    .source(query.getSource()) //
                            ;

                            if (!CollectionUtils.isEmpty(query.getParams())) {
                                Map<String, JsonData> params = getTemplateParams(query.getParams().entrySet());
                                bb.params(params);
                            }

                            return bb;
                        })
                );
            });
            return mtrb;
        });
    }

    public MsearchRequest searchMsearchRequest(
            List<OpenSearchTemplate.MultiSearchQueryParameter> multiSearchQueryParameters, @Nullable String routing) {

        // basically the same stuff as in prepareSearchRequest, but the new Elasticsearch has different builders for a
        // normal search and msearch
        return MsearchRequest.of(mrb -> {
            multiSearchQueryParameters.forEach(param -> {
                ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(param.clazz());

                var query = param.query();
                mrb.searches(sb -> sb //
                        .header(msearchHeaderBuilder(query, param.index(), routing)) //
                        .body(bb -> {
                            bb //
                                    .query(getQuery(query, param.clazz()))//
                                    .seqNoPrimaryTerm(persistentEntity != null ? persistentEntity.hasSeqNoPrimaryTermProperty() : null) //
                                    .version(true) //
                                    .trackScores(query.getTrackScores()) //
                                    .source(getSourceConfig(query)) //
                                    .timeout(timeStringMs(query.getTimeout())) //
                            ;

                            bb.from((int) (query.getPageable().isPaged() ? query.getPageable().getOffset() : 0))
                                .size(query.getRequestSize());

                            if (!isEmpty(query.getFields())) {
                                bb.fields(fb -> {
                                    query.getFields().forEach(fb::field);
                                    return fb;
                                });
                            }

                            if (!isEmpty(query.getStoredFields())) {
                                bb.storedFields(query.getStoredFields());
                            }

                            if (query.isLimiting()) {
                                bb.size(query.getMaxResults());
                            }

                            if (query.getMinScore() > 0) {
                                bb.minScore((double) query.getMinScore());
                            }

                            if (query.getSort() != null) {
                                List<SortOptions> sortOptions = getSortOptions(query.getSort(), persistentEntity);

                                if (!sortOptions.isEmpty()) {
                                    bb.sort(sortOptions);
                                }
                            }

                            addHighlight(query, bb);

                            if (query.getExplain()) {
                                bb.explain(true);
                            }

                            if (!isEmpty(query.getSearchAfter())) {
                                bb.searchAfter(query.getSearchAfter()
                                    .stream()
                                    .map(TypeUtils::toFieldValue)
                                    .toList());
                            }

                            query.getRescorerQueries().forEach(rescorerQuery -> bb.rescore(getRescore(rescorerQuery)));

                            if (!isEmpty(query.getIndicesBoost())) {
                                bb.indicesBoost(query.getIndicesBoost().stream()
                                        .map(indexBoost -> Map.of(indexBoost.getIndexName(), (double) indexBoost.getBoost()))
                                        .collect(Collectors.toList()));
                            }

                            query.getScriptedFields().forEach(scriptedField -> bb.scriptFields(scriptedField.getFieldName(),
                                    sf -> sf.script(getScript(scriptedField.getScriptData()))));

                            if (query instanceof NativeQuery nativeQuery) {
                                prepareNativeSearch(nativeQuery, bb);
                            }
                            return bb;
                        } //
                ) //
                );

            });

            return mrb;
        });
    }

    /**
     * {@link MsearchRequest} and {@link MsearchTemplateRequest} share the same {@link MultisearchHeader}
     */
    private Function<MultisearchHeader.Builder, ObjectBuilder<MultisearchHeader>> msearchHeaderBuilder(Query query,
            IndexCoordinates index, @Nullable String routing) {
        return h -> {
            var searchType = (query instanceof NativeQuery nativeQuery && nativeQuery.getKnnQuery() != null) ? null
                    : searchType(query.getSearchType());

            h //
                    .index(Arrays.asList(index.getIndexNames())) //
                    .searchType(searchType) //
                    .requestCache(query.getRequestCache()) //
            ;

            if (StringUtils.hasText(query.getRoute())) {
                h.routing(query.getRoute());
            } else if (StringUtils.hasText(routing)) {
                h.routing(routing);
            }

            if (query.getPreference() != null) {
                h.preference(query.getPreference());
            }

            return h;
        };
    }

    private <T> void prepareSearchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
            IndexCoordinates indexCoordinates, SearchRequest.Builder builder, boolean forCount, boolean forBatchedSearch) {

        String[] indexNames = indexCoordinates.getIndexNames();

        Assert.notEmpty(indexNames, "indexCoordinates does not contain entries");

        ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(clazz);

        var searchType = (query instanceof NativeQuery nativeQuery && nativeQuery.getKnnQuery() != null) ? null
                : searchType(query.getSearchType());

        builder //
                .version(true) //
                .trackScores(query.getTrackScores()) //
                .allowNoIndices(query.getAllowNoIndices()) //
                .source(getSourceConfig(query)) //
                .searchType(searchType) //
                .timeout(timeStringMs(query.getTimeout())) //
                .requestCache(query.getRequestCache()) //
        ;

        var pointInTime = query.getPointInTime();
        if (pointInTime != null) {
            builder.pit(new Pit.Builder().id(pointInTime.id()).keepAlive(time(pointInTime.keepAlive()).time()).build());
        } else {
            builder //
                    .index(Arrays.asList(indexNames)) //
            ;

            var expandWildcards = query.getExpandWildcards();
            if (expandWildcards != null && !expandWildcards.isEmpty()) {
                builder.expandWildcards(expandWildcards(expandWildcards));
            }

            if (query.getRoute() != null) {
                builder.routing(query.getRoute());
            } else if (StringUtils.hasText(routing)) {
                builder.routing(routing);
            }

            if (query.getPreference() != null) {
                builder.preference(query.getPreference());
            }
        }

        if (persistentEntity != null && persistentEntity.hasSeqNoPrimaryTermProperty()) {
            builder.seqNoPrimaryTerm(true);
        }

        builder
            .from((int) (query.getPageable().isPaged() ? query.getPageable().getOffset() : 0))
            .size(query.getRequestSize());

        if (!isEmpty(query.getFields())) {
            var fieldAndFormats = query.getFields().stream().map(field -> FieldAndFormat.of(b -> b.field(field))).toList();
            builder.fields(fieldAndFormats);
        }

        if (!isEmpty(query.getStoredFields())) {
            builder.storedFields(query.getStoredFields());
        }

        if (query.getIndicesOptions() != null) {
            addIndicesOptions(builder, query.getIndicesOptions());
        }

        if (query.isLimiting()) {
            builder.size(query.getMaxResults());
        }

        if (query.getMinScore() > 0) {
            builder.minScore((double) query.getMinScore());
        }

        addHighlight(query, builder);

        query.getScriptedFields().forEach(scriptedField -> builder.scriptFields(scriptedField.getFieldName(),
                sf -> sf.script(getScript(scriptedField.getScriptData()))));

        if (query instanceof NativeQuery nativeQuery) {
            prepareNativeSearch(nativeQuery, builder);
        }
        // query.getSort() must be checked after prepareNativeSearch as this already might hav a sort set that must have
        // higher priority
        if (query.getSort() != null) {
            List<SortOptions> sortOptions = getSortOptions(query.getSort(), persistentEntity);

            if (!sortOptions.isEmpty()) {
                // ReactiveElasticsearchTemplate adds "_shard_doc" field to sort
                // options which OpenSearch does not support.
                builder.sort(sortOptions.stream()
                    .map(o -> {
                        if (o.isField() && o.field().field().equals("_shard_doc")) {
                            return new SortOptions(FieldSort.of(fn -> fn
                                .field("_doc")
                                .order(o.field().order())));
                        } else {
                            return o;
                        }
                    })
                    .toList());
            }
        }

        if (query.getTrackTotalHits() != null) {
            // logic from the RHLC, choose between -1 and Integer.MAX_VALUE
            int value = query.getTrackTotalHits() ? Integer.MAX_VALUE : -1;
            builder.trackTotalHits(th -> th.count(value));
        } else if (query.getTrackTotalHitsUpTo() != null) {
            builder.trackTotalHits(th -> th.count(query.getTrackTotalHitsUpTo()));
        }

        if (query.getExplain()) {
            builder.explain(true);
        }

        if (!isEmpty(query.getSearchAfter())) {
            builder.searchAfter(query.getSearchAfter()
                .stream()
                .map(TypeUtils::toFieldValue)
                .toList());
        }

        query.getRescorerQueries().forEach(rescorerQuery -> builder.rescore(getRescore(rescorerQuery)));

        if (forCount) {
            builder.size(0) //
                    .trackTotalHits(th -> th.count(Integer.MAX_VALUE)) //
                    .source(SourceConfig.of(sc -> sc.fetch(false)));
        } else if (forBatchedSearch) {
            // request_cache is not allowed on scroll requests.
            builder.requestCache(null);
            // limit the number of documents in a batch if not already set in a pageable
            if (query.getPageable().isUnpaged()) {
                builder.size(query.getReactiveBatchSize());
            }
        }

        if (!isEmpty(query.getIndicesBoost())) {
            builder.indicesBoost(query.getIndicesBoost().stream()
                    .map(indexBoost -> Map.of(indexBoost.getIndexName(), (double) indexBoost.getBoost()))
                    .collect(Collectors.toList()));
        }

        if (!isEmpty(query.getDocValueFields())) {
            builder.docvalueFields(query.getDocValueFields().stream() //
                    .map(docValueField -> FieldAndFormat.of(b -> b.field(docValueField.field()).format(docValueField.format())))
                    .toList());
        }
    }

    private void addIndicesOptions(SearchRequest.Builder builder, IndicesOptions indicesOptions) {

        indicesOptions.getOptions().forEach(option -> {
            switch (option) {
                case ALLOW_NO_INDICES -> builder.allowNoIndices(true);
                case IGNORE_UNAVAILABLE -> builder.ignoreUnavailable(true);
                case IGNORE_THROTTLED -> builder.ignoreThrottled(true);
                // the following ones aren't supported by the builder
                case FORBID_ALIASES_TO_MULTIPLE_INDICES, FORBID_CLOSED_INDICES, IGNORE_ALIASES -> {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER
                                .warn(String.format("indices option %s is not supported by the Elasticsearch client.", option.name()));
                    }
                }
            }
        });

        builder.expandWildcards(indicesOptions.getExpandWildcards().stream()
                .map(wildcardStates -> switch (wildcardStates) {
                    case OPEN -> ExpandWildcard.Open;
                    case CLOSED -> ExpandWildcard.Closed;
                    case HIDDEN -> ExpandWildcard.Hidden;
                    case ALL -> ExpandWildcard.All;
                    case NONE -> ExpandWildcard.None;
                }).collect(Collectors.toList()));
    }

    private Rescore getRescore(RescorerQuery rescorerQuery) {

        return Rescore.of(r -> r //
                .query(rq -> rq //
                        .rescoreQuery(getQuery(rescorerQuery.getQuery(), null)) //
                        .scoreMode(scoreMode(rescorerQuery.getScoreMode())) //
                        .queryWeight(rescorerQuery.getQueryWeight() != null ? rescorerQuery.getQueryWeight() : 1.0f) //
                        .rescoreQueryWeight(
                                rescorerQuery.getRescoreQueryWeight() != null ? rescorerQuery.getRescoreQueryWeight()
                                        : 1.0f) //

                ) //
                .windowSize(rescorerQuery.getWindowSize()));
    }

    private void addHighlight(Query query, SearchRequest.Builder builder) {

        Highlight highlight = query.getHighlightQuery()
                .map(highlightQuery -> new HighlightQueryBuilder(elasticsearchConverter.getMappingContext(), this)
                        .getHighlight(highlightQuery.getHighlight(), highlightQuery.getType()))
                .orElse(null);

        builder.highlight(highlight);
    }

    private void addHighlight(Query query, MultisearchBody.Builder builder) {

        Highlight highlight = query.getHighlightQuery()
                .map(highlightQuery -> new HighlightQueryBuilder(elasticsearchConverter.getMappingContext(), this)
                        .getHighlight(highlightQuery.getHighlight(), highlightQuery.getType()))
                .orElse(null);

        builder.highlight(highlight);
    }

    private List<SortOptions> getSortOptions(Sort sort, @Nullable ElasticsearchPersistentEntity<?> persistentEntity) {
        return sort.stream().map(order -> getSortOptions(order, persistentEntity)).collect(Collectors.toList());
    }

    private SortOptions getSortOptions(Sort.Order order, @Nullable ElasticsearchPersistentEntity<?> persistentEntity) {
        SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.Desc : SortOrder.Asc;

        Order.Mode mode = order.getDirection().isAscending() ? Order.Mode.min : Order.Mode.max;
        String unmappedType = null;
        String missing = null;
        NestedSortValue nestedSortValue = null;

        if (SortOptions.Kind.Score.jsonValue().equals(order.getProperty())) {
            return SortOptions.of(so -> so.score(s -> s.order(sortOrder)));
        }

        if (order instanceof Order o) {

            if (o.getMode() != null) {
                mode = o.getMode();
            }
            unmappedType = o.getUnmappedType();
            missing = o.getMissing();
            nestedSortValue = getNestedSort(o.getNested(), persistentEntity);
        }
        Order.Mode finalMode = mode;
        String finalUnmappedType = unmappedType;
        var finalNestedSortValue = nestedSortValue;

        ElasticsearchPersistentProperty property = (persistentEntity != null) //
                ? persistentEntity.getPersistentProperty(order.getProperty()) //
                : null;
        String fieldName = property != null ? property.getFieldName() : order.getProperty();

        if (order instanceof GeoDistanceOrder geoDistanceOrder) {
            return getSortOptions(geoDistanceOrder, fieldName, finalMode);
        }

        var finalMissing = missing != null ? missing
                : (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) ? "_first"
                        : ((order.getNullHandling() == Sort.NullHandling.NULLS_LAST) ? "_last" : null);

        return SortOptions.of(so -> so //
                .field(f -> {
                    f.field(fieldName) //
                            .order(sortOrder) //
                            .mode(sortMode(finalMode));

                    if (finalUnmappedType != null) {
                        FieldType fieldType = fieldType(finalUnmappedType);

                        if (fieldType != null) {
                            f.unmappedType(fieldType);
                        }
                    }

                    if (finalMissing != null) {
                        f.missing(fv -> fv //
                                .stringValue(finalMissing));
                    }

                    if (finalNestedSortValue != null) {
                        f.nested(finalNestedSortValue);
                    }

                    return f;
                }));
    }

    @Nullable
    private NestedSortValue getNestedSort(@Nullable Order.Nested nested,
            @Nullable ElasticsearchPersistentEntity<?> persistentEntity) {
        return (nested == null || persistentEntity == null) ? null
                : NestedSortValue.of(b -> b //
                        .path(elasticsearchConverter.updateFieldNames(nested.getPath(), persistentEntity)) //
                        .maxChildren(nested.getMaxChildren()) //
                        .nested(getNestedSort(nested.getNested(), persistentEntity)) //
                        .filter(getQuery(nested.getFilter(), persistentEntity.getType())));
    }

    private static SortOptions getSortOptions(GeoDistanceOrder geoDistanceOrder, String fieldName, Order.Mode finalMode) {
        return SortOptions.of(so -> so //
                .geoDistance(gd -> gd //
                        .field(fieldName) //
                        .location(loc -> loc.latlon(Queries.latLon(geoDistanceOrder.getGeoPoint()))) //
                        .distanceType(geoDistanceType(geoDistanceOrder.getDistanceType())).mode(sortMode(finalMode)) //
                        .order(sortOrder(geoDistanceOrder.getDirection())) //
                        .unit(distanceUnit(geoDistanceOrder.getUnit())) //
                        .ignoreUnmapped(geoDistanceOrder.getIgnoreUnmapped())));
    }

    @SuppressWarnings("DuplicatedCode")
    private void prepareNativeSearch(NativeQuery query, SearchRequest.Builder builder) {

        builder //
                .suggest(query.getSuggester()) //
                .collapse(query.getFieldCollapse()) //
                .sort(query.getSortOptions()) //
        ;

        if (query.getKnnQuery() != null) {
            builder.query(query.getKnnQuery().toQuery());
        }

        if (!isEmpty(query.getAggregations())) {
            builder.aggregations(query.getAggregations());
        }

        if (!isEmpty(query.getSearchExtensions())) {
            builder.ext(query.getSearchExtensions());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    private void prepareNativeSearch(NativeQuery query, MultisearchBody.Builder builder) {

        builder //
                .suggest(query.getSuggester()) //
                .collapse(query.getFieldCollapse()) //
                .sort(query.getSortOptions());

        if (query.getKnnQuery() != null) {
            builder.query(query.getKnnQuery().toQuery());
        }

        if (!isEmpty(query.getAggregations())) {
            builder.aggregations(query.getAggregations());
        }

        if (!isEmpty(query.getSearchExtensions())) {
            builder.ext(query.getSearchExtensions());
        }
    }

    @Nullable
    org.opensearch.client.opensearch._types.query_dsl.Query getQuery(@Nullable Query query,
            @Nullable Class<?> clazz) {

        if (query == null) {
            return null;
        }

        elasticsearchConverter.updateQuery(query, clazz);

        org.opensearch.client.opensearch._types.query_dsl.Query esQuery = null;

        if (query instanceof CriteriaQuery) {
            esQuery = CriteriaQueryProcessor.createQuery(((CriteriaQuery) query).getCriteria());
        } else if (query instanceof StringQuery) {
            esQuery = Queries.wrapperQueryAsQuery(((StringQuery) query).getSource());
        } else if (query instanceof NativeQuery nativeQuery) {

            if (nativeQuery.getQuery() != null) {
                esQuery = nativeQuery.getQuery();
            } else if (nativeQuery.getSpringDataQuery() != null) {
                esQuery = getQuery(nativeQuery.getSpringDataQuery(), clazz);
            }
        } else {
            throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
        }

        return esQuery;
    }

    private void addFilter(Query query, SearchRequest.Builder builder) {

        if (query instanceof CriteriaQuery) {
            CriteriaFilterProcessor.createQuery(((CriteriaQuery) query).getCriteria()).ifPresent(builder::postFilter);
        } else // noinspection StatementWithEmptyBody
        if (query instanceof StringQuery) {
            // no filter for StringQuery
        } else if (query instanceof NativeQuery nativeQuery) {
            if (nativeQuery.getFilter() != null) {
                builder.postFilter(nativeQuery.getFilter());
            } else if (nativeQuery.getSpringDataQuery() != null) {
                addFilter(nativeQuery.getSpringDataQuery(), builder);
            }
        } else {
            throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
        }
    }

    public org.opensearch.client.opensearch._types.query_dsl.MoreLikeThisQuery moreLikeThisQuery(MoreLikeThisQuery query,
            IndexCoordinates index) {

        Assert.notNull(query, "query must not be null");
        Assert.notNull(index, "index must not be null");

        return org.opensearch.client.opensearch._types.query_dsl.MoreLikeThisQuery
                .of(q -> {
                    q.like(Like.of(l -> l.document(ld -> ld.index(index.getIndexName()).id(query.getId()))))
                            .fields(query.getFields());

                    if (query.getMinTermFreq() != null) {
                        q.minTermFreq(query.getMinTermFreq());
                    }

                    if (query.getMaxQueryTerms() != null) {
                        q.maxQueryTerms(query.getMaxQueryTerms());
                    }

                    if (!isEmpty(query.getStopWords())) {
                        q.stopWords(query.getStopWords());
                    }

                    if (query.getMinDocFreq() != null) {
                        q.minDocFreq(query.getMinDocFreq());
                    }

                    if (query.getMaxDocFreq() != null) {
                        q.maxDocFreq(query.getMaxDocFreq());
                    }

                    if (query.getMinWordLen() != null) {
                        q.minWordLength(query.getMinWordLen());
                    }

                    if (query.getMaxWordLen() != null) {
                        q.maxWordLength(query.getMaxWordLen());
                    }

                    if (query.getBoostTerms() != null) {
                        q.boostTerms(Float.valueOf(query.getBoostTerms()));
                    }

                    return q;
                });
    }

    public CreatePitRequest searchOpenPointInTimeRequest(IndexCoordinates index, Duration keepAlive,
            Boolean allowPartialPitCreation) {

        Assert.notNull(index, "index must not be null");
        Assert.notNull(keepAlive, "keepAlive must not be null");
        Assert.notNull(allowPartialPitCreation, "allowPartialPitCreation must not be null");

        return CreatePitRequest.of(opit -> opit //
                .index(Arrays.asList(index.getIndexNames())) //
                .allowPartialPitCreation(allowPartialPitCreation) //
                .keepAlive(time(keepAlive)) //
        );
    }

    public DeletePitRequest searchClosePointInTime(String pit) {

        Assert.notNull(pit, "pit must not be null");

        return DeletePitRequest.of(cpit -> cpit.pitId(Collections.singletonList(pit)));
    }

    public SearchTemplateRequest searchTemplate(SearchTemplateQuery query, @Nullable String routing,
            IndexCoordinates index) {

        Assert.notNull(query, "query must not be null");

        return SearchTemplateRequest.of(builder -> {
            builder //
                    .allowNoIndices(query.getAllowNoIndices()) //
                    .explain(query.getExplain()) //
                    .id(query.getId()) //
                    .index(Arrays.asList(index.getIndexNames())) //
                    .preference(query.getPreference()) //
                    .searchType(searchType(query.getSearchType())) //
                    .source(query.getSource()) //
            ;

            if (query.getRoute() != null) {
                builder.routing(query.getRoute());
            } else if (StringUtils.hasText(routing)) {
                builder.routing(routing);
            }

            var expandWildcards = query.getExpandWildcards();
            if (expandWildcards != null && !expandWildcards.isEmpty()) {
                builder.expandWildcards(expandWildcards(expandWildcards));
            }

            if (query.hasScrollTime()) {
                builder.scroll(time(query.getScrollTime()));
            }

            if (!CollectionUtils.isEmpty(query.getParams())) {
                Map<String, JsonData> params = getTemplateParams(query.getParams().entrySet());
                builder.params(params);
            }

            return builder;
        });
    }

    @NonNull
    private Map<String, JsonData> getTemplateParams(Set<Map.Entry<String, Object>> query) {
        Function<Map.Entry<String, Object>, String> keyMapper = Map.Entry::getKey;
        Function<Map.Entry<String, Object>, JsonData> valueMapper = entry -> JsonData.of(entry.getValue(), jsonpMapper);
        return query.stream()
                .collect(Collectors.toMap(keyMapper, valueMapper));
    }

    // endregion

    public PutScriptRequest scriptPut(Script script) {

        Assert.notNull(script, "script must not be null");

        return PutScriptRequest.of(b -> b //
                .id(script.id()) //
                .script(sb -> sb //
                        .lang(ScriptLanguage.of(fn -> fn.custom(script.language()))) //
                        .source(script.source())));
    }

    public GetScriptRequest scriptGet(String name) {

        Assert.notNull(name, "name must not be null");

        return GetScriptRequest.of(b -> b.id(name));
    }

    public DeleteScriptRequest scriptDelete(String name) {

        Assert.notNull(name, "name must not be null");

        return DeleteScriptRequest.of(b -> b.id(name));
    }
    // region helper functions

    public <T> T fromJson(String json, JsonpDeserializer<T> deserializer) {

        Assert.notNull(json, "json must not be null");
        Assert.notNull(deserializer, "deserializer must not be null");

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        return fromJson(byteArrayInputStream, deserializer);
    }

    public <T> T fromJson(ByteArrayInputStream byteArrayInputStream, JsonpDeserializer<T> deserializer) {

        Assert.notNull(byteArrayInputStream, "byteArrayInputStream must not be null");
        Assert.notNull(deserializer, "deserializer must not be null");

        JsonParser parser = jsonpMapper.jsonProvider().createParser(byteArrayInputStream);
        return deserializer.deserialize(parser, jsonpMapper);
    }

    @Nullable
    private ElasticsearchPersistentEntity<?> getPersistentEntity(Object entity) {
        return elasticsearchConverter.getMappingContext().getPersistentEntity(entity.getClass());
    }

    @Nullable
    private ElasticsearchPersistentEntity<?> getPersistentEntity(@Nullable Class<?> clazz) {
        return clazz != null ? elasticsearchConverter.getMappingContext().getPersistentEntity(clazz) : null;
    }

    @Nullable
    private String getPersistentEntityId(Object entity) {

        ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(entity);

        if (persistentEntity != null) {
            Object identifier = persistentEntity //
                    .getIdentifierAccessor(entity).getIdentifier();

            if (identifier != null) {
                return identifier.toString();
            }
        }

        return null;
    }

    private VersionType retrieveVersionTypeFromPersistentEntity(@Nullable Class<?> clazz) {

        ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(clazz);

        VersionType versionType = null;

        if (persistentEntity != null) {
            org.springframework.data.elasticsearch.annotations.Document.VersionType entityVersionType = persistentEntity
                    .getVersionType();

            if (entityVersionType != null) {
                versionType = switch (entityVersionType) {
                    case INTERNAL -> VersionType.Internal;
                    case EXTERNAL -> VersionType.External;
                    case EXTERNAL_GTE -> VersionType.ExternalGte;
                    case FORCE -> VersionType.Force;
                };
            }
        }

        return versionType != null ? versionType : VersionType.External;
    }

    @Nullable
    private SourceConfig getSourceConfig(Query query) {

        if (query.getSourceFilter() != null) {
            return SourceConfig.of(s -> s //
                    .filter(sfb -> {
                        SourceFilter sourceFilter = query.getSourceFilter();
                        String[] includes = sourceFilter.getIncludes();
                        String[] excludes = sourceFilter.getExcludes();

                        if (includes != null) {
                            sfb.includes(Arrays.asList(includes));
                        }

                        if (excludes != null) {
                            sfb.excludes(Arrays.asList(excludes));
                        }

                        return sfb;
                    }));
        } else {
            return null;
        }
    }

    @Nullable
    static Refresh deleteByQueryRefresh(@Nullable RefreshPolicy refreshPolicy) {

        if (refreshPolicy == null) {
            return null;
        }

        return switch (refreshPolicy) {
            case IMMEDIATE -> Refresh.True;
            case WAIT_UNTIL -> Refresh.WaitFor;
            case NONE -> Refresh.False;
        };
    }

    // endregion
}
