package org.msuo.lua2java;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

final class LuaCoerce {

    static LuaTable requireTable(
        Path path,
        LuaValue luaValue,
        ErrorCollector errors,
        String errorMessage
    ) {
        if (!luaValue.istable()) {
            errors.add(path, errorMessage);
            return null;
        }
        return luaValue.checktable();
    }

    static LuaScalar scalarOrError(
        Path path,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        LuaScalar s = LuaScalar.from(luaValue);
        if (s == null) errors.add(path, Errors.expectedScalar(luaValue));
        return s;
    }
}
