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
    private static ClassLoader loader;

    private Reflect() {
    }

    static void initialize(ClassLoader classLoader) {
        loader = classLoader;
    }

    static Class<?> type(String... names) {
        for (String name : names) {
            Class<?> type = load(name);
            if (type != null) return type;
        }
        throw new IllegalStateException("Class missing: " + Arrays.toString(names));
    }

    static Object get(Object owner, String... names) {
        Class<?> ownerType = ReflectionMembers.ownerType(owner);
        Field field = ReflectionMembers.findField(ownerType, names);
        if (field == null) {
            throw new IllegalStateException("Field missing: " + ownerType.getName() + " " + Arrays.toString(names));
        }
        try {
            field.setAccessible(true);
            return field.get(ReflectionMembers.receiver(owner));
        } catch (ReflectiveOperationException error) {
            throw failure(error);
        }
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
        Class<?> ownerType = ReflectionMembers.ownerType(owner);
        Field field = ReflectionMembers.findField(ownerType, names);
        if (field == null) {
            throw new IllegalStateException("Field missing: " + ownerType.getName() + " " + Arrays.toString(names));
        }
        try {
            field.setAccessible(true);
            field.set(ReflectionMembers.receiver(owner), value);
        } catch (ReflectiveOperationException error) {
            throw failure(error);
        }
    }

    static Object call(Object owner, String names, Object... arguments) {
        Class<?> ownerType = ReflectionMembers.ownerType(owner);
        Method method = ReflectionMembers.findMethod(ownerType, names, arguments, owner instanceof Class);
        if (method == null) {
            throw new IllegalStateException(
                "Method missing: " + ownerType.getName() + "." + names + "(" + arguments.length + ")");
        }
        try {
            method.setAccessible(true);
            return method.invoke(ReflectionMembers.receiver(owner), arguments);
        } catch (ReflectiveOperationException error) {
            throw failure(error);
        }
    }

    static boolean has(Object owner, String names, int arity) {
        return owner != null && ReflectionMembers.hasMethod(ReflectionMembers.ownerType(owner), names, arity);
    }

    static Object make(Class<?> type, Object... arguments) {
        Constructor<?> constructor = ReflectionMembers.findConstructor(type, arguments);
        if (constructor == null) {
            throw new IllegalStateException("Constructor missing: " + type.getName() + "(" + arguments.length + ")");
        }
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException error) {
            throw failure(error);
        }
    }

    static List<Object> values(Object owner) {
        List<Object> values = new ArrayList<Object>();
        for (Field field : ReflectionMembers.instanceFields(owner.getClass())) {
            try {
                field.setAccessible(true);
                Object value = field.get(owner);
                if (value != null) values.add(value);
            } catch (ReflectiveOperationException error) {
                throw failure(error);
            }
        }
        return values;
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, true, loader);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static RuntimeException failure(ReflectiveOperationException error) {
        Throwable cause = error instanceof InvocationTargetException
            ? ((InvocationTargetException) error).getCause() : error;
        if (cause instanceof RuntimeException) return (RuntimeException) cause;
        if (cause instanceof Error) throw (Error) cause;
        return new IllegalStateException(cause);
    }
}
