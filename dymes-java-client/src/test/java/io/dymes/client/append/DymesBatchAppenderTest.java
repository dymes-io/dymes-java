// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.append;

import com.github.f4b6a3.ulid.Ulid;
import io.dymes.exceptions.PartialAppendException;
import io.dymes.exceptions.UnsupportedRequestException;
import io.helidon.webclient.api.WebClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How the batch appender reads a node's answers, against a stand-in node.
 */
class DymesBatchAppenderTest {

    private ServerSocket server;
    private Thread serving;

    /** A stand-in node answering every request with {@code status} and {@code body}. */
    private WebClient node(final int status, final String body) throws IOException {
        server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        serving = Thread.ofVirtual().start(() -> {
            while (!server.isClosed()) {
                try (var socket = server.accept()) {
                    final var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
                    int length = 0;
                    for (String line = in.readLine(); line != null && !line.isEmpty(); line = in.readLine()) {
                        if (line.toLowerCase().startsWith("content-length:")) {
                            length = Integer.parseInt(line.substring(15).trim());
                        }
                    }
                    in.skip(length);
                    final var bytes = body.getBytes(UTF_8);
                    final var out = socket.getOutputStream();
                    out.write(("HTTP/1.1 " + status + " X\r\nContent-Type: application/json\r\nContent-Length: "
                            + bytes.length + "\r\nConnection: close\r\n\r\n").getBytes(UTF_8));
                    out.write(bytes);
                    out.flush();
                } catch (IOException e) {
                    // closed
                }
            }
        });
        return WebClient.builder().baseUri("http://127.0.0.1:" + server.getLocalPort()).build();
    }

    @AfterEach
    void stop() throws IOException {
        if (server != null) {
            server.close();
        }
    }

    private static BatchMessage message(final String payload) {
        return BatchMessage.builder().withChannel(5).withRouting(1).withPayload(payload).build();
    }

    @Test
    void anOlderNodeHasNoBatchAppend() throws IOException {
        final var appender = DymesBatchAppender.of(node(404, "")).add(message("a"));
        assertThatThrownBy(appender::commit).isInstanceOf(UnsupportedRequestException.class);
    }

    @Test
    void aClusteredNodeHasNoBatchAppend() throws IOException {
        final var appender = DymesBatchAppender.of(node(501, "UnsupportedOperation")).add(message("a"));
        assertThatThrownBy(appender::commit).isInstanceOf(UnsupportedRequestException.class);
    }

    @Test
    void aStorageFailurePartWaySaysWhatWasStored() throws IOException {
        final var stored = new Ulid(1_700_000_000_000L, 7);
        final var appender = DymesBatchAppender
                .of(node(500, "{\"error\":\"FailedToStore\",\"ids\":[\"" + stored + "\"]}"))
                .add(message("a"))
                .add(message("b"));
        assertThatThrownBy(appender::commit)
                .isInstanceOfSatisfying(PartialAppendException.class,
                                        e -> assertThat(e.appended()).containsExactly(stored));
    }

    @Test
    void aCorrelationIndexMustPointAtAnEarlierMessage() {
        final var appender = DymesBatchAppender.of(WebClient.builder().baseUri("http://127.0.0.1:1").build())
                                               .add(message("a"));
        assertThatThrownBy(() -> appender.add(BatchMessage.builder().withChannel(5).withRouting(1).withPayload("b")
                                                          .withCorrelationIndex(1).build()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMessageCorrelatesOneWayAtMost() {
        assertThatThrownBy(() -> BatchMessage.builder().withChannel(5).withRouting(1).withPayload("b")
                                             .withCorrelationIndex(0).withCorrelationId(new Ulid(1, 1)).build())
                .isInstanceOf(IllegalStateException.class);
    }
}
