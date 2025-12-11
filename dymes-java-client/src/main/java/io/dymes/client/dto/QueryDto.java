// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.f4b6a3.ulid.Ulid;
import jakarta.annotation.Nullable;

import java.math.BigInteger;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueryDto(
        @JsonProperty("query_single") @Nullable String querySingle,
        @JsonProperty("query_range") @Nullable RangeQueryDto queryRange,
        @JsonProperty("query_channel") @Nullable ChannelQueryDto queryChannel,
        @JsonProperty("query_multiple") @Nullable MultiMsgQueryDto queryMultiple,
        @JsonProperty("query_correlation") @Nullable String queryCorrelation
) {

    public static QueryDto ofSingle(final Ulid id) {
        requireNonNull(id, "Message id required");
        return new QueryDto(id.toString(), null, null, null, null);
    }

    public static QueryDto ofRange(final Ulid start,
                                   final Ulid end) {
        return new QueryDto(null, RangeQueryDto.of(start, end), null, null, null);
    }

    public static QueryDto ofChannel(final BigInteger channelId,
                                     final Ulid start,
                                     final Ulid end) {
        return new QueryDto(null, null, ChannelQueryDto.of(channelId, start, end), null, null);
    }

    public static QueryDto ofMultiple(final Stream<Ulid> ids) {
        return new QueryDto(null, null, null, MultiMsgQueryDto.of(ids), null);
    }

    public static QueryDto ofCorrelation(final Ulid id) {
        requireNonNull(id, "Correlation id required");
        return new QueryDto(null, null, null, null, id.toString());
    }

}
