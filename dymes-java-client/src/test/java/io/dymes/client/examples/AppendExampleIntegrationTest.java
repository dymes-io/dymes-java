// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.examples;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.dymes.client.DymesClient;
import io.dymes.testing.DymesContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class AppendExampleIntegrationTest {

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    @Test
    public void appendMessage() {
        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {
            final var payload = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
            // Append message
            final var id = client
                    .append()
                    .withChannel(702)
                    .withRouting(21)
                    .withRouting(101)
                    .withHeader("black rat", "Rattus Rattus")
                    .withHeader("black cat", "Felis Domesticus")
                    .withPayload(payload)
                    .commit();
            assertThat(id).isNotNull();
        }
    }
}


