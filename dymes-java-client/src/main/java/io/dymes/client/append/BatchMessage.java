// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.append;

import com.github.f4b6a3.ulid.Ulid;
import jakarta.annotation.Nullable;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * One message of a batch append ({@link BatchAppendRequest}).
 *
 * @param correlationId    a stored message to correlate to, or {@code null}
 * @param correlationIndex an earlier message of the same batch to correlate to, or {@code null}: its id is assigned
 *                         in the batch, so it can't be given as a correlation id. At most one of the two.
 * @param channel          channel identifier, at most 128 bits
 * @param routing          routing data, at most 128 bits
 * @param headers          key-value headers
 * @param payload          message body
 */
public record BatchMessage(@Nullable Ulid correlationId,
                           @Nullable Integer correlationIndex,
                           BigInteger channel,
                           BigInteger routing,
                           Map<String, String> headers,
                           String payload) {

    /** The largest message: a frame of 2 MiB, less its header. */
    static final int MAX_MESSAGE_SIZE = 2 * 1024 * 1024 - 128;

    public BatchMessage {
        requireNonNull(channel, "Channel required");
        requireNonNull(routing, "Routing required");
        requireNonNull(payload, "Payload required");
        // In the order given (Map.copyOf would lose it), and unmodifiable.
        headers = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(requireNonNull(headers, "Headers required")));
        if (correlationId != null && correlationIndex != null) {
            throw new IllegalStateException("At most one of a correlation id and a correlation index");
        }
        if (correlationIndex != null && correlationIndex < 0) {
            throw new IllegalArgumentException("Correlation index must not be negative");
        }
    }

    /** Approximate size of the message's frame in bytes (UTF-8), for the size limits. */
    int estimatedSize() {
        var size = 128 + payload.getBytes(UTF_8).length;
        for (var header : headers.entrySet()) {
            size += 16 + header.getKey().getBytes(UTF_8).length + header.getValue().getBytes(UTF_8).length;
        }
        return size;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a {@link BatchMessage}: channel, routing and payload are required.
     */
    public static final class Builder {

        private Ulid correlationId;
        private Integer correlationIndex;
        private BigInteger channel;
        private BigInteger routing;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String payload;

        private Builder() {
        }

        /** Correlates to a stored message. */
        public Builder withCorrelationId(final Ulid correlationId) {
            this.correlationId = requireNonNull(correlationId, "Correlation id required");
            return this;
        }

        /** Correlates to a stored message, given as ULID text. */
        public Builder withCorrelationId(final String correlationId) {
            return withCorrelationId(Ulid.from(requireNonNull(correlationId, "Correlation id required")));
        }

        /** Correlates to the earlier message of the same batch at {@code index} (from 0, in the order added). */
        public Builder withCorrelationIndex(final int index) {
            if (index < 0) {
                throw new IllegalArgumentException("Correlation index must not be negative");
            }
            this.correlationIndex = index;
            return this;
        }

        public Builder withChannel(final long channel) {
            return withChannel(BigInteger.valueOf(channel));
        }

        public Builder withChannel(final BigInteger channel) {
            this.channel = checked(channel, "Channel");
            return this;
        }

        public Builder withRouting(final long routing) {
            return withRouting(BigInteger.valueOf(routing));
        }

        public Builder withRouting(final BigInteger routing) {
            this.routing = checked(routing, "Routing");
            return this;
        }

        public Builder withHeader(final String key, final String value) {
            this.headers.put(requireNonNull(key, "Header key required"), requireNonNull(value, "Header value required"));
            return this;
        }

        public Builder withHeaders(final Map<String, String> headers) {
            requireNonNull(headers, "Headers required").forEach(this::withHeader);
            return this;
        }

        public Builder withPayload(final String payload) {
            this.payload = requireNonNull(payload, "Payload required");
            return this;
        }

        public BatchMessage build() {
            requireNonNull(this.channel, "Channel required");
            requireNonNull(this.routing, "Routing required");
            requireNonNull(this.payload, "Payload required");
            var message = new BatchMessage(correlationId, correlationIndex, channel, routing, headers, payload);
            if (message.estimatedSize() > MAX_MESSAGE_SIZE) {
                throw new IllegalArgumentException("Message will exceed " + MAX_MESSAGE_SIZE + " bytes");
            }
            return message;
        }

        private static BigInteger checked(final BigInteger value, final String what) {
            requireNonNull(value, what + " required");
            if (value.signum() < 0) {
                throw new IllegalArgumentException(what + " must be positive");
            }
            if (value.bitLength() > 128) {
                throw new IllegalArgumentException(what + " exceeds 128 bits");
            }
            return value;
        }
    }
}
