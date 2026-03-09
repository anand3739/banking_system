package com.example.banking.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Supports Render-style DATABASE_URL values like:
 * - postgres://user:pass@host:5432/db
 * - postgresql://user:pass@host:5432/db
 * and converts them into Spring datasource properties before auto-configuration.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // If explicit Spring datasource URL is set, do not override it.
        if (StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_URL"))) {
            return;
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (!StringUtils.hasText(databaseUrl)) {
            return;
        }

        Map<String, Object> overrides = new HashMap<>();
        if (isJdbcUrl(databaseUrl)) {
            overrides.put("spring.datasource.url", databaseUrl);
        } else if (isPostgresUri(databaseUrl)) {
            ParsedDatabaseUrl parsed = parsePostgresUri(databaseUrl);
            overrides.put("spring.datasource.url", parsed.jdbcUrl());
            if (StringUtils.hasText(parsed.username())) {
                overrides.put("spring.datasource.username", parsed.username());
            }
            if (StringUtils.hasText(parsed.password())) {
                overrides.put("spring.datasource.password", parsed.password());
            }
        } else {
            return;
        }

        environment.getPropertySources()
                .addFirst(new MapPropertySource("databaseUrlAutoConfig", overrides));
    }

    @Override
    public int getOrder() {
        // Run after ConfigDataEnvironmentPostProcessor so this can override defaults
        // from application.properties when DATABASE_URL is present.
        return Ordered.HIGHEST_PRECEDENCE + 11;
    }

    private boolean isJdbcUrl(String value) {
        return value.startsWith("jdbc:postgresql://");
    }

    private boolean isPostgresUri(String value) {
        return value.startsWith("postgres://") || value.startsWith("postgresql://");
    }

    private ParsedDatabaseUrl parsePostgresUri(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String path = uri.getPath();
        if (!StringUtils.hasText(host) || !StringUtils.hasText(path) || "/".equals(path)) {
            throw new IllegalStateException("Invalid DATABASE_URL format: missing host or database name");
        }

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
        String rawQuery = uri.getRawQuery();
        if (StringUtils.hasText(rawQuery)) {
            jdbcUrl = jdbcUrl + "?" + rawQuery;
        }

        String username = null;
        String password = null;
        String userInfo = uri.getRawUserInfo();
        if (StringUtils.hasText(userInfo)) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        return new ParsedDatabaseUrl(jdbcUrl, username, password);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
