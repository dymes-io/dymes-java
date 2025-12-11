// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import java.math.BigInteger;

/**
 * Fluent filtered Dymes query builder.
 */
public interface FilteredQuery extends BaseFilteredQuery {

    /**
     * Selects messages having the given channel identifier ({@code 128-bit unsigned}).
     *
     * @param channel Channel identifier
     * @return Reference to self for fluent programming
     */
    FilteredQuery channel(final BigInteger channel);

    /**
     * Selects messages having the given channel identifier ({@code 64-bit unsigned}).
     *
     * @param channel Channel identifier
     * @return Reference to self for fluent programming
     */
    FilteredQuery channel(final long channel);

    /**
     * Selects messages matching a {@link io.dymes.client.dto.FilterDto.RoutingFilter routing filter}.
     *
     * @return Reference to filter builder for fluent programming
     */
    @Override
    RoutingFilterBuilder routing();

    /**
     * Selects messages having the given header key (with any value).
     *
     * @param key Header key
     * @return Reference to self for fluent programming
     */
    @Override
    FilteredQuery containsKey(final String key);

    /**
     * Selects messages having the given header key and value.
     *
     * @param key   Header key
     * @param value Header value
     * @return Reference to self for fluent programming
     */
    @Override
    FilteredQuery keyValue(final String key, final String value);

    /**
     * Selects messages <em>NOT</em> having the given header key.
     *
     * @param key Header key
     * @return Reference to self for fluent programming
     */
    @Override
    FilteredQuery withoutKeyValue(final String key, final String value);

}
