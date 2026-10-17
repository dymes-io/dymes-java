<!--
SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors

SPDX-License-Identifier: Apache-2.0
-->

# dymes-java

Libraries to interact with Dymes using Java.

<!-- TOC -->
* [dymes-java](#dymes-java)
  * [Clone with submodules](#clone-with-submodules)
  * [Quickstart](#quickstart)
  * [Examples of interaction with Dymes](#examples-of-interaction-with-dymes)
    * [Appending a new message](#appending-a-new-message)
    * [Querying a single message](#querying-a-single-message)
    * [Querying a range of messages](#querying-a-range-of-messages)
    * [Querying messages on a channel](#querying-messages-on-a-channel)
    * [Querying a correlation chain](#querying-a-correlation-chain)
<!-- TOC -->

## Clone with submodules

```shell
git clone --recurse-submodules git@github.com:dymes-io/dymes-java-oss.git
```

## Quickstart

The following will build a local container image of `Dymes` (for integration tests) followed
by building the `Dymes Java Client`.

> If you cloned _without_ submodules, then those will be fetched during this build.

```shell
make
```

See [Dymes Usage Concepts](./dymes/doc/dymes-usage-concepts.md) before leaping into using Dymes,
and please bear the [Dymes Java Conventions](CONVENTIONS.md) in mind.


---
## Examples of interaction with Dymes

### Appending a new message

```java
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import java.util.Map;

import io.dymes.client.DymesClient;

public class AppendMessage {
    public void appendMessage() {
        final var payload = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        final var clientConfig = Config.builder(ConfigSources.create(Map.of("http-uri", "http://localhost:6510"))
                                                             .build()).build();
        try (final var client = DymesClient.of(clientConfig)) {
            final var id = client
                    .append()
                    .withChannel(702)
                    .withRouting(21)
                    .withHeader("black rat", "Rattus Rattus")
                    .withHeader("black cat", "Felis Domesticus")
                    .withPayload(payload)
                    .commit();
        }
    }
}
```

### Appending messages in a batch

Many messages in order, stored and synchronized once by the node (`POST /api/v1/messages/batch`, Dymes `7f53dd9` or
later). A message may correlate to an earlier one of the same batch by its index, since its id is assigned in the
batch. Batches above the node's limits (1,000 messages, 8 MiB) are sent in several requests.

```java
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import java.util.List;
import java.util.Map;

import com.github.f4b6a3.ulid.Ulid;
import io.dymes.client.DymesClient;
import io.dymes.client.append.BatchMessage;
import io.dymes.exceptions.UnsupportedRequestException;

public class AppendBatch {
    public List<Ulid> appendBatch() {
        final var clientConfig = Config.builder(ConfigSources.create(Map.of("http-uri", "http://localhost:6510"))
                                                             .build()).build();
        try (final var client = DymesClient.of(clientConfig)) {
            return client
                    .appendBatch()
                    .add(BatchMessage.builder().withChannel(702).withRouting(1).withPayload("job started").build())
                    .add(BatchMessage.builder().withChannel(702).withRouting(2).withPayload("record 1")
                                     .withHeader("black cat", "Felis Domesticus")
                                     .withCorrelationIndex(0)   // correlates to "job started"
                                     .build())
                    .add(BatchMessage.builder().withChannel(702).withRouting(2).withPayload("record 2")
                                     .withCorrelationIndex(0)
                                     .build())
                    .commit();                                  // the ids, in the order added
        } catch (UnsupportedRequestException e) {
            // An older or a clustered node: append the messages one at a time instead.
            throw e;
        }
    }
}
```

- **`UnsupportedRequestException`**: the node has no batch append (an older node answers 404, a clustered one 501).
- **`PartialAppendException`**: the batch failed after some messages were stored; they stay, and `appended()` lists
  them, so the caller can repair.
- **`IllegalRequestException`**: the node refused the batch; nothing was stored.

### Querying a single message

```java
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import java.util.Map;

import io.dymes.client.DymesClient;

public class QuerySingleMessage {
    public void querySingleMessage() {
        final var clientConfig = Config.builder(ConfigSources.create(Map.of("http-uri", "http://localhost:6510"))
                                                             .build()).build();
        try (final var client = DymesClient.of(clientConfig)) {
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
            // Fetch appended message
            try (var cursor = client
                    .query()
                    .id()
                    .of(id)
                    .execute()) {
                cursor.stream().findFirst().ifPresent(message -> assertThat(message.id()).isEqualTo(id));
            }
        }
    }
}

```

### Querying a range of messages

```java
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import java.util.Map;

import io.dymes.client.DymesClient;

public class QueryRange {
    public void queryRange() {
        final var clientConfig = Config.builder(ConfigSources.create(Map.of("http-uri", "http://localhost:6510"))
                                                             .build()).build();
        try (final var client = DymesClient.of(clientConfig)) {
            final var then = Instant.now();
            final var payload = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

            // Append message
            final var id = client
                    .append()
                    .withChannel(702)
                    .withRouting(21)
                    .withHeader("black rat", "Rattus Rattus")
                    .withHeader("black cat", "Felis Domesticus")
                    .withPayload(payload)
                    .commit();

            final var now = Instant.now();
            // Fetch message between then and now
            try (var cursor = client
                    .query()
                    .range()
                    .of(then, now)
                    .channel(702)
                    .execute()) {
                var actualIds = cursor.stream().map(Message::id).toList();
                assertThat(actualIds).contains(id);
            }
        }
    }
}

```


### Querying messages on a channel

```java
package io.dymes.client.examples;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import io.dymes.client.DymesClient;
import io.dymes.model.Message;
import io.dymes.testing.DymesContainer;

import java.time.Instant;

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

            final var after = Instant.now();

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

```

### Querying a correlation chain

```java
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import java.util.Map;

import io.dymes.client.DymesClient;

public class QueryCorrelation {
    public void queryCorrelationChain() {
        final var clientConfig = Config.builder(ConfigSources.create(Map.of("http-uri", "http://localhost:6510"))
                                                             .build()).build();
        try (final var client = DymesClient.of(clientConfig)) {
            final var then = Instant.now();
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
                    .withHeader("dogs", "Drool")
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
        }
    }
}

```

