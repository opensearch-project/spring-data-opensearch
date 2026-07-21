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

import java.lang.annotation.Annotation;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.core.index.MappingParameters;
import org.springframework.data.elasticsearch.core.index.MappingParametersCustomizer;
import org.springframework.util.Assert;

public class OpenSearchMappingParametersCustomizer implements MappingParametersCustomizer {
    @Override
    public MappingParameters from(Annotation annotation) {
        Assert.notNull(annotation, "annotation must not be null!");
        if (annotation instanceof Field field) {
            return new OpenSearchMappingParameters(field);
        } else if (annotation instanceof InnerField innerField) {
            return new OpenSearchMappingParameters(innerField);
        } else {
            throw new IllegalArgumentException("annotation must be an instance of @Field or @InnerField");
        }
    }
}
