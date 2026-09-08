package dev.itemnamecopy.test.runtime;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Reflect {
    static ClassLoader loader;

    static Class<?> type(String... names) {
        for (String name : names) {
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new IllegalStateException("Class missing: " + Arrays.toString(names));
    }

    static Object get(Object owner, String... names) {
        Class<?> start = owner instanceof Class ? (Class<?>) owner : owner.getClass();
        for (String name : names) {
            for (Class<?> type = start; type != null; type = type.getSuperclass()) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(owner instanceof Class ? null : owner);
                } catch (NoSuchFieldException ignored) {
                } catch (ReflectiveOperationException error) {
                    throw failure(error);
                }
            }
        }
        throw new IllegalStateException("Field missing: " + start.getName() + " " + Arrays.toString(names));
    }

    static Object optionalGet(Object owner, String... names) {
        if (owner == null) return null;
        try {
            return get(owner, names);
        } catch (IllegalStateException missing) {
            return null;
        }
    }

    static void set(Object owner, Object value, String... names) {
        Class<?> start = owner instanceof Class ? (Class<?>) owner : owner.getClass();
        for (String name : names) {
            for (Class<?> type = start; type != null; type = type.getSuperclass()) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    field.set(owner instanceof Class ? null : owner, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                } catch (ReflectiveOperationException error) {
                    throw failure(error);
                }
            }
        }
        throw new IllegalStateException("Field missing: " + start.getName() + " " + Arrays.toString(names));
    }

    static Object call(Object owner, String names, Object... arguments) {
        Class<?> start = owner instanceof Class ? (Class<?>) owner : owner.getClass();
        for (String name : names.split("\\|")) {
            for (Class<?> type = start; type != null; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    if (!method.getName().equals(name) || !accepts(method.getParameterTypes(), arguments)) continue;
                    if (owner instanceof Class && !Modifier.isStatic(method.getModifiers())) continue;
                    try {
                        method.setAccessible(true);
                        return method.invoke(owner instanceof Class ? null : owner, arguments);
                    } catch (ReflectiveOperationException error) {
                        throw failure(error);
                    }
                }
            }
        }
        for (Method method : start.getMethods()) {
            if (!Arrays.asList(names.split("\\|")).contains(method.getName()) || !accepts(method.getParameterTypes(), arguments))
                continue;
            try {
                return method.invoke(owner instanceof Class ? null : owner, arguments);
            } catch (ReflectiveOperationException error) {
                throw failure(error);
            }
        }
        throw new IllegalStateException("Method missing: " + start.getName() + "." + names + "(" + arguments.length + ")");
    }

    static boolean has(Object owner, String names, int arity) {
        if (owner == null) return false;
        Class<?> start = owner instanceof Class ? (Class<?>) owner : owner.getClass();
        for (Class<?> type = start; type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getParameterCount() == arity && Arrays.asList(names.split("\\|")).contains(method.getName()))
                    return true;
            }
        }
        return false;
    }

    static Object make(Class<?> type, Object... arguments) {
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            if (!accepts(constructor.getParameterTypes(), arguments)) continue;
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(arguments);
            } catch (ReflectiveOperationException error) {
                throw failure(error);
            }
        }
        throw new IllegalStateException("Constructor missing: " + type.getName() + "(" + arguments.length + ")");
    }

    static List<Object> values(Object owner) {
        List<Object> values = new ArrayList<Object>();
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().startsWith("java.")) break;
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (value != null) values.add(value);
                } catch (ReflectiveOperationException error) {
                    throw failure(error);
                }
            }
        }
        return values;
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

    private static RuntimeException failure(ReflectiveOperationException error) {
        Throwable cause = error instanceof InvocationTargetException ? ((InvocationTargetException) error).getCause() : error;
        if (cause instanceof RuntimeException) return (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        return new IllegalStateException(cause);
    }
}
