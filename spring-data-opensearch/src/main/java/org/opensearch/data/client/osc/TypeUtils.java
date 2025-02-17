/*
 * Copyright 2022-2024 the original author or authors.
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

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opensearch.client.opensearch._types.*;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch.core.search.BoundaryScanner;
import org.opensearch.client.opensearch.core.search.BuiltinHighlighterType;
import org.opensearch.client.opensearch.core.search.HighlighterEncoder;
import org.opensearch.client.opensearch.core.search.HighlighterFragmenter;
import org.opensearch.client.opensearch.core.search.HighlighterOrder;
import org.opensearch.client.opensearch.core.search.HighlighterTagsSchema;
import org.opensearch.client.opensearch.core.search.HighlighterType;
import org.opensearch.client.opensearch.core.search.ScoreMode;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndicesOptions;
import org.springframework.data.elasticsearch.core.query.Order;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.RescorerQuery;
import org.springframework.data.elasticsearch.core.query.UpdateResponse;
import org.springframework.data.elasticsearch.core.query.types.ConflictsType;
import org.springframework.data.elasticsearch.core.query.types.OperatorType;
import org.springframework.data.elasticsearch.core.reindex.ReindexRequest;
import org.springframework.lang.Nullable;

/**
 * Utility to handle new OpenSearch client type values.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
final class TypeUtils {

    @Nullable
    static BoundaryScanner boundaryScanner(@Nullable String value) {

        if (value != null) {
            return switch (value.toLowerCase()) {
                case "chars" -> BoundaryScanner.Chars;
                case "sentence" -> BoundaryScanner.Sentence;
                case "word" -> BoundaryScanner.Word;
                default -> null;
            };
        }
        return null;
    }

    static Conflicts conflicts(ReindexRequest.Conflicts conflicts) {
        return switch (conflicts) {
            case ABORT -> Conflicts.Abort;
            case PROCEED -> Conflicts.Proceed;
        };
    }

    @Nullable
    static DistanceUnit distanceUnit(String unit) {

        return switch (unit.toLowerCase()) {
            case "in", "inch" -> DistanceUnit.Inches;
            case "yd", "yards" -> DistanceUnit.Yards;
            case "ft", "feet" -> DistanceUnit.Feet;
            case "km", "kilometers" -> DistanceUnit.Kilometers;
            case "nm", "nmi" -> DistanceUnit.NauticMiles;
            case "mm", "millimeters" -> DistanceUnit.Millimeters;
            case "cm", "centimeters" -> DistanceUnit.Centimeters;
            case "mi", "miles" -> DistanceUnit.Miles;
            case "m", "meters" -> DistanceUnit.Meters;
            default -> null;
        };
    }

    @Nullable
    static FieldType fieldType(String type) {

        for (FieldType fieldType : FieldType.values()) {

            if (fieldType.jsonValue().equals(type)) {
                return fieldType;
            }
        }
        return null;
    }

    @Nullable
    static String toString(@Nullable FieldValue fieldValue) {

        if (fieldValue == null) {
            return null;
        }

        switch (fieldValue._kind()) {
            case Double -> {
                return String.valueOf(fieldValue.doubleValue());
            }
            case Long -> {
                return String.valueOf(fieldValue.longValue());
            }
            case Boolean -> {
                return String.valueOf(fieldValue.booleanValue());
            }
            case String -> {
                return fieldValue.stringValue();
            }
            case Null -> {
                return null;
            }

            default -> throw new IllegalStateException("Unexpected value: " + fieldValue._kind());
        }
    }

    @Nullable
    static Object toObject(@Nullable FieldValue fieldValue) {

        if (fieldValue == null) {
            return null;
        }

        switch (fieldValue._kind()) {
            case Double -> {
                return Double.valueOf(fieldValue.doubleValue());
            }
            case Long -> {
                return Long.valueOf(fieldValue.longValue());
            }
            case Boolean -> {
                return Boolean.valueOf(fieldValue.booleanValue());
            }
            case String -> {
                return fieldValue.stringValue();
            }
            case Null -> {
                return null;
            }

            default -> throw new IllegalStateException("Unexpected value: " + fieldValue._kind());
        }
    }

    @Nullable
    static FieldValue toFieldValue(@Nullable Object fieldValue) {

        if (fieldValue == null) {
            return FieldValue.NULL;
        }

        if (fieldValue instanceof Boolean b) {
            return b ? FieldValue.TRUE : FieldValue.FALSE;
        }

        if (fieldValue instanceof String s) {
            return FieldValue.of(s);
        }

        if (fieldValue instanceof Long l) {
            return FieldValue.of(l);
        }

        if (fieldValue instanceof Integer i) {
            return FieldValue.of((long) i);
        }

        if (fieldValue instanceof Double d) {
            return FieldValue.of(d);
        }

        if (fieldValue instanceof Float f) {
            return FieldValue.of((double) f);
        }

        throw new IllegalStateException("Unexpected value: " + fieldValue);
    }

    @Nullable
    static GeoDistanceType geoDistanceType(GeoDistanceOrder.DistanceType distanceType) {

        return switch (distanceType) {
            case arc -> GeoDistanceType.Arc;
            case plane -> GeoDistanceType.Plane;
        };

    }

    @Nullable
    static SortOrder sortOrder(@Nullable Sort.Direction direction) {

        if (direction == null) {
            return null;
        }

        return switch (direction) {
            case ASC -> SortOrder.Asc;
            case DESC -> SortOrder.Desc;
        };

    }

    @Nullable
    static HighlighterFragmenter highlighterFragmenter(@Nullable String value) {

        if (value != null) {
            return switch (value.toLowerCase()) {
                case "simple" -> HighlighterFragmenter.Simple;
                case "span" -> HighlighterFragmenter.Span;
                default -> null;
            };
        }

        return null;
    }

    @Nullable
    static HighlighterOrder highlighterOrder(@Nullable String value) {

        if (value != null) {
            if ("score".equals(value.toLowerCase())) {
                return HighlighterOrder.Score;
            }
        }

        return null;
    }

    @Nullable
    static HighlighterType highlighterType(@Nullable String value) {

        if (value != null) {
            return switch (value.toLowerCase()) {
                case "unified" -> HighlighterType.of(fn -> fn.builtin(BuiltinHighlighterType.Unified));
                case "plain" -> HighlighterType.of(fn -> fn.builtin(BuiltinHighlighterType.Plain));
                case "fvh" -> HighlighterType.of(fn -> fn.builtin(BuiltinHighlighterType.FastVector));
                default -> null;
            };
        }

        return null;
    }

    @Nullable
    static HighlighterEncoder highlighterEncoder(@Nullable String value) {

        if (value != null) {
            return switch (value.toLowerCase()) {
                case "default" -> HighlighterEncoder.Default;
                case "html" -> HighlighterEncoder.Html;
                default -> null;
            };
        }

        return null;
    }

    @Nullable
    static HighlighterTagsSchema highlighterTagsSchema(@Nullable String value) {

        if (value != null) {
            if ("styled".equals(value.toLowerCase())) {
                return HighlighterTagsSchema.Styled;
            }
        }

        return null;
    }

    @Nullable
    static OpType opType(@Nullable IndexQuery.OpType opType) {

        if (opType != null) {
            return switch (opType) {
                case INDEX -> OpType.Index;
                case CREATE -> OpType.Create;
            };
        }
        return null;
    }

    static Refresh refresh(@Nullable RefreshPolicy refreshPolicy) {

        if (refreshPolicy == null) {
            return Refresh.False;
        }

        return switch (refreshPolicy) {
            case IMMEDIATE -> Refresh.True;
            case WAIT_UNTIL -> Refresh.WaitFor;
            case NONE -> Refresh.False;
        };
    }

    @Nullable
    static UpdateResponse.Result result(@Nullable Result result) {

        if (result == null) {
            return null;
        }

        return switch (result) {
            case Created -> UpdateResponse.Result.CREATED;
            case Updated -> UpdateResponse.Result.UPDATED;
            case Deleted -> UpdateResponse.Result.DELETED;
            case NotFound -> UpdateResponse.Result.NOT_FOUND;
            case NoOp -> UpdateResponse.Result.NOOP;
        };

    }

    @Nullable
    static ScoreMode scoreMode(@Nullable RescorerQuery.ScoreMode scoreMode) {

        if (scoreMode == null) {
            return null;
        }

        return switch (scoreMode) {
            case Default -> null;
            case Avg -> ScoreMode.Avg;
            case Max -> ScoreMode.Max;
            case Min -> ScoreMode.Min;
            case Total -> ScoreMode.Total;
            case Multiply -> ScoreMode.Multiply;
        };

    }

    @Nullable
    static SearchType searchType(@Nullable Query.SearchType searchType) {

        if (searchType == null) {
            return null;
        }

        return switch (searchType) {
            case QUERY_THEN_FETCH -> SearchType.QueryThenFetch;
            case DFS_QUERY_THEN_FETCH -> SearchType.DfsQueryThenFetch;
        };

    }

    @Nullable
    static SortMode sortMode(Order.Mode mode) {

        return switch (mode) {
            case min -> SortMode.Min;
            case max -> SortMode.Max;
            case median -> SortMode.Median;
            case avg -> SortMode.Avg;
        };

    }

    @Nullable
    static Time time(@Nullable Duration duration) {

        if (duration == null) {
            return null;
        }

        return Time.of(t -> t.time(duration.toMillis() + "ms"));
    }

    @Nullable
    static String timeStringMs(@Nullable Duration duration) {

        if (duration == null) {
            return null;
        }

        return duration.toMillis() + "ms";
    }

    @Nullable
    static VersionType versionType(
            @Nullable org.springframework.data.elasticsearch.annotations.Document.VersionType versionType) {

        if (versionType != null) {
            return switch (versionType) {
                case INTERNAL -> VersionType.Internal;
                case EXTERNAL -> VersionType.External;
                case EXTERNAL_GTE -> VersionType.ExternalGte;
                case FORCE -> VersionType.Force;
            };
        }

        return null;
    }

    static Integer waitForActiveShardsCount(@Nullable String value) {
        // values taken from the RHLC implementation
        if (value == null) {
            return -2;
        } else if ("all".equals(value.toUpperCase())) {
            return -1;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Illegale value for waitForActiveShards" + value);
            }
        }
    }

    /**
     * Converts a Long to a Float, returning null if the input is null.
     *
     * @param value the long value
     * @return a FLoat with the given value
     * @since 5.0
     */
    @Nullable
    static Float toFloat(@Nullable Long value) {
        return value != null ? Float.valueOf(value) : null;
    }

    /**
     * Converts a Float to a Long, returning null if the input is null.
     *
     * @param value the float value
     * @return a LOng with the given value
     * @since 5.0
     */
    @Nullable
    static Long toLong(@Nullable Float value) {
        return value != null ? value.longValue() : null;
    }

    /**
     * @sice 5.1
     */
    @Nullable
    public static List<ExpandWildcard> expandWildcards(@Nullable EnumSet<IndicesOptions.WildcardStates> wildcardStates) {
        return (wildcardStates != null && !wildcardStates.isEmpty()) ? wildcardStates.stream()
                .map(wildcardState -> ExpandWildcard.valueOf(wildcardState.name().toLowerCase())).collect(Collectors.toList())
                : null;
    }

    @Nullable
    static TypeMapping typeMapping(@Nullable Document mapping) {
        if (mapping != null) {
            return JsonpUtils.fromJson(mapping, TypeMapping._DESERIALIZER);
        }
        return null;
    }

    @Nullable
    static Document typeMapping(@Nullable TypeMapping typeMapping) {
        return (typeMapping != null) ? Document.parse(typeMapping.toJsonString()) : null;
    }

    @Nullable
    static IndexSettings indexSettings(@Nullable Map<String, Object> settings) {
        return settings != null ?  JsonpUtils.fromJson(Document.from(settings), IndexSettings._DESERIALIZER)
                : null;
    }

    /**
     * Convert a spring-data-elasticsearch operator to an OpenSearch operator.
     */
    @Nullable
    static Operator operator(@Nullable OperatorType operator) {
        return operator != null ? Operator.valueOf(operator.name()) : null;
    }

    /**
     * Convert a spring-data-elasticsearch {@literal conflicts} to an OpenSearch {@literal conflicts}.
     */
    @Nullable
    static Conflicts conflicts(@Nullable ConflictsType conflicts) {
        return conflicts != null ? Conflicts.valueOf(conflicts.name()) : null;
    }
}
