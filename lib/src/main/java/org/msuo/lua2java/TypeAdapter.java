package org.msuo.lua2java;

import org.luaj.vm2.LuaValue;

interface TypeAdapter {
    ReadResult read(Path path, LuaValue luaValue, ErrorCollector errors);

    default ReadResult missing(Path path, ErrorCollector errors) {
        errors.add(path, Errors.missingRequiredField());
        return ReadResult.fail();
    }
}
