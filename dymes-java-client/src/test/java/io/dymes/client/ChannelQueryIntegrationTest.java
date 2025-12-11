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

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ChannelQueryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ChannelQueryIntegrationTest.class);

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    @Test
    public void should_create_and_query_message_range() {

        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {

            final var oddKVs = Map.of("even", "false", "testing", "true", "odd", "true");
            final var evenKVs = Map.of("even", "true", "testing", "true", "odd", "false");

            final long oddChannel = 1;
            final long evenChannel = 2;

            // Populate store
            var expectedIds = new ArrayList<Ulid>();
            var evenIds = new ArrayList<Ulid>();
            var oddIds = new ArrayList<Ulid>();
            var correlation_id = new Ulid(0, 0);
            final int numTestMessages = 10;
            for (int i = 0; i < numTestMessages; i++) {
                final var even = i % 2 == 0;
                correlation_id = client
                        .append()
                        .withChannel(even ? evenChannel : oddChannel)
                        .withRouting(even ? 0b0010 : 0b0001)
                        .withCorrelationId(correlation_id)
                        .withHeaders(even ? evenKVs : oddKVs)
                        .withPayload("Dymes Client Message " + i)
                        .commit();

                expectedIds.add(correlation_id);
                if (even) {
                    evenIds.add(correlation_id);
                } else {
                    oddIds.add(correlation_id);
                }
            }

            // Range query using ULIDs
            {
                logger.debug("Range query appended");
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(expectedIds);
                }
            }

            // Range query with KV filters for evens
            {
                logger.debug("Range query with KV filtering for evens");
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .keyValue("even", "true")
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(evenIds);
                }
            }

            // Range query using routing filter for evens
            {
                logger.debug("Range query with routing mask for evens");
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .routing().narrowing(0b0010)
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(evenIds);
                }
            }

            // Channel query for evens
            {
                logger.debug("Channel query for evens");
                try (var cursor = client
                        .query()
                        .channel()
                        .of(evenChannel, expectedIds.getFirst(), expectedIds.getLast())
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(evenIds);
                }
            }

            // Channel query for odds
            {
                logger.debug("Channel query for odds");
                try (var cursor = client
                        .query()
                        .channel()
                        .of(oddChannel, expectedIds.getFirst(), expectedIds.getLast())
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(oddIds);
                }
            }
        }
    }

}
