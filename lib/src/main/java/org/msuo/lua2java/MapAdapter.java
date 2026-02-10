package org.msuo.lua2java;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

final class MapAdapter implements TypeAdapter {

    private final ParameterizedType pt;

    MapAdapter(ParameterizedType pt) {
        this.pt = pt;
    }

    @Override
    public ReadResult read(
        Path path,
        LuaValue luaValue,
        ErrorCollector errors
    ) {
        LuaTable t = LuaCoerce.requireTable(
            path,
            luaValue,
            errors,
            Errors.mapExpected(luaValue)
        );
        if (t == null) return ReadResult.fail();

        Type kType = pt.getActualTypeArguments()[0];
        Type vType = pt.getActualTypeArguments()[1];

        Class<?> kCls = TypeUtils.requireConcreteClassArg(
            kType,
            path,
            Errors.mapKeyMustBeConcrete(kType),
            errors
        );
        Class<?> vCls = TypeUtils.requireConcreteClassArg(
            vType,
            path,
            Errors.mapValueMustBeConcrete(vType),
            errors
        );
        if (kCls == null || vCls == null) return ReadResult.fail();

        Map<Object, Object> out = new LinkedHashMap<>();

        LuaValue k = LuaValue.NIL;
        while (true) {
            Varargs nxt = t.next(k);
            k = nxt.arg1();
            if (k.isnil()) break;

            LuaValue v = nxt.arg(2);

            Path keyPath = path.rawKey(k.tojstring());
            ReadResult keyRes = LuaDeserializer.readValue(
                keyPath,
                kCls,
                k,
                errors
            );
            if (!keyRes.ok) continue;

            Object keyObj = keyRes.value;

            Path valPath = path.mapKey(keyObj);
            ReadResult valRes = LuaDeserializer.readValue(
                valPath,
                vCls,
                v,
                errors
            );
            if (!valRes.ok) continue;

            out.put(keyObj, valRes.value);
        }

        return ReadResult.ok(out);
    }
}
