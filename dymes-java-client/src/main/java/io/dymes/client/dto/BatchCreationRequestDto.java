// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigInteger;
import java.util.List;

/**
 * A batch creation request ({@code POST /api/v1/messages/batch}).
 */
@JsonSerialize
public record BatchCreationRequestDto(@JsonProperty("messages") List<Entry> messages) {

    /** One message: a creation request, plus an optional index of an earlier entry to correlate to. */
    @JsonSerialize
    @JsonInclude(Include.NON_NULL)
    public record Entry(
            @JsonProperty("encoded_correlation_id") String correlationId,   // encoded using Ulid toString
            @JsonProperty("correlation_index") Integer correlationIndex,
            @JsonProperty("channel") BigInteger channel,
            @JsonProperty("routing") BigInteger routing,
            @JsonProperty("kv_entries") List<CreationRequestDto.KvPair> kvEntries,
            @JsonProperty("encoded_msg_body") String payload) { // encoded using Base64
    }
}
