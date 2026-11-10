/*
 * Copyright 2021-2024 the original author or authors.
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

import io.micrometer.observation.ObservationRegistry;
import java.io.IOException;
import java.util.function.Supplier;
import org.opensearch.client.ApiClient;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.cluster.OpenSearchClusterClient;
import org.opensearch.client.transport.Transport;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * base class for a template that uses one of the {@link org.opensearch.client.opensearch.OpenSearchClient}'s child
 * clients like {@link OpenSearchClusterClient} or
 * {@link org.opensearch.client.opensearch.indices.OpenSearchIndicesClient}.
 *
 * @author Peter-Josef Meisch
 * @since 4.4
 */
public abstract class ChildTemplate<T extends Transport, CLIENT extends ApiClient<T, CLIENT>> implements ApplicationContextAware {

    private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
    @Nullable private OpenSearchObservationConvention observationConvention;
    @Nullable private Supplier<OpenSearchObservationSupport> observationSupportFactory;
    protected final CLIENT client;
    protected final RequestConverter requestConverter;
    protected final ResponseConverter responseConverter;
    protected final OpenSearchExceptionTranslator exceptionTranslator;

    public void setObservationRegistry(final ObservationRegistry observationRegistry) {
        Assert.notNull(observationRegistry, "observationRegistry must not be null");
        this.observationRegistry = observationRegistry;
    }

    public void setObservationConvention(@Nullable final OpenSearchObservationConvention observationConvention) {
        this.observationConvention = observationConvention;
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        if (observationRegistry == ObservationRegistry.NOOP) {
            applicationContext.getBeanProvider(ObservationRegistry.class).ifAvailable(this::setObservationRegistry);
        }
        if (observationConvention == null) {
            applicationContext.getBeanProvider(OpenSearchObservationConvention.class).ifAvailable(this::setObservationConvention);
        }
    }

    OpenSearchObservationSupport observations() {
        if (observationSupportFactory != null) {
            return observationSupportFactory.get();
        }
        return new OpenSearchObservationSupport(observationRegistry, observationConvention);
    }

    void setObservationSupportFactory(final Supplier<OpenSearchObservationSupport> factory) {
        this.observationSupportFactory = factory;
    }

    public ChildTemplate(CLIENT client, ElasticsearchConverter elasticsearchConverter) {
        this.client = client;
        JsonpMapper jsonpMapper = client._transport().jsonpMapper();
        requestConverter = new RequestConverter(elasticsearchConverter, jsonpMapper);
        responseConverter = new ResponseConverter(jsonpMapper);
        exceptionTranslator = new OpenSearchExceptionTranslator(jsonpMapper);
    }

    /**
     * Callback interface to be used with {@link #execute(ClientCallback)} for operating directly on the client.
     */
    @FunctionalInterface
    public interface ClientCallback<CLIENT, RESULT> {
        RESULT doWithClient(CLIENT client) throws IOException;
    }

    /**
     * Execute a callback with the client and provide exception translation.
     *
     * @param callback the callback to execute, must not be {@literal null}
     * @param <RESULT> the type returned from the callback
     * @return the callback result
     */
    public <RESULT> RESULT execute(ClientCallback<CLIENT, RESULT> callback) {

        Assert.notNull(callback, "callback must not be null");

        try {
            return callback.doWithClient(client);
        } catch (IOException | RuntimeException e) {
            throw exceptionTranslator.translateException(e);
        }
    }
}
