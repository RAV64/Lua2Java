package org.msuo.lua2java;

import org.luaj.vm2.LuaValue;

final class PrimitiveRejectedAdapter implements TypeAdapter {

    private final Class<?> primitive;

    PrimitiveRejectedAdapter(Class<?> primitive) {
        this.primitive = primitive;
    }

    @Override
    public ReadResult read(
        Path path,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        errors.add(path, Errors.primitiveNotSupported(primitive));
        return ReadResult.fail();
    }
}
