/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.data.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;
import static org.springframework.data.elasticsearch.utils.IdGenerator.nextIdAsString;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.opensearch.data.client.EnabledIfOpenSearchVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.ScriptedField;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.BaseQueryBuilder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.junit.jupiter.SpringIntegrationTest;
import org.springframework.data.elasticsearch.utils.IndexNameProvider;
import org.springframework.lang.Nullable;

/**
 * All the integration tests that are not in separate files.
 */
@SpringIntegrationTest
public abstract class OpenSearchSpecificIntegrationTests {
    private static final String MULTI_INDEX_PREFIX = "os-test-index";
    private static final String MULTI_INDEX_ALL = MULTI_INDEX_PREFIX + "*";

    @Autowired protected OpenSearchOperations operations;
    private IndexOperations indexOperations;

    @Autowired protected IndexNameProvider indexNameProvider;

    @BeforeEach
    public void before() {

        indexNameProvider.increment();
        indexOperations = operations.indexOps(SampleEntity.class);
        indexOperations.createWithMapping();
    }

    @Test
    @Order(java.lang.Integer.MAX_VALUE)
    void cleanup() {
        operations.indexOps(IndexCoordinates.of(indexNameProvider.getPrefix() + "*")).delete();
        operations.indexOps(IndexCoordinates.of(MULTI_INDEX_ALL)).delete();
    }

    protected abstract BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery();

