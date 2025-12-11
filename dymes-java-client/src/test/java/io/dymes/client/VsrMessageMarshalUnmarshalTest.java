// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client;

import com.github.f4b6a3.ulid.Ulid;
import io.dymes.client.msg.DymesMessageReader;
import io.dymes.client.msg.DymesMessageWriter;
import io.dymes.client.vsr.msg.*;
import io.dymes.client.vsr.ops.*;
import io.dymes.model.DymesMessage;
import io.dymes.model.Message;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VsrMessageMarshalUnmarshalTest {

    /*
    Marshal Unmarshal Tests
     */
    @Test
    void testVsrRequestMarshalUnmarshal() throws IOException {
        // Create a VsrRequest with test data
        long clientId = 0x0123456789ABCDEFL;
        long requestNumber = 0x0FEDCBA987654321L;
        VsrOp testOp = createTestVsrOp();

        var originalRequest = VsrRequest.builder()
                                        .withOp(testOp)
                                        .withClientId(clientId)
                                        .withRequestNumber(requestNumber)
                                        .build();

        // Marshal to bytes
        byte[] marshaledBytes = marshalMessage(originalRequest);

        // Unmarshal back to object
        ByteArrayInputStream byteIn = new ByteArrayInputStream(marshaledBytes);
        DataInputStream dataIn = new DataInputStream(byteIn);
        VsrMessage unmarshaledMessage = VsrMessage.unmarshal(dataIn);

        // Verify it's a VsrRequest and fields match
        assertInstanceOf(VsrRequest.class, unmarshaledMessage);
        VsrRequest unmarshaledRequest = (VsrRequest) unmarshaledMessage;

        assertEquals(clientId, unmarshaledRequest.clientId());
        assertEquals(requestNumber, unmarshaledRequest.requestNumber());
        assertNotNull(unmarshaledRequest.op());
        assertEquals(testOp.opType(), unmarshaledRequest.op().opType());
        assertEquals(VsrMessageType.vsr_request, unmarshaledRequest.messageType());

        // Cast to VsrAppendOp to access message()
        assertInstanceOf(VsrAppendOp.class, unmarshaledRequest.op());
        VsrAppendOp appendOp = (VsrAppendOp) unmarshaledRequest.op();

        DymesMessage message = appendOp.message();

        // Verify headers
        assertNotNull(message.headers());
        assertEquals(2, message.headers().size());
        assertEquals("value1", message.headers().get("key1"));
        assertEquals("value2", message.headers().get("key2"));
    }

    @Test
    void testVsrReplyMarshalUnmarshal() throws IOException {
        // Create a VsrReply with test data
        long viewNumber = 42L;
        long requestNumber = 0x0FEDCBA987654321L;
        long clientId = 7L;
        VsrOpResponse testOp = createTestVsrOpResponse();

        var originalReply = VsrReply.builder().withViewNumber(viewNumber)
                                    .withRequestNumber(requestNumber)
                                    .withClientId(clientId)
                                    .withOpResponse(testOp)
                                    .build();

        // Marshal to bytes
        byte[] marshaledBytes = marshalMessage(originalReply);

        // Unmarshal back to object
        ByteArrayInputStream byteIn = new ByteArrayInputStream(marshaledBytes);
        DataInputStream dataIn = new DataInputStream(byteIn);
        VsrMessage unmarshaledMessage = VsrMessage.unmarshal(dataIn);

        // Verify it's a VsrReply and fields match
        assertInstanceOf(VsrReply.class, unmarshaledMessage);
        VsrReply unmarshaledReply = (VsrReply) unmarshaledMessage;

        assertEquals(viewNumber, unmarshaledReply.viewNumber());
        assertEquals(requestNumber, unmarshaledReply.requestNumber());
        assertEquals(clientId, unmarshaledReply.clientId());
        assertNotNull(unmarshaledReply.response());
        assertEquals(VsrMessageType.vsr_reply, unmarshaledReply.messageType());

        // Cast to VsrAppendResponse to access msgUlid()
        assertInstanceOf(VsrAppendResponse.class, unmarshaledReply.response());
        VsrAppendResponse appendResponse = (VsrAppendResponse) unmarshaledReply.response();
        assertEquals(((VsrAppendResponse) testOp).messageId(), appendResponse.messageId());
    }

    @Test
    void testMultipleRequestsInSequence() throws IOException {
        // Test marshaling multiple requests in sequence
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);

        for (int i = 1; i <= 10; i++) {
            VsrRequest request = VsrRequest.builder()
                                           .withClientId(i)
                                           .withRequestNumber((long) i * 100)
                                           .withOp(createTestVsrOp())
                                           .build();
            request.marshal(dataOut);
            dataOut.flush();
        }

        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        DataInputStream dataIn = new DataInputStream(byteIn);

        // Unmarshal and verify each request
        for (int i = 1; i <= 10; i++) {
            VsrRequest unmarshaled = (VsrRequest) VsrMessage.unmarshal(dataIn);
            assertEquals(i, unmarshaled.clientId());
            assertEquals((long) i * 100, unmarshaled.requestNumber());
        }
    }

    @Test
    void testRoundTripPreservesDataIntegrity() throws IOException {
        // Perform multiple round trips to ensure no data corruption
        VsrRequest original = VsrRequest.builder()
                                        .withClientId(0x7EDCBA9876543210L)
                                        .withRequestNumber(0x0123456789ABCDEFL)
                                        .withOp(createTestVsrOp())
                                        .build();
        assertNotNull(original);

        VsrRequest result = original;
        for (int i = 0; i < 10; i++) {
            byte[] marshaled = marshalMessage(result);
            result = (VsrRequest) VsrMessage.unmarshal(
                    new DataInputStream(new ByteArrayInputStream(marshaled)));
        }

        assertEquals(original.clientId(), result.clientId());
        assertEquals(original.requestNumber(), result.requestNumber());
    }

    @Test
    void testMessageTypeIsCorrectlyIdentified() throws IOException {
        VsrRequest request = VsrRequest.builder()
                                       .withClientId(1L)
                                       .withRequestNumber(1L)
                                       .withOp(createTestVsrOp())
                                       .build();

        VsrReply reply = VsrReply.builder()
                                 .withViewNumber(1L)
                                 .withRequestNumber(1L)
                                 .withClientId(1L)
                                 .withOpResponse(createTestVsrOpResponse())
                                 .build();

        byte[] requestBytes = marshalMessage(request);
        byte[] replyBytes = marshalMessage(reply);

        VsrMessage unmarshaledRequest = VsrMessage.unmarshal(
                new DataInputStream(new ByteArrayInputStream(requestBytes)));
        VsrMessage unmarshaledReply = VsrMessage.unmarshal(
                new DataInputStream(new ByteArrayInputStream(replyBytes)));

        assertEquals(VsrMessageType.vsr_request, unmarshaledRequest.messageType());
        assertEquals(VsrMessageType.vsr_reply, unmarshaledReply.messageType());
    }

    /*
    DymesMessage Write and Read Tests
     */
    @Test
    void testWriteAndReadSingleMessage() throws IOException {
        Ulid messageId = Ulid.fast();
        Ulid correlationId = Ulid.fast();
        BigInteger channel = BigInteger.valueOf(123);
        BigInteger routing = BigInteger.valueOf(456);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Priority", "high");
        String payload = "Test message payload";

        DymesMessage original = new DymesMessage(messageId, correlationId, channel, routing, headers, payload);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(out);
        DymesMessageWriter.write(dataOut, original);
        byte[] wireBytes = out.toByteArray();

        ByteArrayInputStream in = new ByteArrayInputStream(wireBytes);
        DymesMessage read = DymesMessageReader.readOne(in);

        assertEquals(original.id(), read.id());
        assertEquals(original.correlationId(), read.correlationId());
        assertEquals(original.channel(), read.channel());
        assertEquals(original.routing(), read.routing());
        assertEquals(original.headers(), read.headers());
        assertEquals(original.payload(), read.payload());
    }

    @Test
    void testWriteAndReadMultipleMessages() throws IOException {
        DymesMessage msg1 = createTestMessage("First message");
        DymesMessage msg2 = createTestMessage("Second message");
        DymesMessage msg3 = createTestMessage("Third message");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(out);
        DymesMessageWriter.write(dataOut, msg1);
        DymesMessageWriter.write(dataOut, msg2);
        DymesMessageWriter.write(dataOut, msg3);
        byte[] wireBytes = out.toByteArray();

        ByteArrayInputStream in = new ByteArrayInputStream(wireBytes);
        List<Message> messages = DymesMessageReader.readAll(in);

        assertEquals(3, messages.size());
        assertMessageEquals(msg1, (DymesMessage) messages.get(0));
        assertMessageEquals(msg2, (DymesMessage) messages.get(1));
        assertMessageEquals(msg3, (DymesMessage) messages.get(2));
    }

    @Test
    void testEmptyHeaders() throws IOException {
        DymesMessage message = new DymesMessage(
                Ulid.fast(),
                Ulid.fast(),
                BigInteger.ZERO,
                BigInteger.ZERO,
                Map.of(),
                "No headers"
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(out);
        DymesMessageWriter.write(dataOut, message);

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        DymesMessage read = DymesMessageReader.readOne(in);

        assertTrue(read.headers().isEmpty());
        assertEquals(message.payload(), read.payload());
    }

    /*
    Reading in and out from temp file tests
     */
    @Test
    void testVsrRequestEndToEndWithFile() throws IOException {
        // Build VsrRequest from scratch
        Map<String, String> headers = Map.of(
                "Content-Type", "application/json",
                "X-Client", "test-client"
        );

        DymesMessage dymesMessage = new DymesMessage(
                Ulid.fast(),
                Ulid.fast(),
                BigInteger.valueOf(123),
                BigInteger.valueOf(456),
                headers,
                "Test payload for end-to-end"
        );

        VsrOp appendOp = DymesVsrAppendOp.of(dymesMessage);

        VsrRequest originalRequest = VsrRequest.builder()
                                               .withClientId(0x1234567890ABCDEFL)
                                               .withRequestNumber(0x7EDCBA9876543210L)
                                               .withOp(appendOp)
                                               .build();

        // Marshal to temporary file
        Path tempFile = Files.createTempFile("vsr-request-", ".bin");
        try {
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(tempFile.toFile()))) {
                originalRequest.marshal(out);
            }

            // Unmarshal from file
            VsrRequest unmarshaledRequest;
            try (DataInputStream in = new DataInputStream(
                    new FileInputStream(tempFile.toFile()))) {
                unmarshaledRequest = (VsrRequest) VsrMessage.unmarshal(in);
            }

            // Verify contents
            assertEquals(originalRequest.clientId(), unmarshaledRequest.clientId());
            assertEquals(originalRequest.requestNumber(), unmarshaledRequest.requestNumber());
            assertEquals(VsrOpType.append, unmarshaledRequest.op().opType());

            VsrAppendOp unmarshaledOp = (VsrAppendOp) unmarshaledRequest.op();
            DymesMessage unmarshaledMessage = unmarshaledOp.message();

            assertEquals(dymesMessage.id(), unmarshaledMessage.id());
            assertEquals(dymesMessage.correlationId(), unmarshaledMessage.correlationId());
            assertEquals(dymesMessage.channel(), unmarshaledMessage.channel());
            assertEquals(dymesMessage.routing(), unmarshaledMessage.routing());
            assertEquals(dymesMessage.headers(), unmarshaledMessage.headers());
            assertEquals(dymesMessage.payload(), unmarshaledMessage.payload());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testVsrReplyEndToEndWithFile() throws IOException {
        // Build VsrReply from scratch
        Ulid messageId = Ulid.fast();
        VsrOpResponse appendResponse = VsrAppendResponse.of(messageId);

        VsrReply originalReply = VsrReply.builder()
                                         .withViewNumber(42L)
                                         .withRequestNumber(0xABCDEF1234567890L)
                                         .withClientId(999L)
                                         .withOpResponse(appendResponse)
                                         .build();

        // Marshal to temporary file
        Path tempFile = Files.createTempFile("vsr-reply-", ".bin");
        try {
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(tempFile.toFile()))) {
                originalReply.marshal(out);
            }

            // Unmarshal from file
            VsrReply unmarshaledReply;
            try (DataInputStream in = new DataInputStream(
                    new FileInputStream(tempFile.toFile()))) {
                unmarshaledReply = (VsrReply) VsrMessage.unmarshal(in);
            }

            // Verify contents
            assertEquals(originalReply.viewNumber(), unmarshaledReply.viewNumber());
            assertEquals(originalReply.requestNumber(), unmarshaledReply.requestNumber());
            assertEquals(originalReply.clientId(), unmarshaledReply.clientId());

            VsrAppendResponse unmarshaledResponse = (VsrAppendResponse) unmarshaledReply.response();
            assertEquals(messageId, unmarshaledResponse.messageId());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testMultipleMessagesEndToEndWithFile() throws IOException {
        // Build multiple requests
        List<VsrRequest> originalRequests = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            DymesMessage msg = new DymesMessage(
                    Ulid.fast(),
                    Ulid.fast(),
                    BigInteger.valueOf(i * 100),
                    BigInteger.valueOf(i * 200),
                    Map.of("index", String.valueOf(i)),
                    "Payload " + i
            );

            VsrRequest request = VsrRequest.builder()
                                           .withClientId(i)
                                           .withRequestNumber(i * 1000L)
                                           .withOp(DymesVsrAppendOp.of(msg))
                                           .build();

            originalRequests.add(request);
        }

        // Marshal all to temporary file
        Path tempFile = Files.createTempFile("vsr-multi-", ".bin");
        try {
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(tempFile.toFile()))) {
                for (VsrRequest request : originalRequests) {
                    request.marshal(out);
                }
            }

            // Unmarshal all from file
            List<VsrRequest> unmarshaledRequests = new ArrayList<>();
            try (DataInputStream in = new DataInputStream(
                    new FileInputStream(tempFile.toFile()))) {
                while (in.available() > 0) {
                    unmarshaledRequests.add((VsrRequest) VsrMessage.unmarshal(in));
                }
            }

            // Verify count and contents
            assertEquals(originalRequests.size(), unmarshaledRequests.size());

            for (int i = 0; i < originalRequests.size(); i++) {
                VsrRequest original = originalRequests.get(i);
                VsrRequest unmarshaled = unmarshaledRequests.get(i);

                assertEquals(original.clientId(), unmarshaled.clientId());
                assertEquals(original.requestNumber(), unmarshaled.requestNumber());
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void testMixedRequestReplyEndToEndWithFile() throws IOException {
        // Build mixed requests and replies
        VsrRequest request1 = VsrRequest.builder()
                                        .withClientId(1L)
                                        .withRequestNumber(100L)
                                        .withOp(DymesVsrAppendOp.of(createTestMessage("Request 1")))
                                        .build();

        VsrReply reply1 = VsrReply.builder()
                                  .withViewNumber(1L)
                                  .withRequestNumber(100L)
                                  .withClientId(1L)
                                  .withOpResponse(VsrAppendResponse.of(Ulid.fast()))
                                  .build();

        VsrRequest request2 = VsrRequest.builder()
                                        .withClientId(2L)
                                        .withRequestNumber(200L)
                                        .withOp(DymesVsrAppendOp.of(createTestMessage("Request 2")))
                                        .build();

        // Marshal to temporary file
        Path tempFile = Files.createTempFile("vsr-mixed-", ".bin");
        try {
            try (DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(tempFile.toFile()))) {
                request1.marshal(out);
                reply1.marshal(out);
                request2.marshal(out);
            }

            // Unmarshal and verify order and types
            try (DataInputStream in = new DataInputStream(
                    new FileInputStream(tempFile.toFile()))) {
                VsrMessage msg1 = VsrMessage.unmarshal(in);
                VsrMessage msg2 = VsrMessage.unmarshal(in);
                VsrMessage msg3 = VsrMessage.unmarshal(in);

                assertInstanceOf(VsrRequest.class, msg1);
                assertInstanceOf(VsrReply.class, msg2);
                assertInstanceOf(VsrRequest.class, msg3);

                assertEquals(1L, ((VsrRequest) msg1).clientId());
                assertEquals(1L, ((VsrReply) msg2).viewNumber());
                assertEquals(2L, ((VsrRequest) msg3).clientId());
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // Helper methods
    private VsrOp createTestVsrOp() {
        // Create test DymesMessage
        Map<String, String> headers = Map.of("key1", "value1", "key2", "value2");
        DymesMessage testMessage = new DymesMessage(
                Ulid.from(UUID.randomUUID()),
                Ulid.from(UUID.randomUUID()),
                BigInteger.ONE,
                BigInteger.TEN,
                headers,
                "Test payload"
        );

        return DymesVsrAppendOp.of(testMessage);
    }

    private VsrOpResponse createTestVsrOpResponse() {
        Ulid testUlid = Ulid.from(UUID.randomUUID());
        return VsrAppendResponse.of(testUlid);
    }

    private byte[] marshalMessage(VsrMessage message) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteOut);
        message.marshal(dataOut);
        dataOut.flush();
        return byteOut.toByteArray();
    }

    private DymesMessage createTestMessage(String payload) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Test", "true");
        headers.put("Content-Length", String.valueOf(payload.length()));

        return new DymesMessage(
                Ulid.fast(),
                Ulid.fast(),
                BigInteger.valueOf(100),
                BigInteger.valueOf(200),
                headers,
                payload
        );
    }

    private void assertMessageEquals(DymesMessage expected, DymesMessage actual) {
        assertEquals(expected.id(), actual.id());
        assertEquals(expected.correlationId(), actual.correlationId());
        assertEquals(expected.channel(), actual.channel());
        assertEquals(expected.routing(), actual.routing());
        assertEquals(expected.headers(), actual.headers());
        assertEquals(expected.payload(), actual.payload());
    }
}
