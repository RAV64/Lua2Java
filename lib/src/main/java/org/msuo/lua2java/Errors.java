package org.msuo.lua2java;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.luaj.vm2.LuaValue;

final class Errors {

    private Errors() {}

    static String unsupportedType(Type t) {
        return "Unsupported Type: " + t;
    }

    static String unsupportedParameterized(ParameterizedType pt) {
        return "Unsupported parameterized type: " + pt;
    }

    static String unsupportedParameterizedRaw(Type raw) {
        return "Unsupported parameterized raw type: " + raw;
    }

    static String primitiveNotSupported(Class<?> prim) {
        return "Primitive field types are not supported: " + prim.getName();
    }

    static String enumExpectedString(LuaValue got) {
        return "Enum expects string name, got: " + got.typename();
    }

    static String enumUnknown(Class<?> enumClass, String name) {
        return ("Unknown enum value '" + name + "' for " + enumClass.getName());
    }

    static String expectedScalar(LuaValue got) {
        return (
            "Expected primitive (string/number/bool), got: " + got.typename()
        );
    }

    static String mapExpected(LuaValue got) {
        return "Expected table for Map, got: " + got.typename();
    }

    static String collectionExpected(Class<?> raw, LuaValue got) {
        return (
            "Expected table/array for " +
            raw.getSimpleName() +
            ", got: " +
            got.typename()
        );
    }

    static String optionalInnerMustBeConcrete(Type inner) {
        return (
            "Optional inner type must be a concrete class (no nested generics). Got: " +
            inner
        );
    }

    static String collectionElementMustBeConcrete(Type elem) {
        return (
            "Collection element type must be a concrete class (no nested generics). Got: " +
            elem
        );
    }

    static String mapKeyMustBeConcrete(Type kType) {
        return (
            "Map key type must be a concrete class (no nested generics). Got: " +
            kType
        );
    }

    static String mapValueMustBeConcrete(Type vType) {
        return (
            "Map value type must be a concrete class (no nested generics). Got: " +
            vType
        );
    }

    static String missingRequiredField() {
        return "Missing required field (no default value).";
    }

    static String noOneArgCtor(Class<?> target, Class<?> argType) {
        return (
            "No 1-arg constructor on " +
            target.getName() +
            " accepting " +
            argType.getName()
        );
    }

    static String ctorRejected(Class<?> target, Throwable cause) {
        return (
            "Value rejected by " +
            target.getSimpleName() +
            " constructor: " +
            cause.getMessage()
        );
    }

    static String ctorCallFailed(Class<?> target, Exception e) {
        return (
            "Failed calling constructor for " +
            target.getName() +
            ": " +
            e.getMessage()
        );
    }

    static String noNoArgCtor(Class<?> cls) {
        return (
            "No no-arg constructor for nested object type: " + cls.getName()
        );
    }

    static String ctorFailed(Class<?> cls, Throwable cause) {
        return (
            "Constructor failed for " +
            cls.getName() +
            ": " +
            cause.getMessage()
        );
    }

    static String instantiateFailed(Class<?> cls, Exception e) {
        return (
            "Failed to instantiate " + cls.getName() + ": " + e.getMessage()
        );
    }
}
