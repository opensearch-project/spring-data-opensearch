/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc.knn;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.lucene.search.Query;
import org.opensearch.core.ParseField;
import org.opensearch.core.common.Strings;
import org.opensearch.core.common.io.stream.StreamOutput;
import org.opensearch.core.xcontent.XContentBuilder;
import org.opensearch.index.query.AbstractQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryShardContext;
import org.opensearch.index.query.WithFieldName;

public class KnnQueryBuilder extends AbstractQueryBuilder<KnnQueryBuilder> implements WithFieldName {
    public static final String METHOD_PARAMETER_EF_SEARCH = "ef_search";
    public static final String METHOD_PARAMETER_M = "m";
    public static final String METHOD_PARAMETER = "method_parameters";
    public static final String NAME = "knn";
    public static final int K_MAX = 10000;

    public static final ParseField VECTOR_FIELD = new ParseField("vector");
    public static final ParseField K_FIELD = new ParseField("k");
    public static final ParseField FILTER_FIELD = new ParseField("filter");
    public static final ParseField IGNORE_UNMAPPED_FIELD = new ParseField("ignore_unmapped");
    public static final ParseField EF_SEARCH_FIELD = new ParseField(METHOD_PARAMETER_EF_SEARCH);

    private final String fieldName;
    private final float[] vector;
    private Integer k;
    private QueryBuilder filter;
    private boolean ignoreUnmapped;

    private KnnQueryBuilder(final String fieldName, final float[] vector, final Integer k, final QueryBuilder filter, final boolean ignoreUnmapped) {
        this.fieldName = fieldName;
        this.vector = vector;
        this.k = k;
        this.filter = filter;
        this.ignoreUnmapped = ignoreUnmapped;
    }

    public static class Builder {
        private String fieldName;
        private float[] vector;
        private Integer k;
        private QueryBuilder filter;
        private boolean ignoreUnmapped;
        private String queryName;
        private float boost = DEFAULT_BOOST;

        public Builder() {}

        public Builder field(String fieldName) {
            return fieldName(fieldName);
        }

        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        public Builder vector(List<Float> vector) {
            final float[] values = new float [vector.size()];
            for (int i = 0; i < vector.size(); ++i) {
                values[i] = vector.get(i).floatValue();
            }
            return vector(values);
        }

        public Builder vector(float[] vector) {
            this.vector = vector;
            return this;
        }

        public Builder k(Integer k) {
            this.k = k;
            return this;
        }

        public Builder ignoreUnmapped(boolean ignoreUnmapped) {
            this.ignoreUnmapped = ignoreUnmapped;
            return this;
        }

        public Builder filter(QueryBuilder filter) {
            this.filter = filter;
            return this;
        }

        public Builder queryName(String queryName) {
            this.queryName = queryName;
            return this;
        }

        public Builder boost(float boost) {
            this.boost = boost;
            return this;
        }

        public KnnQueryBuilder build() {
            validate();
            return new KnnQueryBuilder(
                fieldName,
                vector,
                k,
                filter,
                ignoreUnmapped
            ).boost(boost).queryName(queryName);
        }

        private void validate() {
            if (Strings.isNullOrEmpty(fieldName)) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "[%s] requires fieldName", NAME));
            }

            if (vector == null) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "[%s] requires query vector", NAME));
            } else if (vector.length == 0) {
                throw new IllegalArgumentException(String.format(Locale.ROOT, "[%s] query vector is empty", NAME));
            }


            if (k != null) {
                if (k <= 0 || k > K_MAX) {
                    final String errorMessage = "[" + NAME + "] requires k to be in the range (0, " + K_MAX + "]";
                    throw new IllegalArgumentException(errorMessage);
                }
            }
        }
    }

    public float[] getVector() {
        return vector;
    }

    public Integer getK() {
        return k;
    }

    public QueryBuilder getFilter() {
        return filter;
    }

    public boolean isIgnoreUnmapped() {
        return ignoreUnmapped;
    }

    @Override
    protected void doXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject(NAME);
        builder.startObject(this.fieldName());

        builder.field(VECTOR_FIELD.getPreferredName(), this.getVector());

        if (this.getK() != null) {
            builder.field(K_FIELD.getPreferredName(), this.getK());
        }
        if (this.getFilter() != null) {
            builder.field(FILTER_FIELD.getPreferredName(), this.getFilter());
        }

        if (this.isIgnoreUnmapped()) {
            builder.field(IGNORE_UNMAPPED_FIELD.getPreferredName(), this.isIgnoreUnmapped());
        }

        builder.field(BOOST_FIELD.getPreferredName(), this.boost());
        if (this.queryName() != null) {
            builder.field(NAME_FIELD.getPreferredName(), this.queryName());
        }

        builder.endObject();
        builder.endObject();
    }

    public static KnnQueryBuilder.Builder builder() {
        return new KnnQueryBuilder.Builder();
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    @Override
    public String fieldName() {
        return fieldName;
    }

    @Override
    protected void doWriteTo(StreamOutput out) throws IOException {
        throw new UnsupportedOperationException("The doWriteTo is not supported");
    }

    @Override
    protected Query doToQuery(QueryShardContext context) throws IOException {
        throw new UnsupportedOperationException("The doToQuery is not supported");
    }

    @Override
    protected boolean doEquals(KnnQueryBuilder other) {
        return Objects.equals(fieldName, other.fieldName)
            && Arrays.equals(vector, other.vector)
            && Objects.equals(k, other.k)
            && Objects.equals(filter, other.filter)
            && Objects.equals(ignoreUnmapped, other.ignoreUnmapped);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(
            fieldName,
            Arrays.hashCode(vector),
            k,
            filter,
            ignoreUnmapped
        );
    }

}
