// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import com.github.f4b6a3.ulid.Ulid;

import java.math.BigInteger;
import java.time.Instant;

/**
 * Fluent ranged query builder.
 */
public interface QueryByChannel extends Query, BaseFilteredQuery {

    /**
     * Selects messages on the given {@code channel} committed between {@link Instant start} and {@link Instant end}, inclusive.
     *
     * @param channelId Channel identifier
     * @param start     {@link Instant Start of time range}
     * @param end       {@link Instant End of time range}
     * @return Reference to self for fluent programming
     */
    QueryByChannel of(final BigInteger channelId, final Instant start, final Instant end);

    /**
     * Selects messages on the given {@code channel} committed between {@link Instant start} and {@link Instant end}, inclusive.
     *
     * @param channelId Channel identifier
     * @param start     {@link Instant Start of time range}
     * @param end       {@link Instant End of time range}
     * @return Reference to self for fluent programming
     */
    QueryByChannel of(final long channelId, final Instant start, final Instant end);

    /**
     * Selects messages on the given {@code channel} committed between {@link Ulid start} and {@link Ulid end}, inclusive.
     *
     * @param channelId Channel identifier
     * @param start     {@link Instant Start of time range}
     * @param end       {@link Instant End of time range}
     * @return Reference to self for fluent programming
     */
    QueryByChannel of(final BigInteger channelId, final Ulid start, final Ulid end);

    /**
     * Selects messages on the given {@code channel} committed between {@link Ulid start} and {@link Ulid end}, inclusive.
     *
     * @param channelId Channel identifier
     * @param start     {@link Instant Start of time range}
     * @param end       {@link Instant End of time range}
     * @return Reference to self for fluent programming
     */
    QueryByChannel of(final long channelId, final Ulid start, final Ulid end);

    /**
     * {@inheritDoc}
     */
    ChannelRoutingFilterBuilder routing();

    /**
     * {@inheritDoc}
     */
    QueryByChannel containsKey(final String key);

    /**
     * {@inheritDoc}
     */
    QueryByChannel keyValue(final String key, final String value);

    /**
     * {@inheritDoc}
     */
    QueryByChannel withoutKeyValue(final String key, final String value);

    /**
     * Fluent routing filter builder for channel queries.
     */
    interface ChannelRoutingFilterBuilder extends RoutingFilterBuilder {

        @Override
        QueryByChannel widening(final BigInteger routingMask);

        @Override
        QueryByChannel widening(final long routingMask);

        @Override
        QueryByChannel narrowing(final BigInteger routingMask);

        @Override
        QueryByChannel narrowing(final long routingMask);

        @Override
        QueryByChannel narrowing(final BigInteger narrowingMask, final BigInteger comparand);

        @Override
        QueryByChannel narrowing(final long narrowingMask, final long comparand);

    }

}
