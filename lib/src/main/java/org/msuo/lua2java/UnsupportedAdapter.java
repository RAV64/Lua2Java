package org.msuo.lua2java;

import org.luaj.vm2.LuaValue;

final class UnsupportedAdapter implements TypeAdapter {

    private final String message;

    UnsupportedAdapter(String message) {
        this.message = message;
    }

    @Override
    public ReadResult read(
        Path path,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        errors.add(path, message);
        return ReadResult.fail();
    }
}
