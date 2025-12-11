// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import io.dymes.client.Cursor;

/**
 * Dymes query.
 */
public interface Query {

    /**
     * Executes the query.
     *
     * @return {@link Cursor} allowing traversal of results
     */
    Cursor execute();
}
