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
public interface QueryByRange extends Query, FilteredQuery {

    /**
     * Selects messages committed between {@link Instant start} and {@link Instant end}, inclusive.
     *
     * @param start {@link Instant Start of time range}
     * @param end   {@link Instant End of time range}
     * @return Reference to self for fluent programming
     */
    QueryByRange of(final Instant start, final Instant end);

    /**
     * Selects messages committed between {@link Ulid start} and {@link Ulid end}, inclusive.
     *
     * @param start {@link Instant Start of time range}
     * @param end   {@link Instant End of time range}
     * @return Reference to self for fluent programming
     */
    QueryByRange of(final Ulid start, final Ulid end);

    /**
     * {@inheritDoc}
     */
    QueryByRange channel(final BigInteger channel);

    /**
     * {@inheritDoc}
     */
    QueryByRange channel(final long channel);

    /**
     * {@inheritDoc}
     */
    RangeRoutingFilterBuilder routing();

    /**
     * {@inheritDoc}
     */
    QueryByRange containsKey(final String key);

    /**
     * {@inheritDoc}
     */
    QueryByRange keyValue(final String key, final String value);

    /**
     * {@inheritDoc}
     */
    QueryByRange withoutKeyValue(final String key, final String value);

    /**
     * Fluent routing filter builder for range queries.
     */
    interface RangeRoutingFilterBuilder extends RoutingFilterBuilder {
        @Override
        QueryByRange widening(final BigInteger routingMask);

        @Override
        QueryByRange widening(final long routingMask);

        @Override
        QueryByRange narrowing(final BigInteger routingMask);

        @Override
        QueryByRange narrowing(final long routingMask);

        @Override
        QueryByRange narrowing(final BigInteger narrowingMask, final BigInteger comparand);

        @Override
        QueryByRange narrowing(final long narrowingMask, final long comparand);

    }

}
