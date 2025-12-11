// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigInteger;
import java.util.List;

import static java.util.Objects.requireNonNull;

@JsonSerialize
@JsonInclude(Include.NON_NULL)
public record CreationRequestDto(
        @JsonProperty("encoded_correlation_id") String correlationId,   // encoded using Ulid toString
        @JsonProperty("channel") BigInteger channel,    // encoded using BigInteger toString
        @JsonProperty("routing") BigInteger routing,
        @JsonProperty("kv_entries") List<KvPair> kv_entries,
        @JsonProperty("encoded_msg_body") String payload) { // encoded using Base64

    @JsonSerialize
    public record KvPair(@JsonProperty("key") String key,
                         @JsonProperty("value") String value) {

        public static KvPair of(final String key,
                                final String value) {

            return new KvPair(requireNonNull(key, "Key required"),
                              requireNonNull(value, "Value required"));
        }
    }

}
