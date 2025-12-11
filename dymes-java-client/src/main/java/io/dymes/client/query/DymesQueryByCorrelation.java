// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.f4b6a3.ulid.Ulid;
import io.dymes.client.Cursor;
import io.dymes.client.cursor.DymesForwardCursor;
import io.dymes.client.dto.ClientQueryDto;
import io.dymes.client.dto.FilterDto;
import io.dymes.client.dto.QueryDto;
import io.dymes.client.jackson.DymesObjectMapperFactory;
import io.dymes.exceptions.IllegalRequestException;
import io.dymes.exceptions.IllegalResponseException;
import io.dymes.exceptions.InfrastructureException;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Status;
import io.helidon.webclient.api.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

import static java.util.Objects.requireNonNull;

public class DymesQueryByCorrelation implements QueryByCorrelation {

    public static DymesQueryByCorrelation of(final WebClient httpClient) {
        return new DymesQueryByCorrelation(httpClient);
    }

    private static final Logger logger = LoggerFactory.getLogger(DymesQueryByCorrelation.class);

    private static final String CURSORS_PATH = "/api/v1/cursors";

    private final WebClient httpClient;
    private final FilterSupport filterSupport;

    private QueryDto queryDto;

    public DymesQueryByCorrelation(final WebClient httpClient) {
        this.httpClient = requireNonNull(httpClient, "HTTP client required");
        this.filterSupport = FilterSupport.of();
    }

    @Override
    public QueryByCorrelation of(Ulid tail) {
        requireNonNull(tail, "Tail of correlation chain required");
        this.queryDto = QueryDto.ofCorrelation(tail);
        return this;
    }

    @Override
    public QueryByCorrelation channel(final BigInteger channel) {
        this.filterSupport.channel(channel);
        return this;
    }

    @Override
    public QueryByCorrelation channel(long channel) {
        this.filterSupport.channel(channel);
        return this;
    }

    @Override
    public CorrelationRoutingFilterBuilder routing() {
        return new InnerFilterBuilder();
    }

    @Override
    public QueryByCorrelation containsKey(String key) {
        this.filterSupport.containsKey(key);
        return this;
    }

    @Override
    public QueryByCorrelation keyValue(final String key, final String value) {
        this.filterSupport.keyValue(key, value);
        return this;
    }

    @Override
    public QueryByCorrelation withoutKeyValue(final String key, final String value) {
        this.filterSupport.withoutKeyValue(key, value);
        return this;
    }

    @Override
    public Cursor execute() {

        final var request =
                httpClient.post(CURSORS_PATH)
                          .contentType(MediaTypes.APPLICATION_JSON)
                          .accept(MediaTypes.TEXT_PLAIN);

        try {
            final var clientQueryDto = ClientQueryDto
                    .of(this.queryDto,
                        this.filterSupport.filters().stream());

            var requestString = DymesObjectMapperFactory.of().writeValueAsString(clientQueryDto);

            final var beforeCursor = System.currentTimeMillis();
            logger.trace("Opening cursor for correlation query request: {}", requestString);
            try (final var response = request.submit(requestString)) {
                switch (response.status().family()) {
                    case Status.Family.SUCCESSFUL:
                        final var cursorId = "0x" + response.as(String.class);
                        logger.debug("Cursor [{}] opened in {} ms",
                                     cursorId,
                                     System.currentTimeMillis() - beforeCursor);
                        return DymesForwardCursor.of(cursorId, this.httpClient);
                    case Status.Family.CLIENT_ERROR:
                        throw new IllegalRequestException("Correlation query was rejected, status: [%s] and body [%s]"
                                                                  .formatted(response.status(),
                                                                             response.as(String.class)));
                    case Status.Family.SERVER_ERROR:
                        throw new InfrastructureException("Correlation query failed, status: [%s] and body [%s]"
                                                                  .formatted(response.status(),
                                                                             response.as(String.class)));
                    default:
                        throw new IllegalResponseException("Correlation query failed, status: [%s] and body [%s]"
                                                                   .formatted(response.status(),
                                                                              response.as(String.class)));
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public class InnerFilterBuilder implements CorrelationRoutingFilterBuilder {

        @Override
        public DymesQueryByCorrelation widening(final BigInteger routingMask) {
            requireNonNull(routingMask, "Routing mask required");
            DymesQueryByCorrelation.this.filterSupport.filters.add(
                    new FilterDto(null,
                                  FilterDto.RoutingFilter.widening(routingMask),
                                  null));
            return DymesQueryByCorrelation.this;
        }

        @Override
        public DymesQueryByCorrelation widening(final long routingMask) {
            return widening(BigInteger.valueOf(routingMask));
        }

        @Override
        public DymesQueryByCorrelation narrowing(final BigInteger routingMask) {
            return narrowing(routingMask, routingMask);
        }

        @Override
        public DymesQueryByCorrelation narrowing(final long routingMask) {
            return narrowing(BigInteger.valueOf(routingMask), BigInteger.valueOf(routingMask));
        }

        @Override
        public DymesQueryByCorrelation narrowing(final BigInteger routingMask, final BigInteger comparand) {
            requireNonNull(routingMask, "Routing mask required");
            requireNonNull(comparand, "Routing comparand required");
            DymesQueryByCorrelation.this.filterSupport.filters.add(
                    new FilterDto(null,
                                  FilterDto.RoutingFilter.narrowing(routingMask, comparand),
                                  null));
            return DymesQueryByCorrelation.this;
        }

        @Override
        public DymesQueryByCorrelation narrowing(final long routingMask, final long comparand) {
            return narrowing(BigInteger.valueOf(routingMask), BigInteger.valueOf(comparand));
        }
    }

}
