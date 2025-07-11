/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.data.client.orhlc;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.indices.PutIndexTemplateRequest;
import org.opensearch.common.xcontent.XContentHelper;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.xcontent.ToXContent;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.Settings;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.util.StreamUtils;

public class RequestFactoryIndexTemplateTests {

    private static RequestFactory requestFactory;

    @BeforeAll
    static void setUpAll() {
        SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
        mappingContext.setInitialEntitySet(
                new HashSet<>(Arrays.asList(RequestFactoryTests.Person.class, RequestFactoryTests.EntityWithSeqNoPrimaryTerm.class)));
        mappingContext.afterPropertiesSet();

        MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
        converter.afterPropertiesSet();

        requestFactory = new RequestFactory((converter));
    }

    @Test
    void shouldCreatePutIndexTemplateRequest() throws JSONException, IOException {
        var esSettingsDocument = org.springframework.data.elasticsearch.core.document.Document.create();
        esSettingsDocument.put("index.number_of_replicas", 2);
        esSettingsDocument.put("index.number_of_shards", 3);
        esSettingsDocument.put("index.refresh_interval", "7s");
        esSettingsDocument.put("index.store.type", "oops");
        var mappings = org.springframework.data.elasticsearch.core.document.Document.parse(
                "{\"properties\":{\"price\":{\"type\":\"double\"}}}");
        AliasActions aliasActions = new AliasActions(
                new AliasAction.Add(AliasActionParameters.builderForTemplate()
                        .withAliases("alias1", "alias2")
                        .build()),
                new AliasAction.Add(AliasActionParameters.builderForTemplate()
                        .withAliases("alias3")
                        .withRouting("11")
                        .build()));
        Settings settings = new Settings();
        settings.put("index.number_of_replicas", 2);
        settings.put("index.number_of_shards", 3);
        settings.put("index.refresh_interval", "7s");
        settings.put("index.store.type", "oops");
        var request = org.springframework.data.elasticsearch.core.index.PutIndexTemplateRequest.builder()
                .withName("test-template")
                .withIndexPatterns("test-*")
                .withSettings(settings)
                .withMapping(mappings) //
                .withAliasActions(aliasActions) //
                .build();

        PutIndexTemplateRequest actualPutIndexTemplateRequest = requestFactory.putIndexTemplateRequest(request);

        String actualRequestJson = requestToString(actualPutIndexTemplateRequest);
        String expectedRequestJson = StreamUtils.copyToString(
                new ClassPathResource("index-template-requests/put-index-template.json").getInputStream(),
                StandardCharsets.UTF_8);
        assertEquals(expectedRequestJson, actualRequestJson, false);
    }

    private String requestToString(ToXContent request) throws IOException {
        return XContentHelper.toXContent(request, XContentType.JSON, true).utf8ToString();
    }
}
