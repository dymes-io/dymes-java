// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.append;

import com.github.f4b6a3.ulid.Ulid;
import jakarta.annotation.Nullable;

import java.math.BigInteger;
import java.util.Map;

/**
 * Dymes append request builder.
 */
public interface AppendRequest {

    /**
     * Sets the message {@link Ulid correlation identifier}.
     *
     * @param id {@link Ulid Correlation identifier}
     * @return Reference to self for fluent programming
     */
    AppendRequest withCorrelationId(final @Nullable Ulid id);

    /**
     * Sets the messages correlation identifier using the given canonical string representation
     * of the {@link Ulid correlation identifier}.
     *
     * @param id Canonical string representation of correlation identifier
     * @return Reference to self for fluent programming
     */
    AppendRequest withCorrelationId(final @Nullable String id);

    /**
     * Sets a "low" channel identifier using a {@code long} as ({@code 64-bit unsigned}).
     *
     * @param channel Channel identifier
     * @return Reference to self for fluent programming
     */
    AppendRequest withChannel(final long channel);

    /**
     * Sets the channel identifier ({@code 128-bit unsigned}).
     *
     * @param channel Channel identifier
     * @return Reference to self for fluent programming
     * @throws IllegalArgumentException if {@code channel} exceeds 128 bits.
     */
    AppendRequest withChannel(final BigInteger channel);

    /**
     * Sets a "low" routing value using a {@code long} as ({@code 64-bit unsigned}).
     *
     * @param routing Routing value
     * @return Reference to self for fluent programming
     */
    AppendRequest withRouting(final long routing);

    /**
     * Sets the routing value ({@code 128-bit unsigned}).
     *
     * @param routing Routing value
     * @return Reference to self for fluent programming
     * @throws IllegalArgumentException if {@code routing} exceeds 128 bits.
     */
    AppendRequest withRouting(final BigInteger routing);

    /**
     * Sets the message payload.
     * <p>
     * No marshalling/unmarshalling or parsing is performed on the payload.
     *
     * @param payload Message payload
     * @return Reference to self for fluent programming
     * @throws IllegalArgumentException if {@code payload} exceeds maximum message size.
     */
    AppendRequest withPayload(final String payload);

    /**
     * Adds a KV header with the given key and value.
     * <p>
     * Keys and values are UTF-8 strings, and neither key nor value may be null.
     *
     * @param key   Header key
     * @param value Header value
     * @return Reference to self for fluent programming
     */
    AppendRequest withHeader(final String key,
                             final String value);

    /**
     * Adds KV headers with the given {@link Map map's} keys and values.
     * <p>
     * Keys and values are UTF-8 strings, and neither key nor value may be null.
     *
     * @param headers {@link Map} with header keys and values.
     * @return Reference to self for fluent programming
     */
    AppendRequest withHeaders(final Map<String, String> headers);

    /**
     * Commits the message to Dymes.
     *
     * @return {@link Ulid Identifier} of the committed {@link io.dymes.model.Message message}.
     */
    Ulid commit();
}
