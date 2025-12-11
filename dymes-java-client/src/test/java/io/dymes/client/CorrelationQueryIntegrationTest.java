// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client;

import com.github.f4b6a3.ulid.Ulid;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.dymes.model.Message;
import io.dymes.testing.DymesContainer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class CorrelationQueryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationQueryIntegrationTest.class);

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    @Test
    public void testCorrelationChainTraversal() {

        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {

            final var channel = new BigInteger("702");

            final var oddKVs = Map.of("even", "false", "testing", "true", "odd", "true");
            final var evenKVs = Map.of("even", "true", "testing", "true", "odd", "false");

            // Populate store
            var evenIds = new ArrayList<Ulid>();
            var oddIds = new ArrayList<Ulid>();
            final int numTestMessages = 1000;
            var prevEvenCorrId = new Ulid(0, 0);
            var prevOddCorrId = new Ulid(0, 0);
            for (int i = 0; i < numTestMessages; i++) {
                final var even = i % 2 == 0;
                final var msg_id = client
                        .append()
                        .withChannel(channel)
                        .withRouting(even ? 0b0010 : 0b0001)
                        .withCorrelationId(even ? prevEvenCorrId : prevOddCorrId)
                        .withHeaders(even ? evenKVs : oddKVs)
                        .withPayload("Dymes Client Message " + i)
                        .commit();

                if (even) {
                    evenIds.add(msg_id);
                    prevEvenCorrId = msg_id;
                } else {
                    oddIds.add(msg_id);
                    prevOddCorrId = msg_id;
                }
            }

            // Query even correlation chain
            {
                final var startedAt = System.currentTimeMillis();
                try (var cursor = client
                        .query()
                        .correlation()
                        .of(evenIds.getLast())
                        .execute()) {
                    var actualIds = cursor
                            .stream()
                            .map(Message::id)
                            .sorted()
                            .toList();
                    assertThat(actualIds).containsExactlyElementsOf(evenIds);
                }
                logger.debug("Traversed even correlation chain in {} ms", System.currentTimeMillis() - startedAt);
            }

            // Query odd correlation chain
            {
                final var startedAt = System.currentTimeMillis();
                try (var cursor = client
                        .query()
                        .correlation()
                        .of(oddIds.getLast())
                        .execute()) {
                    var actualIds = cursor
                            .stream()
                            .map(Message::id)
                            .sorted()
                            .toList();
                    assertThat(actualIds).containsExactlyElementsOf(oddIds);
                }
                logger.debug("Traversed odd correlation chain in {} ms", System.currentTimeMillis() - startedAt);
            }

        }

    }

}
