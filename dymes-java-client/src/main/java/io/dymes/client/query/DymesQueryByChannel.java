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
import java.time.Instant;

import static java.util.Objects.requireNonNull;

public class DymesQueryByChannel implements QueryByChannel {

    public static DymesQueryByChannel of(final WebClient httpClient) {
        return new DymesQueryByChannel(httpClient);
    }

    private static final Logger logger = LoggerFactory.getLogger(DymesQueryByChannel.class);

    private static final String CURSORS_PATH = "/api/v1/cursors";

    private final WebClient httpClient;
    private final FilterSupport filterSupport;

    private QueryDto channelQueryDto;

    public DymesQueryByChannel(final WebClient httpClient) {
        this.httpClient = requireNonNull(httpClient, "HTTP client required");
        this.filterSupport = FilterSupport.of();
    }

    @Override
    public QueryByChannel of(final BigInteger channelId, final Instant start, final Instant end) {
        requireNonNull(channelId, "Channel identifier required");
        requireNonNull(start, "Start of range required");
        requireNonNull(end, "End of range required");
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start of range must be before the end");
        } else if (start.isBefore(Instant.EPOCH)) {
            throw new IllegalArgumentException("Start of range must fall on or after Unix Epoch");
        }
        return of(channelId, Ulid.min(start.toEpochMilli()), Ulid.min(end.toEpochMilli()));
    }

    @Override
    public QueryByChannel of(final long channelId, final Instant start, final Instant end) {
        return of(BigInteger.valueOf(channelId), start, end);
    }

    @Override
    public QueryByChannel of(final BigInteger channelId, final Ulid start, final Ulid end) {
        requireNonNull(channelId, "Channel identifier required");
        requireNonNull(start, "Start of range required");
        requireNonNull(end, "End of range required");
        if (end.compareTo(start) < 1) {
            throw new IllegalArgumentException("Start of range must be before the end");
        }
        this.channelQueryDto = QueryDto.ofChannel(channelId, start, end);
        return this;
    }

    @Override
    public QueryByChannel of(final long channelId, final Ulid start, final Ulid end) {
        return of(BigInteger.valueOf(channelId), start, end);
    }

    @Override
    public ChannelRoutingFilterBuilder routing() {
        return new InnerFilterBuilder();
    }

    @Override
    public QueryByChannel containsKey(String key) {
        this.filterSupport.containsKey(key);
        return this;
    }

    @Override
    public QueryByChannel keyValue(final String key, final String value) {
        this.filterSupport.keyValue(key, value);
        return this;
    }

    @Override
    public QueryByChannel withoutKeyValue(final String key, final String value) {
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
                    .of(this.channelQueryDto,
                        this.filterSupport.filters().stream());

            var requestString = DymesObjectMapperFactory.of().writeValueAsString(clientQueryDto);

            final var beforeCursor = System.currentTimeMillis();
            logger.trace("Opening cursor for request: {}", requestString);
            try (final var response = request.submit(requestString)) {
                switch (response.status().family()) {
                    case Status.Family.SUCCESSFUL:
                        final var cursorId = "0x" + response.as(String.class);
                        logger.debug("Cursor [{}] opened in {} ms",
                                     cursorId,
                                     System.currentTimeMillis() - beforeCursor);
                        return DymesForwardCursor.of(cursorId, this.httpClient);
                    case Status.Family.CLIENT_ERROR:
                        throw new IllegalRequestException("Channel query was rejected, status: [%s] and body [%s]".formatted(
                                response.status(),
                                response.as(String.class)));
                    case Status.Family.SERVER_ERROR:
                        throw new InfrastructureException("Channel query failed, status: [%s] and body [%s]".formatted(
                                response.status(),
                                response.as(String.class)));
                    default:
                        throw new IllegalResponseException("Channel query failed, status: [%s] and body [%s]".formatted(
                                response.status(),
                                response.as(String.class)));
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public class InnerFilterBuilder implements ChannelRoutingFilterBuilder {

        @Override
        public DymesQueryByChannel widening(final BigInteger routingMask) {
            requireNonNull(routingMask, "Routing mask required");
            DymesQueryByChannel.this.filterSupport.filters.add(
                    new FilterDto(null,
                                  FilterDto.RoutingFilter.widening(routingMask),
                                  null));
            return DymesQueryByChannel.this;
        }

        @Override
        public DymesQueryByChannel widening(final long routingMask) {
            return widening(BigInteger.valueOf(routingMask));
        }

        @Override
        public DymesQueryByChannel narrowing(final BigInteger routingMask) {
            return narrowing(routingMask, routingMask);
        }

        @Override
        public DymesQueryByChannel narrowing(final long routingMask) {
            return narrowing(BigInteger.valueOf(routingMask), BigInteger.valueOf(routingMask));
        }

        @Override
        public DymesQueryByChannel narrowing(final BigInteger routingMask, final BigInteger comparand) {
            requireNonNull(routingMask, "Routing mask required");
            requireNonNull(comparand, "Routing comparand required");
            DymesQueryByChannel.this.filterSupport.filters.add(
                    new FilterDto(null,
                                  FilterDto.RoutingFilter.narrowing(routingMask, comparand),
                                  null));
            return DymesQueryByChannel.this;
        }

        @Override
        public DymesQueryByChannel narrowing(final long routingMask, final long comparand) {
            return narrowing(BigInteger.valueOf(routingMask), BigInteger.valueOf(comparand));
        }
    }

}
