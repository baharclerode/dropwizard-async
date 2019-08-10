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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.Value;

/**
 * @author Darth Android
 * @date 7/18/2019
 */
class Types {

    public static boolean isSubtypeOf(@NonNull Type subType, @NonNull Class<?> superType) {
        return superType.isAssignableFrom(rawType(subType));
    }

    public static ParameterizedType parameterized(Type owner, @NonNull Type rawType, @NonNull Type... typeArguments) {
        return new ParameterizedTypeImpl(owner, rawType, typeArguments);
    }

    public static Type arrayOf(@NonNull Type componentType) {
        if (componentType == int.class) {
            return int[].class;
        } else if (componentType == byte.class) {
            return byte[].class;
        } else if (componentType == short.class) {
            return short[].class;
        } else if (componentType == long.class) {
            return long[].class;
        } else if (componentType == char.class) {
            return char[].class;
        } else if (componentType == double.class) {
            return double[].class;
        } else if (componentType == float.class) {
            return float[].class;
        } else if (componentType == boolean.class) {
            return boolean[].class;
        } else if (componentType == void.class) {
            throw new IllegalArgumentException("Can't create a void[] array");
        } else if (componentType == Void.class) {
            throw new IllegalArgumentException("Can't create a Void[] array");
        } else if (componentType instanceof Class) {
            return Array.newInstance((Class) componentType, 0).getClass();
        } else {
            return new GenericArrayTypeImpl(componentType);
        }
    }

    public static Class<?> rawType(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            return rawType(((ParameterizedType) type).getRawType());
        } else if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds == null || upperBounds.length == 0) {
                return Object.class;
            } else {
                return rawType(upperBounds[0]);
            }
        } else if (type instanceof GenericArrayType) {
            return (Class) arrayOf(rawType(((GenericArrayType) type).getGenericComponentType()));
        } else if (type instanceof TypeVariable) {
            Type[] upperBounds = ((TypeVariable) type).getBounds();
            if (upperBounds == null || upperBounds.length == 0) {
                return Object.class;
            } else {
                return rawType(upperBounds[0]);
            }
        } else {
            return Object.class;
        }
    }

    public static Type resolveReifiedType(Type ownerType, Type boundType, @NonNull Class<?> targetType, int targetTypeVariableIndex) {
        Map<TypeVariable<? extends Class<?>>, Type> typeVariableTypeMap = resolveTypeVariables(ownerType);
        Type reifiedType = reifyType(boundType, typeVariableTypeMap);
        return resolveTypeVariable(reifiedType, targetType, targetTypeVariableIndex);
    }

    public static Type reifyType(Type type, @NonNull Map<? extends TypeVariable<?>, Type> typeVariables) {
        if (type instanceof Class) {
            return type;
        } else if (type instanceof ParameterizedType) {
            Type[] boundTypes = ((ParameterizedType) type).getActualTypeArguments();
            Type[] newBoundTypes = new Type[boundTypes.length];
            for (int i = 0; i < boundTypes.length; i++) {
                newBoundTypes[i] = reifyType(boundTypes[i], typeVariables);
            }
            return parameterized(
                reifyType(((ParameterizedType) type).getOwnerType(), typeVariables),
                reifyType(((ParameterizedType) type).getRawType(), typeVariables),
                newBoundTypes
            );
        } else if (type instanceof TypeVariable) {
            Type resolvedType = typeVariables.get(type);
            if (resolvedType != null) {
                return reifyType(resolvedType, typeVariables);
            } else {
                return reifyType(((TypeVariable) type).getBounds()[0], typeVariables);
            }
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Type newComponentType = reifyType(componentType, typeVariables);
            return arrayOf(newComponentType);
        } else if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            if (upperBounds != null && upperBounds.length >0) {
                return reifyType(upperBounds[0], typeVariables);
            } else {
                return Object.class;
            }
        }
        return type;
    }

    public static Map<TypeVariable<? extends Class<?>>, Type> resolveTypeVariables(Type type) {
        Map<TypeVariable<? extends Class<?>>, Type> resolvedTypeVariables = new HashMap<>();
        resolveTypeVariables(type, resolvedTypeVariables);
        return resolvedTypeVariables;
    }

    private static void resolveTypeVariables(Type rootType, @NonNull Map<TypeVariable<? extends Class<?>>, Type> resolvedTypeVariables) {
        List<Type> remainingTypes = new ArrayList<>();
        remainingTypes.add(rootType);
        while (!remainingTypes.isEmpty()) {
            Type type = remainingTypes.remove(0);
            if (type instanceof ParameterizedType) {
                Class<?> rawClass = rawType(type);
                TypeVariable<? extends Class<?>>[] boundVariables = rawClass.getTypeParameters();
                for (int i = 0; i < boundVariables.length; i++) {
                    resolvedTypeVariables.putIfAbsent(boundVariables[i], ((ParameterizedType) type).getActualTypeArguments()[i]);
                }
            } else if (type instanceof Class) {
                remainingTypes.addAll(Arrays.asList(((Class) type).getGenericInterfaces()));
                remainingTypes.add(((Class) type).getGenericSuperclass());
            }
        }
    }

    public static Type resolveTypeVariable(Type boundType, TypeVariable<?> targetTypeVariable) {
        Map<TypeVariable<? extends Class<?>>, Type> typeVariables = resolveTypeVariables(boundType);
        return typeVariables.get(targetTypeVariable);
    }

    public static Type resolveTypeVariable(Type boundType, @NonNull Class<?> targetClass, int targetTypeVariableIndex) {
        TypeVariable<? extends Class<?>>[] typeParameters = targetClass.getTypeParameters();
        if (typeParameters == null || typeParameters.length == 0) {
            throw new IllegalArgumentException(targetClass.getName() + " is not a generic class.");
        } else if (targetTypeVariableIndex >= typeParameters.length) {
            throw new IndexOutOfBoundsException("Generic parameter index " + targetTypeVariableIndex + " is invalid for class " + targetClass
                .getName());
        }
        return resolveTypeVariable(boundType, typeParameters[targetTypeVariableIndex]);
    }

    @Value
    private static class ParameterizedTypeImpl implements ParameterizedType {

        Type ownerType;

        @NonNull
        Type rawType;

        @NonNull
        Type[] actualTypeArguments;
    }

    @Value
    private static class GenericArrayTypeImpl implements GenericArrayType {

        @NonNull
        Type genericComponentType;

        @Override
        public String toString() {
            return getGenericComponentType().toString() + "[]";
        }

    }

    private Types() {}

}
