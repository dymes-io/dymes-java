// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dymes.client.jackson.DymesObjectMapperFactory;
import io.helidon.common.config.Config;
import io.helidon.http.media.jackson.JacksonSupport;
import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.api.WebClientConfig;
import io.helidon.webclient.tracing.WebClientTracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dymes.client.append.AppendRequest;
import io.dymes.client.append.BatchAppendRequest;
import io.dymes.client.append.DymesBatchAppender;
import io.dymes.client.append.DymesAppender;
import io.dymes.client.query.*;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

/**
 * Dymes client.
 */
public class DymesClient implements Dymes, AutoCloseable {

    public static DymesClient of(final Config config) {
        return of(config, DymesObjectMapperFactory.of());
    }

    public static DymesClient of(final Config config,
                                 final ObjectMapper om) {
        return new DymesClient(config, om);
    }

    private static final Logger logger = LoggerFactory.getLogger(DymesClient.class);

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5);

    private final WebClient httpClient;

    public DymesClient(final Config config,
                       final ObjectMapper om) {
        super();
        requireNonNull(config, "Client configuration required");
        requireNonNull(om, "Object mapper required");

        final var connectTimeout =
                config.get("connect-timeout")
                      .as(Duration.class)
                      .orElse(DEFAULT_CONNECT_TIMEOUT);

        final var readTimeout =
                config.get("read-timeout")
                      .as(Duration.class)
                      .orElse(DEFAULT_READ_TIMEOUT);

        this.httpClient =
                WebClientConfig.builder()
                               .addMediaSupport(JacksonSupport.create(om))
                               .addService(WebClientTracing.create())
                               .baseUri(config.get("http-uri"))
                               .readTimeout(readTimeout)
                               .connectTimeout(connectTimeout)
                               .build();

        logger.debug("Dymes client started.");
    }

    @Override
    public void close() {
        this.httpClient.closeResource();
    }

    @Override
    public AppendRequest append() {
        return DymesAppender.of(this.httpClient);
    }

    @Override
    public BatchAppendRequest appendBatch() {
        return DymesBatchAppender.of(this.httpClient);
    }

    @Override
    public QueryBuilder query() {
        return ClientQueryBuilder.of(this.httpClient);
    }

    private record ClientQueryBuilder(WebClient httpClient) implements QueryBuilder {
        public static ClientQueryBuilder of(final WebClient httpClient) {
            return new ClientQueryBuilder(httpClient);
        }

        private ClientQueryBuilder(final WebClient httpClient) {
            this.httpClient = requireNonNull(httpClient);
        }

        @Override
        public QueryById id() {
            return DymesQueryById.of(this.httpClient);
        }

        @Override
        public QueryByRange range() {
            return DymesQueryByRange.of(this.httpClient);
        }

        @Override
        public QueryByChannel channel() {
            return DymesQueryByChannel.of(this.httpClient);
        }

        @Override
        public QueryByCorrelation correlation() {
            return DymesQueryByCorrelation.of(this.httpClient);
        }
    }
}
