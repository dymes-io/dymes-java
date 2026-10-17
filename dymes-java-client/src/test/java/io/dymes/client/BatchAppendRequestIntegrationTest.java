// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client;

import com.github.f4b6a3.ulid.Ulid;
import io.dymes.client.append.BatchMessage;
import io.dymes.exceptions.IllegalRequestException;
import io.dymes.model.Message;
import io.dymes.testing.DymesContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@Testcontainers
public class BatchAppendRequestIntegrationTest {

    @Container
    private static final DymesContainer dymesContainer = DymesContainer.of();

    private static Message read(final DymesClient client, final Ulid id) {
        try (var cursor = client.query().id().of(id).execute()) {
            return cursor.next();
        }
    }

    private static void assertAscending(final List<Ulid> ids) {
        for (int i = 1; i < ids.size(); i++) {
            assertThat(ids.get(i)).isGreaterThan(ids.get(i - 1));
        }
    }

    @Test
    public void should_append_a_batch_in_order_correlated_by_index() {
        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {
            final var ids = client
                    .appendBatch()
                    .add(BatchMessage.builder().withChannel(702).withRouting(1).withPayload("marker").build())
                    .add(BatchMessage.builder().withChannel(702).withRouting(2).withPayload("record 1")
                                     .withHeader("black cat", "Felis Domesticus").withCorrelationIndex(0).build())
                    .add(BatchMessage.builder().withChannel(702).withRouting(2).withPayload("record 2")
                                     .withCorrelationIndex(0).build())
                    .add(BatchMessage.builder().withChannel(702).withRouting(3).withPayload("closed")
                                     .withCorrelationIndex(0).build())
                    .commit();

            assertThat(ids).hasSize(4);
            assertAscending(ids);
            assertThat(read(client, ids.get(0)).payload()).isEqualTo("marker");
            final var record = read(client, ids.get(1));
            assertThat(record.payload()).isEqualTo("record 1");
            assertThat(record.headers()).contains(entry("black cat", "Felis Domesticus"));
            assertThat(record.correlationId()).isEqualTo(ids.get(0));
            assertThat(read(client, ids.get(3)).correlationId()).isEqualTo(ids.get(0));

            // A later batch correlates to a stored message by id.
            final var later = client.appendBatch()
                                    .add(BatchMessage.builder().withChannel(702).withRouting(2).withPayload("follow-up")
                                                     .withCorrelationId(ids.get(0)).build())
                                    .commit();
            assertThat(later.get(0)).isGreaterThan(ids.get(3));
            assertThat(read(client, later.get(0)).correlationId()).isEqualTo(ids.get(0));
        }
    }

    @Test
    public void should_split_a_large_batch_and_correlate_across_requests() {
        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {
            final var batch = client.appendBatch();
            final int count = 2_500;
            for (int i = 0; i < count; i++) {
                final var message = BatchMessage.builder().withChannel(703).withRouting(1).withPayload("message " + i);
                if (i > 0) {
                    // Alternately the batch's first message (in an earlier request) and the one just before.
                    message.withCorrelationIndex(i % 2 == 0 ? 0 : i - 1);
                }
                batch.add(message.build());
            }
            final var ids = batch.commit();

            assertThat(ids).hasSize(count);
            assertAscending(ids);
            for (int i : List.of(1, 2, 999, 1_000, 1_001, 1_002, 2_498, 2_499)) {
                final var message = read(client, ids.get(i));
                assertThat(message.payload()).isEqualTo("message " + i);
                assertThat(message.correlationId()).isEqualTo(ids.get(i % 2 == 0 ? 0 : i - 1));
            }
        }
    }

    @Test
    public void should_refuse_a_batch_with_an_unknown_correlation_id() {
        try (final var client = DymesClient.of(dymesContainer.clientConfig())) {
            final var messages = new ArrayList<BatchMessage>();
            messages.add(BatchMessage.builder().withChannel(704).withRouting(1).withPayload("a").build());
            messages.add(BatchMessage.builder().withChannel(704).withRouting(1).withPayload("b")
                                     .withCorrelationId(new Ulid(1, 1)).build());
            assertThatThrownBy(() -> client.appendBatch().addAll(messages).commit())
                    .isInstanceOf(IllegalRequestException.class);
        }
    }
}
