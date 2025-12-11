// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.cursor;

import io.dymes.exceptions.IllegalRequestException;
import io.dymes.exceptions.IllegalResponseException;
import io.dymes.exceptions.InfrastructureException;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Status;
import io.helidon.webclient.api.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dymes.client.Cursor;
import io.dymes.client.msg.DymesMessageReader;
import io.dymes.model.Message;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Integer.toHexString;

/**
 * Dymes forward cursor.
 * <p>
 * Currently, rewinding is not supported.
 */
public class DymesForwardCursor implements Cursor {

    public static DymesForwardCursor of(final String cursorId,
                                        final WebClient httpClient) {
        return of(cursorId, httpClient, DEFAULT_BATCH_SIZE);
    }

    public static DymesForwardCursor of(final String cursorId,
                                        final WebClient httpClient,
                                        final int batchSize) {
        return new DymesForwardCursor(cursorId, httpClient, batchSize);
    }

    private static final Logger logger = LoggerFactory.getLogger(DymesForwardCursor.class);

    public static final int DEFAULT_BATCH_SIZE = 16;
    public static final int MAX_BATCH_SIZE = 32;
    public static final int MIN_BATCH_SIZE = 1;

    private static final String CURSOR_PATH = "/api/v1/cursors/{id}";

    private final String cursorId;

    private final WebClient httpClient;

    private final int batchSize;
    private int idxBatch;

    private boolean closed = false;

    private List<Message> messageBatch;

    private DymesForwardCursor(final String cursorId,
                               final WebClient httpClient,
                               final int batchSize) {
        this.cursorId = cursorId;
        this.httpClient = Objects.requireNonNull(httpClient, "HTTP client required");
        if (batchSize < MIN_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size must be at least " + MIN_BATCH_SIZE);
        } else if (batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Batch size may not exceed " + MAX_BATCH_SIZE);
        }
        this.messageBatch = new ArrayList<>(batchSize);
        this.batchSize = batchSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Message> stream() {
        if (this.closed) {
            throw new IllegalStateException("Cursor is closed");
        }
        final var spliterator = Spliterators.
                spliteratorUnknownSize(this,
                                       Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SORTED);

        return StreamSupport.stream(spliterator, false)
                            .onClose(this::close);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        final var request = this.httpClient.delete(CURSOR_PATH)
                                           .pathParam("id", this.cursorId);

        try (final var response = request.request()) {
            switch (response.status().family()) {
                case Status.Family.SUCCESSFUL:
                    logger.trace("Cursor [{}] closed", this.cursorId);
                    break;
                case Status.Family.CLIENT_ERROR:
                    throw new IllegalRequestException("Close cursor request was rejected, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
                case Status.Family.SERVER_ERROR:
                    throw new InfrastructureException("Close cursor request failed, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
                default:
                    throw new IllegalResponseException("Close cursor request failed, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
            }
        } catch (final Exception e) {
            logger.warn("Failed to close cursor [{}]", this.cursorId, e);
        }

        this.closed = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        if (this.closed) {
            throw new IllegalStateException("Cursor is closed");
        }
        if (this.idxBatch >= this.messageBatch.size()) {
            this.messageBatch = nextBatch();
            if (this.messageBatch.isEmpty()) {
                return false;
            }
            this.idxBatch = 0;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Cursor at end");
        }
        return this.messageBatch.get(this.idxBatch++);
    }

    private List<Message> nextBatch() {

        final var request =
                this.httpClient.get(CURSOR_PATH)
                               .pathParam("id", this.cursorId)
                               .queryParam("batch_size", "0x" + toHexString(this.batchSize))
                               .accept(MediaTypes.APPLICATION_OCTET_STREAM);

        try (final var response = request.request()) {
            switch (response.status().family()) {
                case Status.Family.SUCCESSFUL:
                    try (var is = response.inputStream()) {
                        return DymesMessageReader.readAll(is);
                    } catch (IOException e) {
                        throw new InfrastructureException("Failed to fetch message batch", e);
                    }
                case Status.Family.CLIENT_ERROR:
                    throw new IllegalRequestException("Request to fetch batch was rejected, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
                case Status.Family.SERVER_ERROR:
                    throw new InfrastructureException("Request to fetch batch failed, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
                default:
                    throw new IllegalResponseException("Request to fetch batch failed, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
            }
        }
    }

}
