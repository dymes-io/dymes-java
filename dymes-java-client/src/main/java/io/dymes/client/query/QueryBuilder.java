// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

public interface QueryBuilder {

    /**
     * Queries by message identifier.
     *
     * @return Fluent query builder
     */
    QueryById id();

    /**
     * Queries by time or identifier range.
     * <p>
     * Filters may optionally be applied.
     *
     * @return Fluent query builder
     */
    QueryByRange range();

    /**
     * Queries by channel.
     * <p>
     * Filters may optionally be applied.
     *
     * @return Fluent query builder
     */
    QueryByChannel channel();

    /**
     * Queries correlation chains.
     * <p>
     * Filters may optionally be applied.
     *
     * @return Fluent query builder
     */
    QueryByCorrelation correlation();
}
