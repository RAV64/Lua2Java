package org.msuo.lua2java;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class LuaDeserializerTest {

    public static final class NonEmptyString {

        public final String value;

        public NonEmptyString(String s) {
            if (s == null || s.trim().isBlank()) {
                throw new IllegalArgumentException("must be non-empty");
            }
            this.value = s;
        }

        @Override
        public String toString() {
            // Makes Map error paths stable/readable: $.limits[foo]
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof NonEmptyString)) return false;
            NonEmptyString other = (NonEmptyString) o;
            return Objects.equals(this.value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }
    }

    public static final class PositiveInteger {

        public final Integer value;

        public PositiveInteger(Integer i) {
            if (i == null || i <= 0) {
                throw new IllegalArgumentException("must be > 0");
            }
            this.value = i;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof PositiveInteger) {
                PositiveInteger other = (PositiveInteger) o;
                return java.util.Objects.equals(this.value, other.value);
            }
            if (o instanceof Integer) {
                return java.util.Objects.equals(this.value, (Integer) o);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hashCode(this.value);
        }
    }

    public static final class PositiveDouble {

        public final Double value;

        public PositiveDouble(Double d) {
            if (d == null || d <= 0.0) {
                throw new IllegalArgumentException("must be > 0");
            }
            this.value = d;
        }
    }

    public enum Mode {
        DEV,
        PROD,
    }

    static final class NestedPort {

        public PositiveInteger port;
    }

    static final class NestedDefaultsOrOptional {

        public NonEmptyString host = new NonEmptyString("localhost");
        public Optional<NonEmptyString> user = Optional.empty();
    }

    static final class NestedHost {

        public NonEmptyString host;
    }

    static final class ItemName {

        public NonEmptyString name;
    }

    static final class ItemN {

        public PositiveInteger n;
    }

    // Leaf parsing fixtures
    static final class CfgStringLeaf {

        public NonEmptyString name;
    }

    static final class CfgIntLeaf {

        public PositiveInteger n;
    }

    static final class CfgDoubleLeaf {

        public PositiveDouble x;
    }

    static final class CfgEnum {

        public Mode mode;
    }

    static final class CfgBooleanLeaf {

        public Boolean enabled;
    }

    // Missing/defaults fixtures
    static final class CfgMissingRequired {

        public NonEmptyString name;
    }

    static final class CfgMissingOptional {

        public Optional<NonEmptyString> name;
    }

    static final class CfgOptionalHasDefaultPresent {

        public Optional<NonEmptyString> name = Optional.of(
            new NonEmptyString("x")
        );
    }

    static final class CfgDefaultValue {

        public NonEmptyString name = new NonEmptyString("default");
    }

    static final class CfgExtraKeysIgnored {

        public NonEmptyString name;
    }

    static final class CfgDefaultNestedObjectKept {

        public NestedDefaultsOrOptional db = new NestedDefaultsOrOptional();
    }

    // Nested objects fixtures
    static final class CfgNestedPort {

        public NestedPort db;
    }

    static final class CfgNestedDefaultsOrOptional {

        public NestedDefaultsOrOptional db;
    }

    static final class CfgEmptyTableButNestedHasRequiredField {

        public NestedPort db;
    }

    public static final class NoNoArgNested {

        public NonEmptyString x;

        public NoNoArgNested(NonEmptyString x) {
            this.x = x;
        }
    }

    static final class CfgBadNestedNoNoArg {

        public NoNoArgNested bad;
    }

    static final class CfgNestedProvidedAsString {

        public NestedPort db;
    }

    // Optionals fixtures
    static final class CfgOptionalOfComplex {

        public Optional<NestedHost> db;
    }

    static final class CfgOptionalLeafBadValue {

        public Optional<PositiveInteger> n;
    }

    // Collections/maps fixtures
    static final class CfgListOfLeaf {

        public List<NonEmptyString> tags;
    }

    static final class CfgSetOfLeaf {

        public Set<NonEmptyString> tags;
    }

    static final class CfgMapOfLeaf {

        public Map<NonEmptyString, PositiveInteger> limits;
    }

    static final class CfgListOfComplex {

        public List<ItemName> items;
    }

    static final class CfgMapOfComplex {

        public Map<NonEmptyString, ItemN> items;
    }

    static final class CfgMissingRequiredList {

        public List<NonEmptyString> tags;
    }

    static final class CfgMissingRequiredMap {

        public Map<NonEmptyString, PositiveInteger> limits;
    }

    static final class CfgDefaultListKept {

        public List<NonEmptyString> tags = new ArrayList<>(
            Arrays.asList(new NonEmptyString("d"))
        );
    }

    static final class CfgDefaultMapKept {

        public Map<NonEmptyString, PositiveInteger> limits =
            new LinkedHashMap<>();

        public CfgDefaultMapKept() {
            limits.put(new NonEmptyString("x"), new PositiveInteger(1));
        }
    }

    static final class CfgListElementWrongType {

        public List<NonEmptyString> tags;
    }

    static final class CfgMapHasBadEntry {

        public Map<NonEmptyString, PositiveInteger> limits;
    }

    static final class CfgNestedGenericsBadInMap {

        public Map<NonEmptyString, List<NonEmptyString>> bad;
    }

    // Error aggregation fixtures
    static final class CfgCollectAllErrors {

        public NonEmptyString a;
        public PositiveInteger b;
    }

    // Reflection edge fixtures
    static class BaseCfg {

        public NonEmptyString base;
    }

    static final class DerivedCfg extends BaseCfg {

        public PositiveInteger child;
    }

    static final class CfgPrimitiveFieldNotSupported {

        public int n;
    }

    static final class CfgRootIsComplex {

        public NonEmptyString name;
    }

    // “generic library” fixtures
    static final class RootA {

        public NonEmptyString x;
    }

    static final class RootB {

        public PositiveInteger y;
    }

    private static <T> T ok(String lua, Class<T> cls) {
        return LuaDeserializer.deserialize(lua, cls);
    }

    private static ConfigDeserializationException fails(
        String lua,
        Class<?> cls
    ) {
        return assertThrows(ConfigDeserializationException.class, () ->
            LuaDeserializer.deserialize(lua, cls)
        );
    }

    private static void assertSingleError(
        ConfigDeserializationException ex,
        String path,
        String contains
    ) {
        assertEquals(1, ex.getErrors().size(), "Expected exactly 1 error");
        assertEquals(path, ex.getErrors().get(0).getPath());
        assertTrue(
            ex.getErrors().get(0).getMessage().contains(contains),
            "Expected error message to contain '" +
                contains +
                "' but was: " +
                ex.getErrors().get(0).getMessage()
        );
    }

    private static void assertErrorPaths(
        ConfigDeserializationException ex,
        String... expectedPaths
    ) {
        Set<String> actual = new HashSet<>();
        for (int i = 0; i < ex.getErrors().size(); i++) {
            actual.add(ex.getErrors().get(i).getPath());
        }
        for (String p : expectedPaths) {
            assertTrue(actual.contains(p), "Missing expected error path: " + p);
        }
    }

    @Nested
    class LeafParsing {

        @Test
        void stringLeaf_usesStringConstructor() {
            CfgStringLeaf cfg = ok(
                "return { name = 'ok' }",
                CfgStringLeaf.class
            );
            assertEquals("ok", cfg.name.value);
        }

        @Test
        void stringLeaf_validationFailure_isReported() {
            ConfigDeserializationException ex = fails(
                "return { name = '' }",
                CfgStringLeaf.class
            );
            assertSingleError(ex, "$.name", "must be non-empty");
        }

        @Test
        void intLeaf_usesIntegerConstructor() {
            CfgIntLeaf cfg = ok("return { n = 3 }", CfgIntLeaf.class);
            assertEquals(Integer.valueOf(3), cfg.n.value);
        }

        @Test
        void intLeaf_validationFailure_isReported() {
            ConfigDeserializationException ex = fails(
                "return { n = 0 }",
                CfgIntLeaf.class
            );
            assertSingleError(ex, "$.n", "must be > 0");
        }

        @Test
        void floatFromLua_supported_whenLeafHasDoubleConstructor() {
            CfgDoubleLeaf cfg = ok("return { x = 1.25 }", CfgDoubleLeaf.class);
            assertEquals(Double.valueOf(1.25), cfg.x.value);
        }

        @Test
        void integerProvidedToDoubleLeaf_isNotAutoCoerced() {
            ConfigDeserializationException ex = fails(
                "return { x = 1 }",
                CfgDoubleLeaf.class
            );
            assertSingleError(ex, "$.x", "accepting java.lang.Integer");
        }

        @Test
        void enum_supported_fromString() {
            CfgEnum cfg = ok("return { mode = 'PROD' }", CfgEnum.class);
            assertEquals(Mode.PROD, cfg.mode);
        }

        @Test
        void enum_unknownValue_fails() {
            ConfigDeserializationException ex = fails(
                "return { mode = 'NOPE' }",
                CfgEnum.class
            );
            assertSingleError(ex, "$.mode", "Unknown enum value");
        }

        @Test
        void booleanLeaf_supported_onlyForBooleanTarget() {
            CfgBooleanLeaf cfg = ok(
                "return { enabled = true }",
                CfgBooleanLeaf.class
            );
            assertEquals(Boolean.TRUE, cfg.enabled);
        }
    }

    @Nested
    class MissingAndDefaults {

        @Test
        void missingRequired_withoutDefault_fails() {
            ConfigDeserializationException ex = fails(
                "return { }",
                CfgMissingRequired.class
            );
            assertSingleError(ex, "$.name", "Missing required field");
        }

        @Test
        void missingOptional_defaultsToEmpty() {
            CfgMissingOptional cfg = ok("return { }", CfgMissingOptional.class);
            assertEquals(Optional.empty(), cfg.name);
        }

        @Test
        void optionalDefaultPresent_isKeptWhenKeyMissing() {
            CfgOptionalHasDefaultPresent cfg = ok(
                "return { }",
                CfgOptionalHasDefaultPresent.class
            );
            assertTrue(cfg.name.isPresent());
            assertEquals("x", cfg.name.get().value);
        }

        @Test
        void defaultValue_isKeptWhenKeyMissing() {
            CfgDefaultValue cfg = ok("return { }", CfgDefaultValue.class);
            assertEquals("default", cfg.name.value);
        }

        @Test
        void extraKeys_inLuaTable_areIgnored() {
            CfgExtraKeysIgnored cfg = ok(
                "return { name = 'ok', extra = 123, other = { a = 1 } }",
                CfgExtraKeysIgnored.class
            );
            assertEquals("ok", cfg.name.value);
        }

        @Test
        void defaultNestedObject_isKeptWhenKeyMissing() {
            CfgDefaultNestedObjectKept cfg = ok(
                "return { }",
                CfgDefaultNestedObjectKept.class
            );

            assertNotNull(cfg.db);
            assertEquals("localhost", cfg.db.host.value);
            assertEquals(Optional.empty(), cfg.db.user);
        }
    }

    @Nested
    class NestedObjects {

        @Test
        void nestedObject_fromTable_usesNoArgAndSetsFields() {
            CfgNestedPort cfg = ok(
                "return { db = { port = 5432 } }",
                CfgNestedPort.class
            );
            assertEquals(Integer.valueOf(5432), cfg.db.port.value);
        }

        @Test
        void emptyTableForComplex_isEnoughWhenFieldsDefaultOrOptional() {
            CfgNestedDefaultsOrOptional cfg = ok(
                "return { db = { } }",
                CfgNestedDefaultsOrOptional.class
            );
            assertEquals("localhost", cfg.db.host.value);
            assertEquals(Optional.empty(), cfg.db.user);
        }

        @Test
        void emptyTableForNestedWithRequiredFields_reportsMissingField() {
            ConfigDeserializationException ex = fails(
                "return { db = { } }",
                CfgEmptyTableButNestedHasRequiredField.class
            );
            assertSingleError(ex, "$.db.port", "Missing required field");
        }

        @Test
        void ifNestedObjectCannotInstantiate_doNotRecurseIntoIt() {
            ConfigDeserializationException ex = fails(
                "return { bad = { x = '' } }",
                CfgBadNestedNoNoArg.class
            );
            assertSingleError(ex, "$.bad", "No no-arg constructor");
        }

        @Test
        void nestedObjectProvidedAsPrimitive_failsViaMissingConstructor() {
            ConfigDeserializationException ex = fails(
                "return { db = 'nope' }",
                CfgNestedProvidedAsString.class
            );
            assertSingleError(ex, "$.db", "No 1-arg constructor");
        }
    }

    @Nested
    class Optionals {

        @Test
        void optionalOfComplex_supported() {
            CfgOptionalOfComplex cfg = ok(
                "return { db = { host = 'x' } }",
                CfgOptionalOfComplex.class
            );

            assertTrue(cfg.db.isPresent());
            assertEquals("x", cfg.db.get().host.value);
        }

        @Test
        void optionalLeaf_badValue_reportsError() {
            ConfigDeserializationException ex = fails(
                "return { n = 0 }",
                CfgOptionalLeafBadValue.class
            );

            assertSingleError(ex, "$.n", "must be > 0");
        }
    }

    @Nested
    class CollectionsAndMaps {

        @Test
        void listOfLeafTypes_supported() {
            CfgListOfLeaf cfg = ok(
                "return { tags = { 'a', 'b' } }",
                CfgListOfLeaf.class
            );
            assertEquals(2, cfg.tags.size());
            assertEquals("a", cfg.tags.get(0).value);
        }

        @Test
        void setOfLeafTypes_supported_andDeduplicates() {
            CfgSetOfLeaf cfg = ok(
                "return { tags = { 'a', 'b', 'a' } }",
                CfgSetOfLeaf.class
            );
            assertEquals(2, cfg.tags.size());
        }

        @Test
        void mapOfLeafTypes_supported() {
            CfgMapOfLeaf cfg = ok(
                "return { limits = { foo = 1, bar = 2 } }",
                CfgMapOfLeaf.class
            );
            assertEquals(2, cfg.limits.size());
        }

        @Test
        void mapKeyWrongType_reportsError_atKeyPathWithBraces() {
            ConfigDeserializationException ex = fails(
                "return { limits = { [1] = 1 } }",
                CfgMapOfLeaf.class
            );
            assertSingleError(ex, "$.limits{1}", "accepting java.lang.Integer");
        }

        @Test
        void listCanContainComplexTypes() {
            CfgListOfComplex cfg = ok(
                "return { items = { { name = 'a' }, { name = 'b' } } }",
                CfgListOfComplex.class
            );
            assertEquals(2, cfg.items.size());
            assertEquals("b", cfg.items.get(1).name.value);
        }

        @Test
        void mapCanContainComplexTypes() {
            CfgMapOfComplex cfg = ok(
                "return { items = { x = { n = 1 }, y = { n = 2 } } }",
                CfgMapOfComplex.class
            );
            assertEquals(2, cfg.items.size());
        }

        @Test
        void missingRequiredList_withoutDefault_fails() {
            ConfigDeserializationException ex = fails(
                "return { }",
                CfgMissingRequiredList.class
            );
            assertSingleError(ex, "$.tags", "Missing required field");
        }

        @Test
        void missingRequiredMap_withoutDefault_fails() {
            ConfigDeserializationException ex = fails(
                "return { }",
                CfgMissingRequiredMap.class
            );
            assertSingleError(ex, "$.limits", "Missing required field");
        }

        @Test
        void defaultList_isKeptWhenKeyMissing() {
            CfgDefaultListKept cfg = ok("return { }", CfgDefaultListKept.class);
            assertEquals(1, cfg.tags.size());
            assertEquals("d", cfg.tags.get(0).value);
        }

        @Test
        void defaultMap_isKeptWhenKeyMissing() {
            CfgDefaultMapKept cfg = ok("return { }", CfgDefaultMapKept.class);
            assertEquals(1, cfg.limits.size());
        }

        @Test
        void listElementWrongType_reportsError_atElementPath() {
            ConfigDeserializationException ex = fails(
                "return { tags = { 1 } }",
                CfgListElementWrongType.class
            );
            assertSingleError(ex, "$.tags[1]", "accepting java.lang.Integer");
        }

        @Test
        void mapBadEntry_reportsError() {
            ConfigDeserializationException ex = fails(
                "return { limits = { ok = 1, bad = 0 } }",
                CfgMapHasBadEntry.class
            );

            assertEquals(1, ex.getErrors().size());
            assertTrue(ex.getErrors().get(0).getPath().startsWith("$.limits["));
            assertTrue(
                ex.getErrors().get(0).getMessage().contains("must be > 0")
            );
        }

        @Test
        void nestedGenericsInMap_areRejected() {
            ConfigDeserializationException ex = fails(
                "return { bad = { foo = { 'a' } } }",
                CfgNestedGenericsBadInMap.class
            );
            assertSingleError(ex, "$.bad", "no nested generics");
        }
    }

    @Nested
    class ErrorAggregation {

        @Test
        void collectAllErrors_continueAfterFailures() {
            ConfigDeserializationException ex = fails(
                "return { a = '', b = 0 }",
                CfgCollectAllErrors.class
            );

            assertEquals(2, ex.getErrors().size());
            assertErrorPaths(ex, "$.a", "$.b");
        }
    }

    @Nested
    class ReflectionEdgeCases {

        @Test
        void inheritedFields_areDeserialized() {
            DerivedCfg cfg = ok(
                "return { base = 'x', child = 1 }",
                DerivedCfg.class
            );
            assertEquals("x", cfg.base.value);
            assertEquals(Integer.valueOf(1), cfg.child.value);
        }

        @Test
        void primitiveFieldTypes_areRejected() {
            ConfigDeserializationException ex = fails(
                "return { n = 1 }",
                CfgPrimitiveFieldNotSupported.class
            );
            assertSingleError(
                ex,
                "$.n",
                "Primitive field types are not supported"
            );
        }

        @Test
        void rootNotATable_forComplexConfig_failsAtRootPath() {
            ConfigDeserializationException ex = fails(
                "return 'nope'",
                CfgRootIsComplex.class
            );
            assertSingleError(ex, "$", "No 1-arg constructor");
        }
    }

    @Nested
    class GenericLibrarySanity {

        @Test
        void notHardcodedToOneRootType() {
            RootA a = ok("return { x = 'ok' }", RootA.class);
            RootB b = ok("return { y = 1 }", RootB.class);

            assertEquals("ok", a.x.value);
            assertEquals(Integer.valueOf(1), b.y.value);
        }

        @Test
        void iTest() {
            TestMe fixture = ok(
                "return { x = 'eks', ints = { 1, 2, 3 }, innie = { b = 7 }, onnie = { c = 'k' } }",
                TestMe.class
            );

            assertEquals("eks", fixture.x.toString());
            assertTrue(fixture.ints.contains(new PositiveInteger(1)));
            assertTrue(fixture.ints.contains(new PositiveInteger(2)));
            assertTrue(fixture.ints.contains(new PositiveInteger(3)));
            assertEquals("a", fixture.innie.a);
            assertEquals(7, fixture.innie.b);
            assertEquals("k", fixture.onnie.get().c);
            assertEquals(5, fixture.onnie.get().d);
        }
    }

    static class TestMe {

        NonEmptyString x;
        List<PositiveInteger> ints = List.of();
        InnerTest innie;
        Optional<OInnerTest> onnie;

        static class InnerTest {

            String a = "a";
            Integer b = 3;
        }

        static class OInnerTest {

            String c = "c";
            Integer d = 5;
        }
    }

    static final class CfgOptionalLeafNoDefault {

        public Optional<PositiveInteger> n;
    }

    static final class CfgOptionalLeafWithDefaultPresent {

        public Optional<NonEmptyString> name = Optional.of(
            new NonEmptyString("default")
        );
    }

    static final class CfgOptionalLeafWithDefaultEmpty {

        public Optional<NonEmptyString> name = Optional.empty();
    }

    static final class CfgOptionalComplexWithDefaultPresent {

        public Optional<TestMe.OInnerTest> onnie = Optional.of(
            new TestMe.OInnerTest()
        );
    }

    static final class CfgOptionalComplexNoDefault {

        public Optional<TestMe.OInnerTest> onnie;
    }

    static final class CfgOptionalComplexInnerFieldMutation {

        public Optional<TestMe.OInnerTest> onnie = Optional.of(
            new TestMe.OInnerTest()
        );
    }

    public static final class NoNoArgNested2 {

        public NonEmptyString x;

        public NoNoArgNested2(NonEmptyString x) {
            this.x = x;
        }
    }

    static final class CfgOptionalBadInnerNoNoArg {

        public Optional<NoNoArgNested2> bad;
    }

    @Nested
    class OptionalSemantics {

        @Test
        void missingOptional_withoutDefault_defaultsToEmpty() {
            CfgOptionalLeafNoDefault cfg = ok(
                "return { }",
                CfgOptionalLeafNoDefault.class
            );
            assertEquals(Optional.empty(), cfg.n);
        }

        @Test
        void optionalComplex_providedNil_isIndistinguishableFromMissing_andKeepsDefaultPresent() {
            CfgOptionalComplexWithDefaultPresent cfg = ok(
                "return { onnie = nil }",
                CfgOptionalComplexWithDefaultPresent.class
            );

            // Same reason as above: `onnie = nil` deletes the key, so the default remains.
            assertTrue(cfg.onnie.isPresent());
            assertEquals("c", cfg.onnie.get().c);
            assertEquals(Integer.valueOf(5), cfg.onnie.get().d);
        }

        @Test
        void missingOptional_keepsDefaultPresent() {
            CfgOptionalLeafWithDefaultPresent cfg = ok(
                "return { }",
                CfgOptionalLeafWithDefaultPresent.class
            );
            assertTrue(cfg.name.isPresent());
            assertEquals("default", cfg.name.get().value);
        }

        @Test
        void missingOptional_keepsDefaultEmpty() {
            CfgOptionalLeafWithDefaultEmpty cfg = ok(
                "return { }",
                CfgOptionalLeafWithDefaultEmpty.class
            );
            assertEquals(Optional.empty(), cfg.name);
        }

        @Test
        void providedValidOptionalLeaf_parsesPresent() {
            CfgOptionalLeafNoDefault cfg = ok(
                "return { n = 3 }",
                CfgOptionalLeafNoDefault.class
            );
            assertTrue(cfg.n.isPresent());
            assertEquals(Integer.valueOf(3), cfg.n.get().value);
        }

        @Test
        void providedInvalidOptionalLeaf_fails_andDoesNotOverwriteDefaultPresent() {
            ConfigDeserializationException ex = fails(
                "return { name = '' }",
                CfgOptionalLeafWithDefaultPresent.class
            );

            assertSingleError(ex, "$.name", "must be non-empty");

            // Also ensure we didn't silently overwrite to Optional.empty() or similar:
            // (We can't read cfg because deserialize threw, but the key property is:
            //  - an error is raised
            //  - it is at $.name
            //  - message is from constructor validation)
        }

        @Test
        void providedInvalidOptionalLeaf_fails_atOptionalPath() {
            ConfigDeserializationException ex = fails(
                "return { n = 0 }",
                CfgOptionalLeafNoDefault.class
            );
            assertSingleError(ex, "$.n", "must be > 0");
        }

        @Test
        void optionalComplex_missing_withoutDefault_defaultsToEmpty() {
            CfgOptionalComplexNoDefault cfg = ok(
                "return { }",
                CfgOptionalComplexNoDefault.class
            );
            assertEquals(Optional.empty(), cfg.onnie);
        }

        @Test
        void optionalComplex_missing_keepsDefaultPresent() {
            CfgOptionalComplexWithDefaultPresent cfg = ok(
                "return { }",
                CfgOptionalComplexWithDefaultPresent.class
            );

            assertTrue(cfg.onnie.isPresent());
            // Default values from TestMe.OInnerTest should remain
            assertEquals("c", cfg.onnie.get().c);
            assertEquals(Integer.valueOf(5), cfg.onnie.get().d);
        }

        @Test
        void optionalComplex_providedTable_overridesDefault_andMutatesInnerFields() {
            CfgOptionalComplexInnerFieldMutation cfg = ok(
                "return { onnie = { c = 'k' } }",
                CfgOptionalComplexInnerFieldMutation.class
            );

            assertTrue(cfg.onnie.isPresent());
            assertEquals("k", cfg.onnie.get().c);
            // Unprovided field keeps its inner default
            assertEquals(Integer.valueOf(5), cfg.onnie.get().d);
        }

        @Test
        void providedNilOptional_isIndistinguishableFromMissing_andKeepsDefaultPresent() {
            CfgOptionalLeafWithDefaultPresent cfg = ok(
                "return { name = nil }",
                CfgOptionalLeafWithDefaultPresent.class
            );

            // In Lua, setting a table key to nil removes the key. Luaj can't
            // distinguish this from a missing key during binding.
            assertTrue(cfg.name.isPresent());
            assertEquals("default", cfg.name.get().value);
        }

        @Test
        void optionalComplex_providedEmptyTable_isPresent_andKeepsInnerDefaults() {
            CfgOptionalComplexNoDefault cfg = ok(
                "return { onnie = { } }",
                CfgOptionalComplexNoDefault.class
            );

            assertTrue(cfg.onnie.isPresent());
            assertEquals("c", cfg.onnie.get().c);
            assertEquals(Integer.valueOf(5), cfg.onnie.get().d);
        }

        @Test
        void optionalComplex_providedButInnerCannotInstantiate_fails() {
            ConfigDeserializationException ex = fails(
                "return { bad = { x = 'ok' } }",
                CfgOptionalBadInnerNoNoArg.class
            );

            // Error is produced when trying to instantiate the nested object
            assertSingleError(ex, "$.bad", "No no-arg constructor");
        }
    }
}
