// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.jackson;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.javamoney.moneta.CurrencyUnitBuilder;
import org.threeten.extra.Interval;
import org.zalando.jackson.datatype.money.MoneyModule;

import javax.money.Monetary;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Dymes {@link ObjectMapper Object Mapper} factory for Jackson.
 * <p>
 * Provides support for:
 * <ul>
 *     <li>Java Time (JSR310)</li>
 *     <li>Java Time intervals (JSR310-extras)</li>
 *     <li>{@code Optional} properties</li>
 * </ul>
 * <p>
 */
public abstract class DymesObjectMapperFactory {

    public static ObjectMapper of() {
        return of(new ObjectMapper());
    }

    public static ObjectMapper of(final JsonFactory jsonFactory) {
        return of(new ObjectMapper(jsonFactory));
    }

    public static ObjectMapper of(final ObjectMapper objectMapper) {
        // Optionals
        objectMapper.registerModule(new Jdk8Module());

        // Parameter names
        objectMapper.registerModule(new ParameterNamesModule());

        // JSR310 (Java Time)
        objectMapper.registerModule(new JavaTimeModule());

        // JSR310  (Java Time extras)
        SimpleModule threeTenExtrasModule = new SimpleModule();
        threeTenExtrasModule.addSerializer(Interval.class, IntervalSerializer.of());
        threeTenExtrasModule.addDeserializer(Interval.class, IntervalDeserializer.of());
        objectMapper.registerModule(threeTenExtrasModule);

        // Money
        objectMapper.registerModule(new MoneyModule());
        if (!Monetary.isCurrencyAvailable("kWh")) {
            CurrencyUnitBuilder.of("kWh", "platform-common")
                               .setDefaultFractionDigits(3)
                               .build(true);
        }

        // CRM compatibility
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
        simpleModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ISO_LOCAL_TIME));
        objectMapper.registerModule(simpleModule);

        // For Helidon exceptions to not be wrapped by Jackson unintentionally
        objectMapper.disable(SerializationFeature.WRAP_EXCEPTIONS);
        objectMapper.setSerializerProvider(DymesSerializationProvider.of());

        return objectMapper;
    }

    /**
     * Interval {@link JsonSerializer} serializer.
     * Writes interval {@code start} and {@code end} in order.
     */
    private static final class IntervalSerializer extends JsonSerializer<Interval> {
        static IntervalSerializer of() {
            return new IntervalSerializer();
        }

        @Override
        public void serialize(final Interval value,
                              final JsonGenerator gen,
                              final SerializerProvider serializers) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("start");
            InstantSerializer.INSTANCE.serialize(value.getStart(), gen, serializers);
            gen.writeFieldName("end");
            InstantSerializer.INSTANCE.serialize(value.getEnd(), gen, serializers);
            gen.writeEndObject();
        }
    }

    /**
     * Interval {@link JsonDeserializer} deserializer.
     * <p>
     * Reads {@code start} and {@code end} in order, ignores additional fields.
     */
    private static final class IntervalDeserializer extends JsonDeserializer<Interval> {
        static IntervalDeserializer of() {
            return new IntervalDeserializer();
        }

        @Override
        public Interval deserialize(final JsonParser jsonParser,
                                    final DeserializationContext ctx) throws IOException {

            if (!jsonParser.isExpectedStartObjectToken()) {
                throw new JsonParseException("Expected Interval Object");
            }

            jsonParser.nextToken();
            if (jsonParser.currentTokenId() != JsonTokenId.ID_FIELD_NAME
                    || !"start".equals(jsonParser.currentName())) {
                throw new JsonParseException("Expected Interval 'start' field");
            }

            jsonParser.nextToken();
            final var start = InstantDeserializer.INSTANT.deserialize(jsonParser, ctx);

            jsonParser.nextToken();
            if (jsonParser.currentTokenId() != JsonTokenId.ID_FIELD_NAME
                    || !"end".equals(jsonParser.currentName())) {
                throw new JsonParseException("Expected Interval 'end' field");
            }

            jsonParser.nextToken();
            final var end = InstantDeserializer.INSTANT.deserialize(jsonParser, ctx);

            jsonParser.nextToken();
            if (jsonParser.currentTokenId() != JsonTokenId.ID_END_OBJECT) {
                throw new JsonParseException("Unexpected additional content in Interval");
            }

            return Interval.of(start, end);
        }
    }

}