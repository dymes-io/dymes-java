// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.cursor;

import jakarta.annotation.Nullable;
import io.dymes.client.Cursor;
import io.dymes.model.Message;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Dymes single message cursor.
 * <p>
 * Helper to provide unified cursor API even to single message results.
 */
public class DymesSingleMessageCursor implements Cursor {

    public static DymesSingleMessageCursor of(final @Nullable Message message) {
        return new DymesSingleMessageCursor(message);
    }

    private final @Nullable Message message;
    private boolean reachedEnd = false;
    private boolean closed = false;

    private DymesSingleMessageCursor(final @Nullable Message message) {
        this.message = message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Message> stream() {
        return Optional.ofNullable(this.message).stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        this.closed = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return this.message != null && !this.reachedEnd;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message next() {
        if (this.closed) {
            throw new IllegalStateException("Cursor is closed");
        } else if (this.message == null || this.reachedEnd) {
            throw new NoSuchElementException("Cursor at end");
        }
        this.reachedEnd = true;
        return this.message;
    }
}
