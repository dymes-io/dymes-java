// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client;

import io.dymes.client.append.AppendRequest;
import io.dymes.client.query.QueryBuilder;

/**
 * Dymes client API.
 */
public interface Dymes {

    /**
     * Allows fluent creation of a Dymes append request.
     *
     * @return Append request
     */
    AppendRequest append();

    /**
     * Allows fluent creation of a Dymes query request.
     *
     * @return Query builder
     */
    QueryBuilder query();

}
