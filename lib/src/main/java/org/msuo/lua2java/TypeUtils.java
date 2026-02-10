package org.msuo.lua2java;

import java.lang.reflect.Type;

final class TypeUtils {

    static Class<?> requireConcreteClassArg(
        Type t,
        Path path,
        String errorMessage,
        ErrorCollector errors
    ) {
        if (t instanceof Class<?>) return (Class<?>) t;
        errors.add(path, errorMessage);
        return null;
    }
}
