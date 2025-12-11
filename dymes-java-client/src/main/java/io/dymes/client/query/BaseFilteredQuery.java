// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

/**
 * Fluent filtered Dymes query builder.
 */
public interface BaseFilteredQuery {

    /**
     * Selects messages matching a {@link io.dymes.client.dto.FilterDto.RoutingFilter routing filter}.
     *
     * @return Reference to filter builder for fluent programming
     */
    RoutingFilterBuilder routing();

    /**
     * Selects messages having the given header key (with any value).
     *
     * @param key Header key
     * @return Reference to self for fluent programming
     */
    BaseFilteredQuery containsKey(final String key);

    /**
     * Selects messages having the given header key and value.
     *
     * @param key   Header key
     * @param value Header value
     * @return Reference to self for fluent programming
     */
    BaseFilteredQuery keyValue(final String key, final String value);

    /**
     * Selects messages <em>NOT</em> having the given header key.
     *
     * @param key Header key
     * @return Reference to self for fluent programming
     */
    BaseFilteredQuery withoutKeyValue(final String key, final String value);

}
