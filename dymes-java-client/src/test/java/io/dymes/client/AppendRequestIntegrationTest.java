// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.dymes.testing.DymesContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@Testcontainers
public class AppendRequestIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(AppendRequestIntegrationTest.class);

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    @Test
    public void should_create_message_without_headers() {

        final var payload = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {

            final var id = client
                    .append()
                    .withChannel(702)
                    .withRouting(101)
                    .withPayload(payload)
                    .commit();

            try (var cursor = client
                    .query()
                    .id()
                    .of(id)
                    .execute()) {
                cursor.stream()
                      .findFirst()
                      .ifPresent(message -> assertThat(message.id()).isEqualTo(id));
            }
        }

    }

    @Test
    public void should_create_message_with_headers() {

        final var payload = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {

            final var id = client
                    .append()
                    .withChannel(702)
                    .withRouting(101)
                    .withHeader("black rat", "Rattus Rattus")
                    .withHeader("black cat", "Felis Domesticus")
                    .withPayload(payload)
                    .commit();

            assertThat(id).isNotNull();

            try (var cursor = client
                    .query()
                    .id()
                    .of(id)
                    .execute()) {
                assertThat(cursor.next().headers()).contains(entry("black cat", "Felis Domesticus"));
            }
        }

    }

    @Test
    public void should_create_message_with_correlation() {

        final var payload = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {

            final var refId = client
                    .append()
                    .withChannel(702)
                    .withRouting(101)
                    .withPayload(payload)
                    .commit();

            assertThat(refId).isNotNull();
            final var id = client
                    .append()
                    .withChannel(702)
                    .withRouting(101)
                    .withCorrelationId(refId)
                    .withHeader("cats", "rule")
                    .withHeader("dogs", "drool")
                    .withPayload(payload)
                    .commit();

            try (var cursor = client
                    .query()
                    .id()
                    .of(id)
                    .execute()) {
                var fetched = cursor.next();
                assertThat(fetched.headers()).contains(entry("cats", "rule"));
                assertThat(fetched.correlationId()).isEqualTo(refId);
            }
        }

    }

}
