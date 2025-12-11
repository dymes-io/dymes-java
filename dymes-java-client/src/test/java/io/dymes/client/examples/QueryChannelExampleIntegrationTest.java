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
class QueryChannelExampleIntegrationTest {

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    @Test
    public void queryRange() {
        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {

            final var before = Instant.now();

            // Append messages

            final var musicRadioChannel = 105L;
            final var talkRadioChannel = 702L;
            final var room101 = 101L;

            final var talkRoutingUpdates = 0b0001;
            final var talkRoutingFeatures = 0b0010;

            client.append()
                  .withChannel(talkRadioChannel)
                  .withRouting(talkRoutingUpdates)
                  .withPayload("News")
                  .commit();

            client.append()
                  .withChannel(room101)
                  .withRouting(0)
                  .withPayload("Consigned to /dev/null")
                  .commit();

            client.append()
                  .withChannel(musicRadioChannel)
                  .withRouting(0)
                  .withPayload("Song 1")
                  .commit();

            client.append()
                  .withChannel(room101)
                  .withRouting(0)
                  .withPayload("Garbage disposed of")
                  .commit();

            client.append()
                  .withChannel(talkRadioChannel)
                  .withRouting(talkRoutingUpdates)
                  .withPayload("Traffic report")
                  .commit();

            client.append()
                  .withChannel(musicRadioChannel)
                  .withRouting(0)
                  .withPayload("Song 2")
                  .commit();

            client.append()
                  .withChannel(talkRadioChannel)
                  .withRouting(talkRoutingFeatures)
                  .withPayload("Engineering in Action")
                  .commit();

            final var after = before.plus(1, ChronoUnit.SECONDS);

            // Fetch songs
            try (var cursor = client
                    .query()
                    .channel()
                    .of(musicRadioChannel, before, after)
                    .execute()) {
                var content = cursor.stream().map(Message::payload).toList();
                assertThat(content).containsExactly("Song 1", "Song 2");
            }

            // Fetch talks
            try (var cursor = client
                    .query()
                    .channel()
                    .of(talkRadioChannel, before, after)
                    .execute()) {
                var content = cursor.stream().map(Message::payload).toList();
                assertThat(content).containsExactly("News", "Traffic report", "Engineering in Action");
            }

            // Fetch features
            try (var cursor = client
                    .query()
                    .channel()
                    .of(talkRadioChannel, before, after)
                    .routing()
                    .narrowing(talkRoutingFeatures)
                    .execute()) {
                var content = cursor.stream().map(Message::payload).toList();
                assertThat(content).containsExactly("Engineering in Action");
            }

        }
    }

}
