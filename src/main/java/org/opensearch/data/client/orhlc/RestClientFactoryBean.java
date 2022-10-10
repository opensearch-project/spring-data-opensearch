/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.data.client.orhlc;


import java.net.URL;
import java.util.ArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * RestClientFactoryBean
 * @since since 5.0
 */
public class RestClientFactoryBean implements FactoryBean<RestHighLevelClient>, InitializingBean, DisposableBean {

    private static final Log LOGGER = LogFactory.getLog(RestClientFactoryBean.class);

    private @Nullable RestHighLevelClient client;
    private String hosts = "http://localhost:9200";
    static final String COMMA = ",";

    @Override
    public void destroy() {
        try {
            LOGGER.info("Closing OpenSearch  client");
            if (client != null) {
                client.close();
            }
        } catch (final Exception e) {
            LOGGER.error("Error closing OpenSearch client: ", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        buildClient();
    }

    @Override
    public RestHighLevelClient getObject() {

        if (client == null) {
            throw new FactoryBeanNotInitializedException();
        }

        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return RestHighLevelClient.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    protected void buildClient() throws Exception {

        Assert.hasText(hosts, "[Assertion Failed] At least one host must be set.");

        ArrayList<HttpHost> httpHosts = new ArrayList<>();
        for (String host : hosts.split(COMMA)) {
            URL hostUrl = new URL(host);
            httpHosts.add(new HttpHost(hostUrl.getHost(), hostUrl.getPort(), hostUrl.getProtocol()));
        }
        client = new RestHighLevelClient(RestClient.builder(httpHosts.toArray(new HttpHost[httpHosts.size()])));
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public String getHosts() {
        return this.hosts;
    }
}
