// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client;

import io.dymes.model.Message;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Dymes result cursor.
 */
public interface Cursor extends Iterator<Message>, AutoCloseable {

    /**
     * Traverse the {@link Cursor cursor} yielding a {@link Stream stream} of {@link Message messages}.
     *
     * @return {@link Stream} of {@link Message messages}
     */
    Stream<Message> stream();

    @Override
    default void remove() {
        throw new UnsupportedOperationException("Dymes cursors are read-only");
    }

    @Override
    void close();
}
