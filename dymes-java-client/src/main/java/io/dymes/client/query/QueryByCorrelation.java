// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import com.github.f4b6a3.ulid.Ulid;

import java.math.BigInteger;

/**
 * Fluent correlation chain query builder.
 */
public interface QueryByCorrelation extends Query, FilteredQuery {

    /**
     * Selects messages in the correlation chain starting at the {@link Ulid tail}, inclusive.
     *
     * @param tail {@link Ulid Tail of correlation chain}
     * @return Reference to self for fluent programming
     */
    QueryByCorrelation of(final Ulid tail);

    /**
     * {@inheritDoc}
     */
    QueryByCorrelation channel(final BigInteger channel);

    /**
     * {@inheritDoc}
     */
    QueryByCorrelation channel(final long channel);

    /**
     * {@inheritDoc}
     */
    CorrelationRoutingFilterBuilder routing();

    /**
     * {@inheritDoc}
     */
    QueryByCorrelation containsKey(final String key);

    /**
     * {@inheritDoc}
     */
    QueryByCorrelation keyValue(final String key, final String value);

    /**
     * {@inheritDoc}
     */
    QueryByCorrelation withoutKeyValue(final String key, final String value);

    /**
     * Fluent routing filter builder for correlation queries.
     */
    interface CorrelationRoutingFilterBuilder extends RoutingFilterBuilder {
        @Override
        QueryByCorrelation widening(final BigInteger routingMask);

        @Override
        QueryByCorrelation widening(final long routingMask);

        @Override
        QueryByCorrelation narrowing(final BigInteger routingMask);

        @Override
        QueryByCorrelation narrowing(final long routingMask);

        @Override
        QueryByCorrelation narrowing(final BigInteger narrowingMask, final BigInteger comparand);

        @Override
        QueryByCorrelation narrowing(final long narrowingMask, final long comparand);

    }

}
