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
public class RangeQueryIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(RangeQueryIntegrationTest.class);

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    @Test
    public void should_create_and_query_message_range() {

        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {

            final var channel = new BigInteger("702");

            final var oddKVs = Map.of("even", "false", "testing", "true", "odd", "true");
            final var evenKVs = Map.of("even", "true", "testing", "true", "odd", "false");

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
                        .withChannel(channel)
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

            // Query range using ULIDs
            {
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(expectedIds);
                }
            }

            // Query range using ULIDs with channel filter
            {
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .channel(channel)
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(expectedIds);
                }
            }

            // Query range using ULIDs with channel and routing filters
            {
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .routing().widening(0b0011)
                        .channel(channel)
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(expectedIds);
                }
            }

            // Query range using ULIDs with KV filters for evens
            {
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

            // Query range using routing filter for evens
            {
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

            // Query range using ULIDs with KV filters for odds
            {
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .withoutKeyValue("even", "true")
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(oddIds);
                }
            }

            // Query range using routing filter for odds
            {
                try (var cursor = client
                        .query()
                        .range()
                        .of(expectedIds.getFirst(), expectedIds.getLast())
                        .routing().narrowing(0b0001)
                        .execute()) {
                    var actualIds = cursor.stream().map(Message::id).toList();
                    assertThat(actualIds).containsExactlyElementsOf(oddIds);
                }
            }

        }

    }

}
