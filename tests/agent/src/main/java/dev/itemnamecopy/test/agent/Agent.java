package dev.itemnamecopy.test.agent;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public final class Agent {
    public static void premain(String options, Instrumentation instrumentation) throws Exception {
        File directory = new File(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(new File(directory, "client-test-runtime.jar")));
        URLClassLoader isolated = new URLClassLoader(new URL[] {
            new File(directory, "client-test-transformer.jar").toURI().toURL()
        }, null);
        ClassFileTransformer transformer = (ClassFileTransformer) isolated.loadClass(
            "dev.itemnamecopy.test.agent.Transformer").newInstance();
        instrumentation.addTransformer(transformer);
    }
}
