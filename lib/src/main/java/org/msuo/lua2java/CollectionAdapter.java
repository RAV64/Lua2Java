package org.msuo.lua2java;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

final class CollectionAdapter implements TypeAdapter {

    private final ParameterizedType pt;
    private final Class<?> raw;

    CollectionAdapter(ParameterizedType pt, Class<?> raw) {
        this.pt = pt;
        this.raw = raw;
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
            Errors.collectionExpected(raw, luaValue)
        );
        if (t == null) return ReadResult.fail();

        Class<?> elemCls = TypeUtils.requireConcreteClassArg(
            pt.getActualTypeArguments()[0],
            path,
            Errors.collectionElementMustBeConcrete(
                pt.getActualTypeArguments()[0]
            ),
            errors
        );
        if (elemCls == null) return ReadResult.fail();

        final boolean wantSet = Set.class.isAssignableFrom(raw);
        final Collection<Object> out = wantSet
            ? new LinkedHashSet<>()
            : new ArrayList<>();

        int n = t.length();
        for (int i = 1; i <= n; i++) {
            LuaValue v = t.get(i);
            Path elemPath = path.index(i);
            ReadResult rr = LuaDeserializer.readValue(
                elemPath,
                elemCls,
                v,
                errors
            );
            if (rr.ok) out.add(rr.value);
        }

        return ReadResult.ok(out);
    }
}
