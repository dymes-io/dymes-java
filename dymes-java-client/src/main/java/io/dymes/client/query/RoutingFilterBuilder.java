// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import java.math.BigInteger;

/**
 * Routing filter builder
 */
public interface RoutingFilterBuilder {

    /**
     * Applies a widening routing mask as filter.
     * <p>
     * Selects messages having a routing value that is non-zero after applying the given routing mask ({@code 128-bit unsigned}).
     *
     * @param wideningMask Routing mask
     * @return Reference to query builder for fluent programming
     */
    BaseFilteredQuery widening(final BigInteger wideningMask);

    /**
     * Applies a widening routing mask as filter.
     * <p>
     * Selects messages having a routing value that is non-zero after applying the given routing mask ({@code 64-bit unsigned}).
     *
     * @param wideningMask Routing mask
     * @return Reference to query builder for fluent programming
     */
    BaseFilteredQuery widening(final long wideningMask);

    /**
     * Applies a narrowing routing mask as filter.
     * <p>
     * Selects messages having a routing value that matched the routing mask after applying the given routing mask ({@code 128-bit unsigned}).
     *
     * @param narrowingMask Routing mask
     * @return Reference to query builder for fluent programming
     */
    BaseFilteredQuery narrowing(final BigInteger narrowingMask);

    /**
     * Applies a narrowing routing mask as filter.
     * <p>
     * Selects messages having a routing value that matched the routing mask after applying the given routing mask ({@code 64-bit unsigned}).
     *
     * @param narrowingMask Routing mask
     * @return Reference to query builder for fluent programming
     */
    BaseFilteredQuery narrowing(final long narrowingMask);

    /**
     * Applies a narrowing routing mask as filter.
     * <p>
     * Selects messages having a routing value that matched the routing comparand after applying the given routing mask ({@code 128-bit unsigned}).
     *
     * @param narrowingMask Routing mask
     * @param comparand Routing comparand
     * @return Reference to query builder for fluent programming
     */
    BaseFilteredQuery narrowing(final BigInteger narrowingMask, final BigInteger comparand);

    /**
     * Applies a narrowing routing mask as filter.
     * <p>
     * Selects messages having a routing value that matched the routing comparand after applying the given routing mask ({@code 64-bit unsigned}).
     *
     * @param narrowingMask Routing mask
     * @param comparand Routing comparand
     * @return Reference to query builder for fluent programming
     */
    BaseFilteredQuery narrowing(final long narrowingMask, final long comparand);

}
