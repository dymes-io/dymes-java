// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.f4b6a3.ulid.Ulid;

import java.math.BigInteger;

import static java.util.Objects.requireNonNull;

@JsonSerialize
public record ChannelQueryDto(
        @JsonProperty("channel_id") BigInteger channelId,
        @JsonProperty("range_start") String rangeStart,
        @JsonProperty("range_end") String rangeEnd
) {

    public static ChannelQueryDto of(final BigInteger channelId,
                                     final Ulid rangeStart,
                                     final Ulid rangeEnd) {
        return new ChannelQueryDto(requireNonNull(channelId, "Channel identifier required"),
                                   requireNonNull(rangeStart, "Query range start required").toString(),
                                   requireNonNull(rangeEnd, "Query range end required").toString());
    }

}
