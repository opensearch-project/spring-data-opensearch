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

import jakarta.json.JsonException;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.io.StringReader;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.JsonpMapperBase;
import org.opensearch.client.json.JsonpSerializable;
import org.springframework.data.elasticsearch.core.document.Document;

final class JsonpUtils {
    static final JsonProvider DEFAULT_PROVIDER = provider();

    static final JsonpMapper DEFAULT_JSONP_MAPPER = new JsonpMapperBase() {
        @Override
        public JsonProvider jsonProvider() {
            return DEFAULT_PROVIDER;
        }

        @Override
        public <T> void serialize(T value, JsonGenerator generator) {
            if (value instanceof JsonpSerializable) {
                ((JsonpSerializable) value).serialize(generator, this);
                return;
            }

            throw new JsonException(
                "Cannot find a serializer for type " + value.getClass().getName() +
                ". Consider using a full-featured JsonpMapper"
            );
        }

        @Override
        protected <T> JsonpDeserializer<T> getDefaultDeserializer(Class<T> clazz) {
            throw new JsonException(
                "Cannot find a default deserializer for type " + clazz.getName() +
                    ". Consider using a full-featured JsonpMapper");
        }
    };

    private JsonpUtils() {}

    static JsonProvider provider() {
        return JsonProvider.provider();
    }

    static <T> T fromJson(Document document, JsonpDeserializer<T> deserializer) {
        try (JsonParser parser = DEFAULT_JSONP_MAPPER.jsonProvider().createParser(new StringReader(document.toJson()))) {
            return deserializer.deserialize(parser, DEFAULT_JSONP_MAPPER);
        }
    }
}
