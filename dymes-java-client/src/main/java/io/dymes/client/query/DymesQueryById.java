// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import com.github.f4b6a3.ulid.Ulid;
import io.dymes.exceptions.IllegalRequestException;
import io.dymes.exceptions.IllegalResponseException;
import io.dymes.exceptions.InfrastructureException;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.http.Status;
import io.helidon.webclient.api.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.dymes.client.Cursor;
import io.dymes.client.cursor.DymesSingleMessageCursor;
import io.dymes.client.msg.DymesMessageReader;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class DymesQueryById implements QueryById {

    public static DymesQueryById of(final WebClient httpClient) {
        return new DymesQueryById(httpClient);
    }

    private static final Logger logger = LoggerFactory.getLogger(DymesQueryById.class);

    private static final String MESSAGE_PATH = "/api/v1/messages/{id}";

    private final WebClient httpClient;

    private Ulid messageId;

    public DymesQueryById(final WebClient httpClient) {
        this.httpClient = requireNonNull(httpClient, "HTTP client required");
    }

    @Override
    public QueryById of(final Ulid messageId) {
        this.messageId = requireNonNull(messageId, "Message Id required");
        return this;
    }

    @Override
    public QueryById of(final String messageId) {
        return of(Ulid.from(requireNonNull(messageId, "Message Id required")));
    }

    @Override
    public Cursor execute() {

        requireNonNull(this.messageId, "Message id required");

        logger.trace("Executing dymes query by id [{}]", this.messageId);
        final var beforeQuery = System.currentTimeMillis();
        try (final var response =
                     this.httpClient.get(MESSAGE_PATH)
                                    .pathParam("id", this.messageId.toString())
                                    .accept(MediaTypes.APPLICATION_OCTET_STREAM)
                                    .request()) {

            if (response.status().code() == Status.OK_200.code()) {
                final var afterQuery = System.currentTimeMillis();
                logger.debug("Executed Dymes query by id [{}] in {} ms", this.messageId, afterQuery - beforeQuery);
                try (var is = response.inputStream()) {
                    final var message = DymesMessageReader.readOne(is);
                    logger.debug("Read Dymes response in {} ms", System.currentTimeMillis() - afterQuery);
                    return DymesSingleMessageCursor.of(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (response.status().code() == Status.NOT_FOUND_404.code()) {
                return DymesSingleMessageCursor.of(null);
            }

            switch (response.status().family()) {
                case Status.Family.CLIENT_ERROR:
                    throw new IllegalRequestException("Id query was rejected, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
                case Status.Family.SERVER_ERROR:
                    throw new InfrastructureException("Id query failed, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
                default:
                    throw new IllegalResponseException("Id query failed, status: [%s] and body [%s]".formatted(
                            response.status(),
                            response.as(String.class)));
            }
        }
    }

}
