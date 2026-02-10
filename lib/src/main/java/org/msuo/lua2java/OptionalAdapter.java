package org.msuo.lua2java;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import org.luaj.vm2.LuaValue;

final class OptionalAdapter implements TypeAdapter {

    private final ParameterizedType pt;

    OptionalAdapter(ParameterizedType pt) {
        this.pt = pt;
    }

    @Override
    public ReadResult missing(Path path, ErrorCollector errors) {
        // Key missing and no default on field => Optional.empty()
        return ReadResult.ok(Optional.empty());
    }

    @Override
    public ReadResult read(
        Path path,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        // Provided key with nil-like value => Optional.empty()
        if (luaValue.isnil()) return ReadResult.ok(Optional.empty());

        Class<?> inner = TypeUtils.requireConcreteClassArg(
            pt.getActualTypeArguments()[0],
            path,
            Errors.optionalInnerMustBeConcrete(pt.getActualTypeArguments()[0]),
            errors
        );
        if (inner == null) return ReadResult.ok(Optional.empty());

        ReadResult innerRes = LuaDeserializer.readValue(
            path,
            inner,
            luaValue,
            errors
        );
        if (!innerRes.ok) return ReadResult.fail();
        return ReadResult.ok(Optional.of(innerRes.value));
    }
}
