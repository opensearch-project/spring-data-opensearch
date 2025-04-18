package org.opensearch.spring.boot.autoconfigure.data;

import java.lang.annotation.Annotation;
import org.springframework.boot.autoconfigure.data.AbstractRepositoryConfigurationSourceSupport;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.config.ReactiveElasticsearchRepositoryConfigurationExtension;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

class ReactiveOpenSearchRepositoriesRegistrar extends AbstractRepositoryConfigurationSourceSupport {

    @Override
    protected Class<? extends Annotation> getAnnotation() {
        return EnableReactiveElasticsearchRepositories.class;
    }

    @Override
    protected Class<?> getConfiguration() {
        return EnableOpenSearchRepositoriesConfiguration.class;
    }

    @Override
    protected RepositoryConfigurationExtension getRepositoryConfigurationExtension() {
        return new ReactiveElasticsearchRepositoryConfigurationExtension();
    }

    @EnableReactiveElasticsearchRepositories
    private static final class EnableOpenSearchRepositoriesConfiguration {

    }

}
