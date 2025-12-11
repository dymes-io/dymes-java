// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.append;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.f4b6a3.ulid.Ulid;
import io.dymes.client.jackson.DymesObjectMapperFactory;
import io.dymes.exceptions.IllegalRequestException;
import io.dymes.exceptions.IllegalResponseException;
import io.dymes.exceptions.InfrastructureException;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Status;
import io.helidon.webclient.api.WebClient;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dymes.client.dto.CreationRequestDto;

import java.math.BigInteger;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class DymesAppender implements AppendRequest {

    public static DymesAppender of(final WebClient httpClient) {
        return new DymesAppender(httpClient);
    }

    private static final Logger logger = LoggerFactory.getLogger(DymesAppender.class);

    private static final String MESSAGES_PATH = "/api/v1/messages";
    private static final Ulid NIL_CORRELATION_ID = new Ulid(0, 0);
    private static final String NIL_CORRELATION_ID_TEXT = NIL_CORRELATION_ID.toString();

    private final WebClient httpClient;

    private Ulid correlationId;
    private BigInteger channel;
    private BigInteger routing;
    private String payload;
    private final Map<String, String> headers = new HashMap<>();

    private static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024 - 128;

    private int messageSize = 128;

    private DymesAppender(final WebClient httpClient) {
        this.httpClient = requireNonNull(httpClient, "HTTP client required");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AppendRequest withCorrelationId(final @Nullable String correlationId) {
        if (correlationId != null) {
            return withCorrelationId(Ulid.from(correlationId));
        }
        return withCorrelationId((Ulid) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DymesAppender withCorrelationId(final @Nullable Ulid correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AppendRequest withChannel(long channel) {
        return withChannel(BigInteger.valueOf(channel));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DymesAppender withChannel(final BigInteger channel) {
        requireNonNull(channel, "Channel required");
        if (channel.signum() < 0) {
            throw new IllegalArgumentException("Channel must be positive");
        }
        if (channel.bitLength() > 128) {
            throw new IllegalArgumentException("Channel exceeds 128 bits");
        }
        this.channel = channel;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AppendRequest withRouting(long routing) {
        return withRouting(BigInteger.valueOf(routing));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DymesAppender withRouting(final BigInteger routing) {
        requireNonNull(routing, "Routing required");
        if (routing.signum() < 0) {
            throw new IllegalArgumentException("Routing must be positive");
        }
        if (routing.bitLength() > 128) {
            throw new IllegalArgumentException("Routing exceeds 128 bits");
        }
        this.routing = routing;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DymesAppender withPayload(final String payload) {
        var payloadSize = requireNonNull(payload, "Payload required").getBytes(UTF_8).length;
        if (payloadSize + this.messageSize > MAX_MESSAGE_SIZE) {
            throw new IllegalArgumentException("Message will exceed " + MAX_MESSAGE_SIZE + " bytes");
        }
        this.payload = payload;
        this.messageSize += payloadSize;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DymesAppender withHeader(final String key,
                                    final String value) {
        var headerSize = 16 + requireNonNull(key, "Header key required").length()
                + requireNonNull(value, "Header value required").length();
        if (headerSize + this.messageSize > MAX_MESSAGE_SIZE) {
            throw new IllegalArgumentException("Message will exceed " + MAX_MESSAGE_SIZE + " bytes");
        }
        this.headers.put(key, value);
        this.messageSize += headerSize;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AppendRequest withHeaders(final Map<String, String> headers) {
        requireNonNull(headers, "Headers required").forEach(this::withHeader);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Ulid commit() {

        var requestDTO = buildDTO();

        try {
            final var httpRequest =
                    this.httpClient.post(MESSAGES_PATH)
                                   .contentType(MediaTypes.APPLICATION_JSON)
                                   .accept(MediaTypes.TEXT_PLAIN);
            var requestString = DymesObjectMapperFactory.of().writeValueAsString(requestDTO);
            try (final var response = httpRequest.submit(requestString)) {
                return switch (response.status().family()) {
                    case Status.Family.SUCCESSFUL -> Ulid.from(response.as(String.class));
                    case Status.Family.CLIENT_ERROR ->
                            throw new IllegalRequestException("Append request was rejected, status: [%s] and body [%s]".formatted(
                                    response.status(),
                                    response.as(String.class)));
                    case Status.Family.SERVER_ERROR ->
                            throw new InfrastructureException("Append request failed, status: [%s] and body [%s]".formatted(
                                    response.status(),
                                    response.as(String.class)));
                    default ->
                            throw new IllegalResponseException("Append request failed, status: [%s] and body [%s]".formatted(
                                    response.status(),
                                    response.as(String.class)));
                };
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private CreationRequestDto buildDTO() {
        requireNonNull(this.channel, "Channel required");
        requireNonNull(this.routing, "Routing required");
        requireNonNull(this.payload, "Payload required");

        final var encodedCorrelationId = Optional
                .ofNullable(this.correlationId)
                .map(Ulid::toString)
                .orElse(NIL_CORRELATION_ID_TEXT);

        final var kvPairs = this.headers
                .entrySet()
                .stream()
                .map(mpe -> CreationRequestDto.KvPair.of(mpe.getKey(), mpe.getValue()))
                .toList();

        final var encodedPayload = Base64.getUrlEncoder().encodeToString(payload.getBytes(UTF_8));

        return new CreationRequestDto(encodedCorrelationId,
                                      channel,
                                      routing,
                                      kvPairs,
                                      encodedPayload);
    }

}
