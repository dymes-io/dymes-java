// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.f4b6a3.ulid.Ulid;

import static java.util.Objects.requireNonNull;

@JsonSerialize
public record RangeQueryDto(
        @JsonProperty("range_start") String rangeStart,
        @JsonProperty("range_end") String rangeEnd
) {

    public static RangeQueryDto of(final Ulid rangeStart,
                                   final Ulid rangeEnd) {
        return new RangeQueryDto(requireNonNull(rangeStart, "Query range start required").toString(),
                                 requireNonNull(rangeEnd, "Query range end required").toString());
    }

}
