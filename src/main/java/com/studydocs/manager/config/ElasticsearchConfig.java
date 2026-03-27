package com.studydocs.manager.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration.
 * <p>
 * Enables Elasticsearch repositories ONLY when search.indexing.enabled=true.
 * When Elasticsearch is not running, set search.indexing.enabled=false
 * in application.properties so the app can start without ES.
 */
@Configuration
@ConditionalOnProperty(name = "search.indexing.enabled", havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackages = "com.studydocs.manager.search")
public class ElasticsearchConfig {
}
