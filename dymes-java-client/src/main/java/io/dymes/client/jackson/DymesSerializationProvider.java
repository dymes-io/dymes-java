// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.CacheProvider;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;

import java.io.IOException;

public class DymesSerializationProvider extends DefaultSerializerProvider {
    public static DymesSerializationProvider of() {
        return new DymesSerializationProvider();
    }

    public DymesSerializationProvider() { }
    public DymesSerializationProvider(final DymesSerializationProvider serializerProvider) { super(serializerProvider); }

    protected DymesSerializationProvider(final SerializerProvider serializerProvider,
                                         final SerializationConfig config,
                                         final SerializerFactory serializerFactory) {
        super(serializerProvider, config, serializerFactory);
    }

    protected DymesSerializationProvider(final DymesSerializationProvider src,
                                         final CacheProvider cacheProvider) {
        super(src, cacheProvider);
    }

    @Override
    public DefaultSerializerProvider copy() {
        if (getClass() != DymesSerializationProvider.class) {
            return super.copy();
        }
        return new DymesSerializationProvider(this);
    }

    @Override
    public DymesSerializationProvider createInstance(final SerializationConfig config,
                                                     final SerializerFactory serializerFactory) {
        return new DymesSerializationProvider(this, config, serializerFactory);
    }

    @Override
    public DefaultSerializerProvider withCaches(final CacheProvider cp) {
        return new DymesSerializationProvider(this, cp);
    }

    @Override
    public void serializeValue(final JsonGenerator jsonGenerator,
                               final Object value,
                               final JavaType rootType,
                               JsonSerializer<Object> serializer) throws IOException {
        _generator = jsonGenerator;
        if (value == null) {
            _serializeNull(jsonGenerator);
            return;
        }
        // Let's ensure types are compatible at this point
        if ((rootType != null) && !rootType.getRawClass().isAssignableFrom(value.getClass())) {
            _reportIncompatibleRootType(value, rootType);
        }
        // root value, not reached via property:
        if (serializer == null) {
            serializer = findTypedValueSerializer(rootType, true, null);
        }
        var rootName = _config.getFullRootName();
        if (rootName == null) { // not explicitly specified
            if (_config.isEnabled(SerializationFeature.WRAP_ROOT_VALUE)) {
                rootName = (rootType == null)
                        ? _config.findRootName(value.getClass())
                        : _config.findRootName(rootType);
                _serialize(jsonGenerator, value, serializer, rootName);
                return;
            }
        } else if (!rootName.isEmpty()) {
            _serialize(jsonGenerator, value, serializer, rootName);
            return;
        }
        _serialize(jsonGenerator, value, serializer);
    }

    private void _serialize(final JsonGenerator jsonGenerator,
                            final Object value,
                            final JsonSerializer<Object> serializer,
                            final PropertyName rootName) throws IOException {
        try {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeFieldName(rootName.simpleAsEncoded(_config));
            serializer.serialize(value, jsonGenerator, this);
            jsonGenerator.writeEndObject();
        } catch (final RuntimeException ex) {
            if (this.isEnabled(SerializationFeature.WRAP_EXCEPTIONS)) {
                throw _wrapAsJsonMappingException(jsonGenerator, ex);
            }
            throw ex;
        }
    }

    private void _serialize(final JsonGenerator jsonGenerator,
                            final Object value,
                            final JsonSerializer<Object> serializer) throws IOException {
        try {
            serializer.serialize(value, jsonGenerator, this);
        } catch (final RuntimeException ex) {
            if (this.isEnabled(SerializationFeature.WRAP_EXCEPTIONS)) {
                throw _wrapAsJsonMappingException(jsonGenerator, ex);
            }
            throw ex;
        }
    }

    private IOException _wrapAsJsonMappingException(final JsonGenerator jsonGenerator,
                                                    final RuntimeException ex) {
        var msg = ClassUtil.exceptionMessage(ex);
        if (msg == null) {
            msg = "[no message for "+ex.getClass().getName()+"]";
        }
        return new JsonMappingException(jsonGenerator, msg, ex);
    }
}