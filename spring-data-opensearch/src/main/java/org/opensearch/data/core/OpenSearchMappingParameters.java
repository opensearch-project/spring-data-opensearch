/*
 * Copyright 2026 the original author or authors.
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

package org.opensearch.data.core;

import java.io.IOException;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldElementType;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.KnnAlgorithmType;
import org.springframework.data.elasticsearch.annotations.KnnSimilarity;
import org.springframework.data.elasticsearch.core.index.MappingParameters;
import tools.jackson.databind.node.ObjectNode;

class OpenSearchMappingParameters extends MappingParameters {
    static final String FIELD_PARAM_DIMENSION = "dimension";
    static final String FIELD_PARAM_DATA_TYPE = "data_type";
    static final String FIELD_PARAM_METHOD = "method";
    static final String FIELD_PARAM_NAME = "name";
    static final String FIELD_PARAM_M = "m";
    static final String FIELD_PARAM_EF_CONSTRUCTION = "ef_construction";
    static final String FIELD_PARAM_PARAMETERS = "parameters";
    static final String FIELD_PARAM_SPACE_TYPE = "space_type";

    protected OpenSearchMappingParameters(Field field) {
        super(field);
    }

    protected OpenSearchMappingParameters(InnerField field) {
        super(field);
    }

    @Override
    public void writeTypeAndParametersTo(ObjectNode objectNode) throws IOException {
        super.writeTypeAndParametersTo(objectNode);
        if (type() == FieldType.Dense_Vector) {
            // "dims" -> "dimension"
            objectNode.remove("dims");
            objectNode.put(FIELD_PARAM_DIMENSION, dims());

            // "element_type" -> "data_type"
            if (!FieldElementType.DEFAULT.equals(elementType())) {
                objectNode.remove("element_type");
                objectNode.put(FIELD_PARAM_DATA_TYPE, elementType());
            }

            // "similarity" -> "space_type"
            if (knnSimilarity() != KnnSimilarity.DEFAULT) {
                objectNode.remove("similarity");
                objectNode.put(FIELD_PARAM_SPACE_TYPE, toSpaceType(knnSimilarity()));
            }

            // "index_options" -> "method"
            if (knnIndexOptions() != null) {
                objectNode.remove("index_options");
                ObjectNode methodNode = objectNode.putObject(FIELD_PARAM_METHOD);
                KnnAlgorithmType algoType = knnIndexOptions().type();
                if (algoType != KnnAlgorithmType.DEFAULT) {
                    methodNode.put(FIELD_PARAM_NAME, algoType.getType());
                }

                ObjectNode parametersNode = methodNode.putObject(FIELD_PARAM_PARAMETERS);
                if (knnIndexOptions().m() >= 0) {
                    parametersNode.put(FIELD_PARAM_M, knnIndexOptions().m());
                }
                if (knnIndexOptions().efConstruction() >= 0) {
                    parametersNode.put(FIELD_PARAM_EF_CONSTRUCTION, knnIndexOptions().efConstruction());
                }
            }
        }
    }

    private static String toSpaceType(KnnSimilarity similarity) {
        switch (similarity) {
            case COSINE: return "cosinesimil";
            case L2_NORM: return "l2";
            case DOT_PRODUCT: return "innerproduct";
            case MAX_INNER_PRODUCT: return "innerproduct";
            case DEFAULT: return null;
        }
        return "l2";
    }
}
