package org.msuo.lua2java;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

public final class LuaDeserializer {

    private LuaDeserializer() {}

    public static <T> T deserialize(File luaFile, Class<T> configClass)
        throws IOException {
        return deserialize(luaFile.toPath(), configClass);
    }

    public static <T> T deserialize(
        java.nio.file.Path luaFile,
        Class<T> configClass
    ) throws IOException {
        return deserialize(luaFile, StandardCharsets.UTF_8, configClass);
    }

    public static <T> T deserialize(
        java.nio.file.Path luaFile,
        Charset charset,
        Class<T> configClass
    ) throws IOException {
        String lua = Files.readString(luaFile, charset);
        return deserialize(lua, configClass);
    }

    public static <T> T deserialize(String lua, Class<T> configClass) {
        Globals g = JsePlatform.standardGlobals();
        LuaValue root = g.load(lua).call();
        return deserialize(root, configClass);
    }

    public static <T> T deserialize(LuaValue lua, Class<T> configClass) {
        ErrorCollector errors = new ErrorCollector();
        ReadResult rr = readValue(Path.root(), configClass, lua, errors);

        if (errors.hasErrors()) {
            throw new ConfigDeserializationException(errors.asList());
        }

        @SuppressWarnings("unchecked")
        T cast = (T) rr.value;
        return cast;
    }

    protected static ReadResult readValue(
        Path path,
        Type targetType,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        return adapterFor(targetType).read(path, luaValue, errors);
    }

    static TypeAdapter adapterFor(Type targetType) {
        if (targetType instanceof ParameterizedType) {
            return adapterForParameterized((ParameterizedType) targetType);
        }
        if (targetType instanceof Class<?>) {
            return adapterForClass((Class<?>) targetType);
        }
        return new UnsupportedAdapter(Errors.unsupportedType(targetType));
    }

    private static TypeAdapter adapterForParameterized(ParameterizedType pt) {
        Type raw = pt.getRawType();
        if (!(raw instanceof Class<?>)) {
            return new UnsupportedAdapter(
                Errors.unsupportedParameterizedRaw(raw)
            );
        }

        Class<?> rawClass = (Class<?>) raw;

        if (rawClass == Optional.class) return new OptionalAdapter(pt);
        if (Map.class.isAssignableFrom(rawClass)) return new MapAdapter(pt);
        if (Collection.class.isAssignableFrom(rawClass)) {
            return new CollectionAdapter(pt, rawClass);
        }

        return new UnsupportedAdapter(Errors.unsupportedParameterized(pt));
    }

    private static TypeAdapter adapterForClass(Class<?> cls) {
        if (cls.isPrimitive()) return new PrimitiveRejectedAdapter(cls);
        if (cls.isEnum()) return new EnumAdapter(cls);
        return new ClassAdapter(cls);
    }
}
