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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class QueryRangeNarrowingWithComparandExampleIntegrationTest {

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();


    /**
     * Assume Routing ID structure of [Scenario][Message Type][Message Type Specific], each item is 3 bits
     * <p>
     * Where Message Types
     * <ul>
     * <li>0b001 - 1 - Registration Pending Notification</li>
     * <li>0b010 - 2 - Agent Appointment</li>
     * <li>0b011 - 3 - Agent Appointment Confirmation</li>
     * <li>0b100 - 4 - Message Type X</li>
     * <li>0b101 - 5 - Message Type Y</li>
     * </ul>
     */
    @Test
    void queryRange() {
        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {
            final var then = Instant.now();
            
            // Append message
            client
                 .append()
                 .withChannel(702)
                 .withRouting(0b011_001_111)
                 .withPayload("Some Registration Pending Payload")
                 .commit();
            client
                 .append()
                 .withChannel(702)
                 .withRouting(0b011_010_100)
                 .withPayload("Some Agent Appointment Payload (DC)")
                 .commit();
            client
                 .append()
                 .withChannel(702)
                 .withRouting(0b011_010_010)
                 .withPayload("Some Agent Appointment Payload (DA)")
                 .commit();
            client
                 .append()
                 .withChannel(702)
                 .withRouting(0b011_010_001)
                 .withPayload("Some Agent Appointment Payload (MOP)")
                 .commit();
            client
                 .append()
                 .withChannel(702)
                 .withRouting(0b011_011_001)
                 .withPayload("Some Agent Appointment Confirmation Payload (MOP)")
                 .commit();

            // Fetch only Agent Appointments (Specific Message Type Filter)
            try (var cursor = client
                 .query()
                 .range()
                 .of(then, then.plus(1, ChronoUnit.SECONDS))
                 .channel(702)
                 .routing()
                 .narrowing(0b000_111_000, 0b000_010_000)
                 .execute()) {

                var content = cursor.stream()
                                    .map(Message::payload)
                                    .toList();
                assertThat(content).containsExactly("Some Agent Appointment Payload (DC)",
                     "Some Agent Appointment Payload (DA)",
                     "Some Agent Appointment Payload (MOP)");
            }
        }
    }

}
