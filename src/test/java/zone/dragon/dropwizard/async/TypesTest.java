/*
 * Copyright 2019 Bryan Harclerode
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package zone.dragon.dropwizard.async;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Darth Android
 * @date 7/18/2019
 */
@DisplayName("Types")
public class TypesTest {

    private static class RawTypeFixture<T extends Number> {

        public static final Type PRIMITIVE_TYPE;
        public static final Type CLASS_TYPE;
        public static final Type PARAMETERIZED_TYPE;
        public static final Type GENERIC_ARRAY_TYPE;
        public static final Type TYPE_VARIABLE;
        public static final Type UPPER_BOUND_WILDCARD_TYPE;
        public static final Type LOWER_BOUND_WILDCARD_TYPE;

        static {
            try {
                PRIMITIVE_TYPE = RawTypeFixture.class.getDeclaredField("primitive").getGenericType();
                CLASS_TYPE = RawTypeFixture.class.getDeclaredField("clazz").getGenericType();
                PARAMETERIZED_TYPE = RawTypeFixture.class.getDeclaredField("parameterizedType").getGenericType();
                GENERIC_ARRAY_TYPE = RawTypeFixture.class.getDeclaredField("genericArrayType").getGenericType();
                TYPE_VARIABLE = RawTypeFixture.class.getDeclaredField("typeVariable").getGenericType();
                ParameterizedType wildcardTypes = (ParameterizedType) RawTypeFixture.class.getDeclaredField("wildcardType").getGenericType();
                UPPER_BOUND_WILDCARD_TYPE = wildcardTypes.getActualTypeArguments()[0];
                LOWER_BOUND_WILDCARD_TYPE = wildcardTypes.getActualTypeArguments()[1];
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Failed to init test fixture", e);
            }
        }

        int primitive;
        Integer clazz;
        List<Integer> parameterizedType;
        List<Integer>[] genericArrayType;
        public T typeVariable;
        Map<? extends String, ? super Integer> wildcardType;

    }

    private static class BoundTypeFixture extends GenericTypeFixture<Long> {


        public static final Type INHERITED_TYPE;

        public static final Type SIMPLE_BOUND_TYPE;

        public static final Type NESTED_BOUND_TYPE;

        private static final Type INHERITED_NESTED_TYPE;

        static {
            try {
                INHERITED_TYPE = BoundTypeFixture.class.getField("inheritedBoundType").getGenericType();
                INHERITED_NESTED_TYPE = BoundTypeFixture.class.getField("inheritedNestedBoundType").getGenericType();
                SIMPLE_BOUND_TYPE = BoundTypeFixture.class.getDeclaredField("simpleBoundType").getGenericType();
                NESTED_BOUND_TYPE = BoundTypeFixture.class.getDeclaredField("nestedBoundType").getGenericType();
            } catch (NoSuchFieldException e) {
                throw new RuntimeException("Failed to init test fixture", e);
            }
        }

        GenericTypeFixture<Double> simpleBoundType;
        GenericTypeFixture<GenericTypeFixture<Float>> nestedBoundType;


    }

    private static class GenericTypeFixture<T> {
        public T inheritedBoundType;
        public GenericTypeFixture<GenericTypeFixture<T>> inheritedNestedBoundType;
    }

    @Nested
    @DisplayName("rawType(Type)")
    class RawType {

        @Test
        @DisplayName("with primitive type")
        void withPrimitiveType() {
            assertThat(Types.rawType(RawTypeFixture.PRIMITIVE_TYPE)).isEqualTo(int.class);
        }

        @Test
        @DisplayName("with class type")
        void withClassType() {
            assertThat(Types.rawType(RawTypeFixture.CLASS_TYPE)).isEqualTo(Integer.class);
        }

        @Test
        @DisplayName("with parameterized type")
        void withParameterized() {
            assertThat(Types.rawType(RawTypeFixture.PARAMETERIZED_TYPE)).isEqualTo(List.class);
        }

        @Test
        @DisplayName("with generic array type")
        void withGenericArray() {
            assertThat(Types.rawType(RawTypeFixture.GENERIC_ARRAY_TYPE)).isEqualTo(List[].class);
        }

        @Test
        @DisplayName("with type variable")
        void withTypeVariable() {
            assertThat(Types.rawType(RawTypeFixture.TYPE_VARIABLE)).isEqualTo(Number.class);
        }

        @Test
        @DisplayName("with wildcard with upper bound")
        void withWildcardWithUpperBound() {
            assertThat(Types.rawType(RawTypeFixture.UPPER_BOUND_WILDCARD_TYPE)).isEqualTo(String.class);
        }

        @Test
        @DisplayName("with wildcard with lower bound")
        void withWildcardWithLowerBound() {
            assertThat(Types.rawType(RawTypeFixture.LOWER_BOUND_WILDCARD_TYPE)).isEqualTo(Object.class);
        }
    }

    @Nested
    @DisplayName("resolveType(Type,Class,int)")
    class ResolveType {


        @Test
        @DisplayName("with bound type")
        void withBoundType() {
            Type result = Types.resolveReifiedType(BoundTypeFixture.class, BoundTypeFixture.SIMPLE_BOUND_TYPE, GenericTypeFixture.class, 0);
            assertThat(result).isEqualTo(Double.class);
        }

        @Test
        @DisplayName("with nested bound type")
        void withNestedBoundType() {
            Type result = Types.resolveReifiedType(BoundTypeFixture.class, BoundTypeFixture.NESTED_BOUND_TYPE, GenericTypeFixture.class, 0);
            assertThat(result).isEqualTo(Types.parameterized(TypesTest.class, GenericTypeFixture.class, Float.class));
        }


        @Test
        void withNonGenericType() {
            Type result = Types.resolveReifiedType(BoundTypeFixture.class, BoundTypeFixture.INHERITED_TYPE, GenericTypeFixture.class, 0);
            assertThat(result).isEqualTo(null);
        }

        @Test
        void withNestedInheritedType() {
            Type result = Types.resolveReifiedType(BoundTypeFixture.class, BoundTypeFixture.INHERITED_NESTED_TYPE, GenericTypeFixture.class, 0);
            assertThat(result).isEqualTo(Types.parameterized(TypesTest.class, GenericTypeFixture.class, Long.class));
        }

    }

}
