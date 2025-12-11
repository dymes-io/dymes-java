// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.examples;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.dymes.client.DymesClient;
import io.dymes.model.Message;
import io.dymes.testing.DymesContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class QueryCorrelationExampleIntegrationTest {

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    @Test
    public void queryCorrelationChain() {
        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {
            final var payload = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

            // Commit messages
            final var head = client
                    .append()
                    .withChannel(702)
                    .withRouting(0b0001)
                    .withHeader("black rat", "Rattus Rattus")
                    .withHeader("black cat", "Felis Domesticus")
                    .withPayload(payload)
                    .commit();

            final var middle = client
                    .append()
                    .withChannel(702)
                    .withRouting(0b00011)
                    .withPayload(payload)
                    .withCorrelationId(head)
                    .commit();

            final var tail = client
                    .append()
                    .withChannel(702)
                    .withRouting(0b00101)
                    .withHeader("cats", "Rule")
                    .withHeader("dog", "Drool")
                    .withPayload(payload)
                    .withCorrelationId(middle)
                    .commit();

            // Fetch correlation chain
            try (var cursor = client
                    .query()
                    .correlation()
                    .of(tail)
                    .execute()) {
                var actualIds = cursor.stream().map(Message::id).toList();
                assertThat(actualIds.getFirst()).isEqualTo(tail);
                assertThat(actualIds.getLast()).isEqualTo(head);
            }

            // Fetch correlation sub-chain
            try (var cursor = client
                    .query()
                    .correlation()
                    .of(middle)
                    .execute()) {
                var actualIds = cursor.stream().map(Message::id).toList();
                assertThat(actualIds.getFirst()).isEqualTo(middle);
                assertThat(actualIds.getLast()).isEqualTo(head);
            }
        }
    }

}
