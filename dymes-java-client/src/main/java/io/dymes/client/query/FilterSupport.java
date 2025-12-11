// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.query;

import io.dymes.client.dto.FilterDto;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class FilterSupport implements FilteredQuery {

    public static FilterSupport of() {
        return new FilterSupport();
    }

    protected final List<FilterDto> filters;

    private FilterSupport() {
        this.filters = new ArrayList<>();
    }

    public List<FilterDto> filters() {
        return filters;
    }

    @Override
    public FilteredQuery channel(final BigInteger channel) {
        requireNonNull(channel, "Channel required");
        filters.add(new FilterDto(channel, null, null));
        return this;
    }

    @Override
    public FilteredQuery channel(long channel) {
        return channel(BigInteger.valueOf(channel));
    }

    @Override
    public RoutingFilterBuilder routing() {
        return new InnerFilterBuilder();
    }

    @Override
    public FilteredQuery containsKey(String key) {
        requireNonNull(key, "Key required");
        final var kvFilter = new FilterDto.KvFilter(key, null, null);
        filters.add(new FilterDto(null, null, kvFilter));
        return this;
    }

    @Override
    public FilteredQuery keyValue(String key, String value) {
        requireNonNull(key, "Key required");
        requireNonNull(value, "Value required");
        final var kvFilter = new FilterDto.KvFilter(null, FilterDto.KvPair.of(key, value), null);
        filters.add(new FilterDto(null, null, kvFilter));
        return this;
    }

    @Override
    public FilteredQuery withoutKeyValue(String key, String value) {
        requireNonNull(key, "Key required");
        requireNonNull(value, "Value required");
        final var kvFilter = new FilterDto.KvFilter(null, null, FilterDto.KvPair.of(key, value));
        filters.add(new FilterDto(null, null, kvFilter));
        return this;
    }

    public class InnerFilterBuilder implements RoutingFilterBuilder {

        @Override
        public FilteredQuery widening(final BigInteger routingMask) {
            requireNonNull(routingMask, "Routing mask required");
            FilterSupport.this.filters.add(new FilterDto(null, FilterDto.RoutingFilter.widening(routingMask), null));
            return FilterSupport.this;
        }

        @Override
        public FilteredQuery widening(long wideningMask) {
            return widening(BigInteger.valueOf(wideningMask));
        }

        @Override
        public FilteredQuery narrowing(final BigInteger routingMask) {
            return narrowing(routingMask, routingMask);
        }

        @Override
        public FilteredQuery narrowing(long narrowingMask) {
            return narrowing(BigInteger.valueOf(narrowingMask));
        }

        @Override
        public FilteredQuery narrowing(final BigInteger routingMask, final BigInteger comparand) {
            requireNonNull(routingMask, "Routing mask required");
            requireNonNull(comparand, "Routing comparand required");
            FilterSupport.this.filters.add(
                    new FilterDto(null,
                                  FilterDto.RoutingFilter.narrowing(routingMask, comparand),
                                  null));
            return FilterSupport.this;
        }

        @Override
        public FilteredQuery narrowing(final long routingMask, final long comparand) {
            return narrowing(BigInteger.valueOf(routingMask), BigInteger.valueOf(comparand));
        }






    }
}
