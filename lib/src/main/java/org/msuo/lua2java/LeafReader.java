package org.msuo.lua2java;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.luaj.vm2.LuaValue;

final class LeafReader {

    private LeafReader() {}

    static ReadResult readLeaf(
        Path path,
        Class<?> target,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        LuaScalar scalar = LuaCoerce.scalarOrError(path, luaValue, errors);
        if (scalar == null) return ReadResult.fail();

        if (target.isAssignableFrom(scalar.boxedType)) {
            return ReadResult.ok(scalar.value);
        }

        Constructor<?> ctor = findOneArgCtor(target, scalar.boxedType);
        if (ctor == null) {
            errors.add(path, Errors.noOneArgCtor(target, scalar.boxedType));
            return ReadResult.fail();
        }

        try {
            ctor.setAccessible(true);
            return ReadResult.ok(ctor.newInstance(scalar.value));
        } catch (InvocationTargetException e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            errors.add(path, Errors.ctorRejected(target, cause));
            return ReadResult.fail();
        } catch (ReflectiveOperationException e) {
            errors.add(path, Errors.ctorCallFailed(target, e));
            return ReadResult.fail();
        }
    }

    private static Constructor<?> findOneArgCtor(
        Class<?> target,
        Class<?> paramType
    ) {
        Constructor<?>[] ctors = target.getDeclaredConstructors();
        for (int i = 0; i < ctors.length; i++) {
            Constructor<?> c = ctors[i];
            if (c.getParameterCount() != 1) continue;
            if (c.getParameterTypes()[0] == paramType) return c;
        }
        return null;
    }
}
