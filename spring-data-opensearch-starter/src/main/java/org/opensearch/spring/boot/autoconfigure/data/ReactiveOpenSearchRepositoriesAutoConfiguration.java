package org.opensearch.spring.boot.autoconfigure.data;

import org.opensearch.data.client.osc.ReactiveOpenSearchClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.support.ReactiveElasticsearchRepositoryFactoryBean;
import reactor.core.publisher.Mono;

@AutoConfiguration
@ConditionalOnClass({ ReactiveOpenSearchClient.class, ReactiveElasticsearchRepository.class, Mono.class })
@ConditionalOnProperty(prefix = "spring.data.elasticsearch.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnMissingBean(ReactiveElasticsearchRepositoryFactoryBean.class)
@Import(ReactiveOpenSearchRepositoriesRegistrar.class)
public class ReactiveOpenSearchRepositoriesAutoConfiguration {

}
