package org.opensearch.data.client.orhlc;

import static org.opensearch.index.query.QueryBuilders.wrapperQuery;

import org.opensearch.index.query.QueryBuilder;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.lang.Nullable;

public class QueryUtil {
    @Nullable
    public static QueryBuilder getFilter(Query query) {
        QueryBuilder opensearchFilter;

        if (query instanceof NativeSearchQuery) {
            NativeSearchQuery searchQuery = (NativeSearchQuery) query;
            opensearchFilter = searchQuery.getFilter();
        } else if (query instanceof CriteriaQuery) {
            CriteriaQuery criteriaQuery = (CriteriaQuery) query;
            opensearchFilter = new CriteriaFilterProcessor().createFilter(criteriaQuery.getCriteria());
        } else if (query instanceof StringQuery) {
            opensearchFilter = null;
        } else {
            throw new IllegalArgumentException(
                    "unhandled Query implementation " + query.getClass().getName());
        }

        return opensearchFilter;
    }

    @Nullable
    public static QueryBuilder getQuery(Query query) {
        QueryBuilder opensearchQuery;

        if (query instanceof NativeSearchQuery) {
            NativeSearchQuery searchQuery = (NativeSearchQuery) query;
            opensearchQuery = searchQuery.getQuery();
        } else if (query instanceof CriteriaQuery) {
            CriteriaQuery criteriaQuery = (CriteriaQuery) query;
            opensearchQuery = new CriteriaQueryProcessor().createQuery(criteriaQuery.getCriteria());
        } else if (query instanceof StringQuery) {
            StringQuery stringQuery = (StringQuery) query;
            opensearchQuery = wrapperQuery(stringQuery.getSource());
        } else {
            throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
        }

        return opensearchQuery;
    }
}