    @Test
    @EnabledIfOpenSearchVersion(
            onOrAfter = "2.3.0",
            reason = "https://github.com/opensearch-project/OpenSearch/issues/1147")
    public void testPointInTimeKeepAliveExpired() throws InterruptedException {
        // given
        // first document
        String documentId = nextIdAsString();
        SampleEntity sampleEntity1 = SampleEntity.builder().id(documentId).message("abc").rate(10)
                .version(System.currentTimeMillis()).build();

        // second document
        String documentId2 = nextIdAsString();
        SampleEntity sampleEntity2 = SampleEntity.builder().id(documentId2).message("xyz").rate(5)
                .version(System.currentTimeMillis()).build();

        // third document
        String documentId3 = nextIdAsString();
        SampleEntity sampleEntity3 = SampleEntity.builder().id(documentId3).message("xyzg").rate(10)
                .version(System.currentTimeMillis()).build();

        List<IndexQuery> indexQueries = getIndexQueries(Arrays.asList(sampleEntity1, sampleEntity2, sampleEntity3));

        operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));
        String pit = operations.openPointInTime(IndexCoordinates.of(indexNameProvider.indexName()),
                Duration.ofMillis(10));
        Assertions.assertNotNull(pit);
        Query.PointInTime qpit = new Query.PointInTime(pit,Duration.ofMillis(10));
        Query query = getBuilderWithMatchAllQuery() //
                .withSort(Sort.by(Sort.Order.desc("message"))) //
                .withPageable(Pageable.ofSize(2))
                .withPointInTime(qpit).build();
        SearchHits<SampleEntity> results = operations.search(query,SampleEntity.class);
        assertThat(results.getSearchHits().size()).isEqualTo(2);

        final Query searchAfterQuery = getBuilderWithMatchAllQuery() //
                .withSort(Sort.by(Sort.Order.desc("message"))) //
                .withPointInTime(qpit)
                .withSearchAfter(List.of(Objects.requireNonNull(results.getSearchHit(1).getContent().getMessage())))
                .build();

        final long started = System.nanoTime();
        while ((System.nanoTime() - started) < TimeUnit.SECONDS.toNanos(120)) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
            if (operations.listPointInTime().isEmpty()) {
                break;
            }
        }

        assertThatExceptionOfType(ResourceNotFoundException.class)
            .isThrownBy(()-> operations.search(searchAfterQuery,SampleEntity.class));

        Boolean pitResult = operations.closePointInTime(pit);
        Assertions.assertTrue(pitResult);
    }

    private IndexQuery getIndexQuery(SampleEntity sampleEntity) {
        return new IndexQueryBuilder().withId(sampleEntity.getId()).withObject(sampleEntity)
                .withVersion(sampleEntity.getVersion()).build();
    }

    private List<IndexQuery> getIndexQueries(List<SampleEntity> sampleEntities) {
        List<IndexQuery> indexQueries = new ArrayList<>();
        for (SampleEntity sampleEntity : sampleEntities) {
            indexQueries.add(getIndexQuery(sampleEntity));
        }
        return indexQueries;
    }

    // region entities
    @Document(indexName = "#{@indexNameProvider.indexName()}")
    @Setting(shards = 1, replicas = 0, refreshInterval = "-1")
    protected static class SampleEntity {
        @Nullable
        @Id private String id;
        @Nullable
        @Field(type = Text, store = true, fielddata = true) private String type;
        @Nullable
        @Field(type = Text, store = true, fielddata = true) private String message;
        private int rate;
        @Nullable
        @ScriptedField private Double scriptedRate;
        private boolean available;
        @Nullable private GeoPoint location;
        @Nullable
        @Version private Long version;

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {

            @Nullable private String id;
            @Nullable private String type;
            @Nullable private String message;
            @Nullable private Long version;
            private int rate;
            @Nullable private GeoPoint location;

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder type(String type) {
                this.type = type;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder version(Long version) {
                this.version = version;
                return this;
            }

            public Builder rate(int rate) {
                this.rate = rate;
                return this;
            }

            public Builder location(GeoPoint location) {
                this.location = location;
                return this;
            }

            public SampleEntity build() {
                SampleEntity sampleEntity = new SampleEntity();
                sampleEntity.setId(id);
                sampleEntity.setType(type);
                sampleEntity.setMessage(message);
                sampleEntity.setRate(rate);
                sampleEntity.setVersion(version);
                sampleEntity.setLocation(location);
                return sampleEntity;
            }
        }

        public SampleEntity() {}

        @Nullable
        public String getId() {
            return id;
        }

        public void setId(@Nullable String id) {
            this.id = id;
        }

        @Nullable
        public String getType() {
            return type;
        }

        public void setType(@Nullable String type) {
            this.type = type;
        }

        @Nullable
        public String getMessage() {
            return message;
        }

        public void setMessage(@Nullable String message) {
            this.message = message;
        }

        public int getRate() {
            return rate;
        }

        public void setRate(int rate) {
            this.rate = rate;
        }

        @Nullable
        public java.lang.Double getScriptedRate() {
            return scriptedRate;
        }

        public void setScriptedRate(@Nullable java.lang.Double scriptedRate) {
            this.scriptedRate = scriptedRate;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        @Nullable
        public GeoPoint getLocation() {
            return location;
        }

        public void setLocation(@Nullable GeoPoint location) {
            this.location = location;
        }

        @Nullable
        public java.lang.Long getVersion() {
            return version;
        }

        public void setVersion(@Nullable java.lang.Long version) {
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            SampleEntity that = (SampleEntity) o;

            if (rate != that.rate)
                return false;
            if (available != that.available)
                return false;
            if (!Objects.equals(id, that.id))
                return false;
            if (!Objects.equals(type, that.type))
                return false;
            if (!Objects.equals(message, that.message))
                return false;
            if (!Objects.equals(scriptedRate, that.scriptedRate))
                return false;
            if (!Objects.equals(location, that.location))
                return false;
            return Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (message != null ? message.hashCode() : 0);
            result = 31 * result + rate;
            result = 31 * result + (scriptedRate != null ? scriptedRate.hashCode() : 0);
            result = 31 * result + (available ? 1 : 0);
            result = 31 * result + (location != null ? location.hashCode() : 0);
            result = 31 * result + (version != null ? version.hashCode() : 0);
            return result;
        }
    }
    // endregion
}
