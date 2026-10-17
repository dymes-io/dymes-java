// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.append;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.f4b6a3.ulid.Ulid;
import io.dymes.client.dto.BatchCreationRequestDto;
import io.dymes.client.dto.BatchCreationResponseDto;
import io.dymes.client.dto.CreationRequestDto;
import io.dymes.client.jackson.DymesObjectMapperFactory;
import io.dymes.exceptions.IllegalRequestException;
import io.dymes.exceptions.IllegalResponseException;
import io.dymes.exceptions.InfrastructureException;
import io.dymes.exceptions.PartialAppendException;
import io.dymes.exceptions.UnsupportedRequestException;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Status;
import io.helidon.webclient.api.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * {@link BatchAppendRequest} over {@code POST /api/v1/messages/batch}. Sends the messages in chunks within the node's
 * limits ({@link #MAX_MESSAGES} a request, about {@link #MAX_REQUEST_SIZE} bytes); a correlation index into an earlier
 * chunk is sent as that message's id.
 */
public class DymesBatchAppender implements BatchAppendRequest {

    public static DymesBatchAppender of(final WebClient httpClient) {
        return new DymesBatchAppender(httpClient);
    }

    private static final Logger logger = LoggerFactory.getLogger(DymesBatchAppender.class);

    private static final String BATCH_PATH = "/api/v1/messages/batch";

    /** Most messages in one request (the node's limit). */
    static final int MAX_MESSAGES = 1_000;

    /** Request size a chunk stays within, below the node's 8 MiB body limit (base64 and JSON overhead included). */
    static final int MAX_REQUEST_SIZE = 6 * 1024 * 1024;

    private final WebClient httpClient;
    private final List<BatchMessage> messages = new ArrayList<>();

    private DymesBatchAppender(final WebClient httpClient) {
        this.httpClient = requireNonNull(httpClient, "HTTP client required");
    }

    @Override
    public DymesBatchAppender add(final BatchMessage message) {
        requireNonNull(message, "Message required");
        if (message.correlationIndex() != null && message.correlationIndex() >= this.messages.size()) {
            throw new IllegalArgumentException("Correlation index %d does not point at an earlier message (%d added)"
                    .formatted(message.correlationIndex(), this.messages.size()));
        }
        this.messages.add(message);
        return this;
    }

    @Override
    public DymesBatchAppender addAll(final List<BatchMessage> messages) {
        requireNonNull(messages, "Messages required").forEach(this::add);
        return this;
    }

    @Override
    public int size() {
        return this.messages.size();
    }

    @Override
    public List<Ulid> commit() {
        if (this.messages.isEmpty()) {
            throw new IllegalStateException("No messages to append");
        }
        final var ids = new ArrayList<Ulid>(this.messages.size());
        int start = 0;
        while (start < this.messages.size()) {
            final int end = chunkEnd(start);
            try {
                ids.addAll(send(start, end, ids));
            } catch (PartialAppendException e) {
                final var appended = new ArrayList<>(ids);
                appended.addAll(e.appended());
                throw new PartialAppendException(e.getMessage(), appended, e);
            } catch (RuntimeException e) {
                if (ids.isEmpty()) {
                    throw e;
                }
                throw new PartialAppendException("Batch append failed after %d of %d messages were stored: %s"
                        .formatted(ids.size(), this.messages.size(), e.getMessage()), ids, e);
            }
            start = end;
        }
        return List.copyOf(ids);
    }

    /** The end (exclusive) of the chunk starting at {@code start}: at most {@link #MAX_MESSAGES}, within the size. */
    private int chunkEnd(final int start) {
        int end = start;
        long size = 32;
        while (end < this.messages.size() && end - start < MAX_MESSAGES) {
            // Base64 grows the payload by a third; JSON adds field names and quoting.
            final long next = this.messages.get(end).estimatedSize() * 4L / 3 + 256;
            if (end > start && size + next > MAX_REQUEST_SIZE) {
                break;
            }
            size += next;
            end++;
        }
        return end;
    }

    private List<Ulid> send(final int start, final int end, final List<Ulid> earlier) {
        final var entries = new ArrayList<BatchCreationRequestDto.Entry>(end - start);
        for (int i = start; i < end; i++) {
            final var message = this.messages.get(i);
            String correlationId = message.correlationId() == null ? null : message.correlationId().toString();
            Integer correlationIndex = null;
            if (message.correlationIndex() != null) {
                if (message.correlationIndex() >= start) {
                    correlationIndex = message.correlationIndex() - start;
                } else {
                    correlationId = earlier.get(message.correlationIndex()).toString();
                }
            }
            final var kvPairs = message.headers()
                                       .entrySet()
                                       .stream()
                                       .map(e -> CreationRequestDto.KvPair.of(e.getKey(), e.getValue()))
                                       .toList();
            entries.add(new BatchCreationRequestDto.Entry(correlationId,
                                                          correlationIndex,
                                                          message.channel(),
                                                          message.routing(),
                                                          kvPairs,
                                                          Base64.getUrlEncoder()
                                                                .encodeToString(message.payload().getBytes(UTF_8))));
        }

        final String requestString;
        try {
            requestString = DymesObjectMapperFactory.of().writeValueAsString(new BatchCreationRequestDto(entries));
        } catch (JsonProcessingException e) {
            throw new IllegalRequestException("Unable to encode the batch", e);
        }

        final var httpRequest = this.httpClient.post(BATCH_PATH)
                                               .contentType(MediaTypes.APPLICATION_JSON)
                                               .accept(MediaTypes.APPLICATION_JSON);
        try (final var response = httpRequest.submit(requestString)) {
            final var status = response.status();
            // An older node's 404 may have no body.
            final var body = response.entity().hasEntity() ? response.as(String.class) : "";
            if (status.family() == Status.Family.SUCCESSFUL) {
                final var ids = ids(body);
                if (ids.size() != end - start) {
                    throw new IllegalResponseException("Batch append answered %d ids for %d messages"
                            .formatted(ids.size(), end - start));
                }
                logger.debug("Appended a batch of {} messages", ids.size());
                return ids;
            }
            if (status.code() == Status.NOT_FOUND_404.code() || status.code() == Status.NOT_IMPLEMENTED_501.code()) {
                throw new UnsupportedRequestException("The node has no batch append, status: [%s]".formatted(status));
            }
            if (status.family() == Status.Family.CLIENT_ERROR) {
                throw new IllegalRequestException("Batch append was rejected, status: [%s] and body [%s]"
                        .formatted(status, body));
            }
            if (status.family() == Status.Family.SERVER_ERROR) {
                final var stored = storedBefore(body);
                if (!stored.isEmpty()) {
                    throw new PartialAppendException("Batch append failed after %d messages were stored, status: [%s] and body [%s]"
                            .formatted(stored.size(), status, body), stored);
                }
                throw new InfrastructureException("Batch append failed, status: [%s] and body [%s]"
                        .formatted(status, body));
            }
            throw new IllegalResponseException("Batch append failed, status: [%s] and body [%s]".formatted(status, body));
        }
    }

    private static List<Ulid> ids(final String body) {
        try {
            final var dto = DymesObjectMapperFactory.of().readValue(body, BatchCreationResponseDto.class);
            if (dto.ids() == null) {
                throw new IllegalResponseException("Batch append answered no ids: [%s]".formatted(body));
            }
            return dto.ids().stream().map(Ulid::from).toList();
        } catch (JsonProcessingException e) {
            throw new IllegalResponseException("Unable to decode the batch answer: [%s]".formatted(body));
        }
    }

    /** The ids a failed batch says were stored, or none if the body doesn't list them. */
    private static List<Ulid> storedBefore(final String body) {
        try {
            final var dto = DymesObjectMapperFactory.of().readValue(body, BatchCreationResponseDto.class);
            return dto.ids() == null ? List.of() : dto.ids().stream().map(Ulid::from).toList();
        } catch (JsonProcessingException | RuntimeException e) {
            return List.of();
        }
    }
}
