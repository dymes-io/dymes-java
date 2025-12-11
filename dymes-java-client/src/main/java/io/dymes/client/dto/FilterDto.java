// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.math.BigInteger;

import static java.util.Objects.requireNonNull;

@JsonSerialize
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilterDto(
        @JsonProperty("channel_filter") BigInteger channelFilter,
        @JsonProperty("routing_filter") RoutingFilter routingFilter,
        @JsonProperty("kv_filter") KvFilter kvFilter
) {

    public enum RoutingVariant {
        /**
         * Traditional masking - widening
         */
        widening,
        /**
         * Constrictive masking - narrowing
         */
        narrowing
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RoutingFilter(
            @JsonProperty("variant") RoutingVariant variant,
            @JsonProperty("routing_mask") BigInteger routingMask,
            @JsonProperty("routing_cmp") BigInteger routingComparand
    ) {
        public static RoutingFilter widening(final BigInteger routingMask) {
            return new RoutingFilter(RoutingVariant.widening, requireNonNull(routingMask, "Routing mask required"), BigInteger.ZERO);
        }

        public static RoutingFilter narrowing(final BigInteger routingMask) {
            return narrowing(routingMask, routingMask);
        }

        public static RoutingFilter narrowing(final BigInteger routingMask, BigInteger routingComparand) {
            return new RoutingFilter(RoutingVariant.narrowing,
                                     requireNonNull(routingMask, "Routing mask required"),
                                     requireNonNull(routingComparand, "Routing comparand required"));
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KvFilter(
            @JsonProperty("has") String keyFilter,
            @JsonProperty("eql") KvPair eql,
            @JsonProperty("neq") KvPair neq
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KvPair(
            @JsonProperty("key") String key,
            @JsonProperty("value") String value
    ) {
        public static KvPair of(String key, String value) {
            return new KvPair(key, value);
        }
    }

}
