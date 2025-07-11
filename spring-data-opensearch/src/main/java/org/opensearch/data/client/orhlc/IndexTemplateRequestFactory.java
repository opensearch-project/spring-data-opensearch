package org.opensearch.data.client.orhlc;

import static org.opensearch.data.client.orhlc.QueryUtil.*;

import java.util.Arrays;

import org.opensearch.action.admin.indices.alias.Alias;
import org.opensearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.opensearch.client.indices.GetIndexTemplatesRequest;
import org.opensearch.client.indices.IndexTemplatesExistRequest;
import org.opensearch.client.indices.PutIndexTemplateRequest;
import org.opensearch.index.query.QueryBuilder;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.DeleteTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.ExistsTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetIndexTemplateRequest;
import org.springframework.data.elasticsearch.core.index.GetTemplateRequest;
import org.springframework.data.elasticsearch.core.query.Query;

public class IndexTemplateRequestFactory {
    private final ElasticsearchConverter elasticsearchConverter;

    public IndexTemplateRequestFactory(ElasticsearchConverter elasticsearchConverter) {
        this.elasticsearchConverter = elasticsearchConverter;
    }

    public GetIndexTemplatesRequest getIndexTemplatesRequest(GetIndexTemplateRequest getTemplateRequest) {
        return new GetIndexTemplatesRequest(getTemplateRequest.templateName());
    }

    public IndexTemplatesExistRequest indexTemplatesExistsRequest(ExistsIndexTemplateRequest existsTemplateRequest) {
        return new IndexTemplatesExistRequest(existsTemplateRequest.templateName());
    }

    public DeleteIndexTemplateRequest deleteIndexTemplateRequest(org.springframework.data.elasticsearch.core.index.DeleteIndexTemplateRequest deleteTemplateRequest) {
        return new DeleteIndexTemplateRequest(deleteTemplateRequest.templateName());
    }

    public GetIndexTemplatesRequest getIndexTemplatesRequest(GetTemplateRequest getTemplateRequest) {
        return new GetIndexTemplatesRequest(getTemplateRequest.getTemplateName());
    }

    public IndexTemplatesExistRequest indexTemplatesExistsRequest(ExistsTemplateRequest existsTemplateRequest) {
        return new IndexTemplatesExistRequest(existsTemplateRequest.getTemplateName());
    }

    public DeleteIndexTemplateRequest deleteIndexTemplateRequest(DeleteTemplateRequest deleteTemplateRequest) {
        return new DeleteIndexTemplateRequest(deleteTemplateRequest.getTemplateName());
    }

    public PutIndexTemplateRequest putIndexTemplateRequest(org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest putIndexTemplateRequest) {
        PutIndexTemplateRequest request = new PutIndexTemplateRequest(putIndexTemplateRequest.name())
                .patterns(Arrays.asList(putIndexTemplateRequest.indexPatterns()));

        if (putIndexTemplateRequest.settings() != null) {
            request.settings(putIndexTemplateRequest.settings());
        }

        if (putIndexTemplateRequest.mapping() != null) {
            request.mapping(putIndexTemplateRequest.mapping());
        }

        AliasActions aliasActions = putIndexTemplateRequest.aliasActions();

        if (aliasActions == null) {
            return request;
        }

        aliasActions.getActions().forEach(aliasAction -> {
            AliasActionParameters parameters = aliasAction.getParameters();
            String[] parametersAliases = parameters.getAliases();

            if (parametersAliases == null) {
                return;
            }

            for (String aliasName : parametersAliases) {
                Alias alias = createAndConfigureAlias(aliasName, parameters);
                request.alias(alias);
            }
        });

        return request;
    }

    private Alias createAndConfigureAlias(String aliasName, AliasActionParameters parameters) {
        Alias alias = new Alias(aliasName);

        // noinspection DuplicatedCode
        if (parameters.getRouting() != null) {
            alias.routing(parameters.getRouting());
        }

        if (parameters.getIndexRouting() != null) {
            alias.indexRouting(parameters.getIndexRouting());
        }

        if (parameters.getSearchRouting() != null) {
            alias.searchRouting(parameters.getSearchRouting());
        }

        if (parameters.getHidden() != null) {
            alias.isHidden(parameters.getHidden());
        }

        if (parameters.getWriteIndex() != null) {
            alias.writeIndex(parameters.getWriteIndex());
        }

        // noinspection DuplicatedCode
        Query filterQuery = parameters.getFilterQuery();

        if (filterQuery != null) {
            elasticsearchConverter.updateQuery(filterQuery, parameters.getFilterQueryClass());
            QueryBuilder queryBuilder = getFilter(filterQuery);

            if (queryBuilder == null) {
                queryBuilder = getQuery(filterQuery);
            }

            alias.filter(queryBuilder);
        }

        return alias;
    }
}
