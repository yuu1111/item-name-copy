package dev.itemnamecopy.test.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class ReflectionMembers {
    private ReflectionMembers() {
    }

    static Class<?> ownerType(Object owner) {
        return owner instanceof Class ? (Class<?>) owner : owner.getClass();
    }

    static Object receiver(Object owner) {
        return owner instanceof Class ? null : owner;
    }

    static Field findField(Class<?> start, String... names) {
        for (String name : names) {
            for (Class<?> type : hierarchy(start)) {
                try {
                    return type.getDeclaredField(name);
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        return null;
    }

    static Method findMethod(Class<?> start, String names, Object[] arguments, boolean staticOnly) {
        String[] alternatives = alternatives(names);
        for (String name : alternatives) {
            for (Class<?> type : hierarchy(start)) {
                Method method = matchingMethod(type.getDeclaredMethods(), name, arguments, staticOnly);
                if (method != null) return method;
            }
        }
        for (Method method : start.getMethods()) {
            if (contains(alternatives, method.getName()) && accepts(method.getParameterTypes(), arguments)) return method;
        }
        return null;
    }

    static boolean hasMethod(Class<?> start, String names, int arity) {
        String[] alternatives = alternatives(names);
        for (Class<?> type : hierarchy(start)) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() == arity && contains(alternatives, method.getName())) return true;
            }
        }
        return false;
    }

    static Constructor<?> findConstructor(Class<?> type, Object[] arguments) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (accepts(constructor.getParameterTypes(), arguments)) return constructor;
        }
        return null;
    }

    static List<Field> instanceFields(Class<?> start) {
        List<Field> fields = new ArrayList<Field>();
        for (Class<?> type : hierarchy(start)) {
            if (type.getName().startsWith("java.")) break;
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) fields.add(field);
            }
        }
        return fields;
    }

    private static Method matchingMethod(Method[] methods, String name, Object[] arguments, boolean staticOnly) {
        for (Method method : methods) {
            if (!method.getName().equals(name) || !accepts(method.getParameterTypes(), arguments)) continue;
            if (!staticOnly || Modifier.isStatic(method.getModifiers())) return method;
        }
        return null;
    }

    private static boolean accepts(Class<?>[] parameters, Object[] arguments) {
        if (parameters.length != arguments.length) return false;
        for (int i = 0; i < parameters.length; i++) {
            if (arguments[i] == null) {
                if (parameters[i].isPrimitive()) return false;
            } else if (!boxed(parameters[i]).isInstance(arguments[i])) return false;
        }
        return true;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == char.class) return Character.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        return type;
    }

    private static String[] alternatives(String names) {
        return names.split("\\|");
    }

    private static boolean contains(String[] names, String candidate) {
        return Arrays.asList(names).contains(candidate);
    }

    private static List<Class<?>> hierarchy(Class<?> start) {
        List<Class<?>> types = new ArrayList<Class<?>>();
        for (Class<?> type = start; type != null; type = type.getSuperclass()) types.add(type);
        return types;
    }
}
