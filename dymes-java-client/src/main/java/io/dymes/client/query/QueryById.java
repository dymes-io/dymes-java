// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import com.github.f4b6a3.ulid.Ulid;

/**
 * Fluent message identifier query builder.
 */
public interface QueryById extends Query {

    /**
     * Selects messages with the given {@link Ulid message identifier}.
     *
     * @param messageId {@link Ulid Message identifier}
     * @return Reference to self for fluent programming
     */
    QueryById of(final Ulid messageId);

    /**
     * Selects messages with the given canonical string representation of a {@link Ulid message identifier}.
     *
     * @param messageId Canonical string representation of a {@link Ulid message identifier}
     * @return Reference to self for fluent programming
     */
    QueryById of(final String messageId);

}
