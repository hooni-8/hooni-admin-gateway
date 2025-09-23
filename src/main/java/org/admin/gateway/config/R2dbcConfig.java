package org.admin.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.admin.gateway.repository.GatewayReactiveCrudRepositoryImpl;

@Configuration
@EnableR2dbcAuditing
@EnableR2dbcRepositories(basePackages = "org.admin.gateway.repository", repositoryBaseClass = GatewayReactiveCrudRepositoryImpl.class)
public class R2dbcConfig {
}