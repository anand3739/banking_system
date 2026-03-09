package com.example.banking.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        String url = environment.getProperty("SPRING_DATASOURCE_URL");
        String username = environment.getProperty("SPRING_DATASOURCE_USERNAME");
        String password = environment.getProperty("SPRING_DATASOURCE_PASSWORD");

        if (!StringUtils.hasText(url)) {
            String databaseUrl = environment.getProperty("DATABASE_URL");
            if (StringUtils.hasText(databaseUrl)) {
                ParsedDatabase parsed = parse(databaseUrl);
                url = parsed.jdbcUrl();
                if (!StringUtils.hasText(username)) {
                    username = parsed.username();
                }
                if (!StringUtils.hasText(password)) {
                    password = parsed.password();
                }
            }
        }

        if (!StringUtils.hasText(url)) {
            url = "jdbc:postgresql://localhost:5432/banking_db";
        }
        if (!StringUtils.hasText(username)) {
            username = "postgres";
        }
        if (!StringUtils.hasText(password)) {
            password = "postgres";
        }

        HikariDataSource hikari = new HikariDataSource();
        hikari.setDriverClassName("org.postgresql.Driver");
        hikari.setJdbcUrl(url);
        hikari.setUsername(username);
        hikari.setPassword(password);
        return hikari;
    }

    private ParsedDatabase parse(String value) {
        if (value.startsWith("jdbc:postgresql://")) {
            return new ParsedDatabase(value, null, null);
        }
        if (!(value.startsWith("postgres://") || value.startsWith("postgresql://"))) {
            throw new IllegalStateException("Unsupported DATABASE_URL format: " + value);
        }

        URI uri = URI.create(value);
        if (!StringUtils.hasText(uri.getHost()) || !StringUtils.hasText(uri.getPath()) || "/".equals(uri.getPath())) {
            throw new IllegalStateException("Invalid DATABASE_URL: missing host or database name");
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
        if (StringUtils.hasText(uri.getRawQuery())) {
            jdbcUrl += "?" + uri.getRawQuery();
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

        return new ParsedDatabase(jdbcUrl, username, password);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private record ParsedDatabase(String jdbcUrl, String username, String password) {
    }
}
