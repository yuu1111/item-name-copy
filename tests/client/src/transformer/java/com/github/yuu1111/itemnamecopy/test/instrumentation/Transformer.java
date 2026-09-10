package com.github.yuu1111.itemnamecopy.test.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class Transformer implements ClassFileTransformer, Opcodes {
    private static final String RUNTIME = "com/github/yuu1111/itemnamecopy/test/bootstrap/ItemNameCopyTestRuntime";
    private final boolean external = Boolean.getBoolean("itemnamecopy.test.external");

    @Override
    public byte[] transform(ClassLoader loader, final String name, Class<?> redefining,
                            ProtectionDomain domain, byte[] bytes) {
        final boolean minecraft = "net/minecraft/client/Minecraft".equals(name);
        if (external && !minecraft) return null;
        final boolean screen = "net/minecraft/client/gui/screens/Screen".equals(name)
                || "net/minecraft/client/gui/screen/Screen".equals(name)
                || "net/minecraft/client/gui/GuiScreen".equals(name);
        final boolean keyboard = "org/lwjgl/input/Keyboard".equals(name);
        final boolean mouse = "org/lwjgl/input/Mouse".equals(name);
        final boolean inputConstants = "com/mojang/blaze3d/platform/InputConstants".equals(name);
        if (!minecraft && !screen && !keyboard && !mouse && !inputConstants) return null;
        try {
            ClassReader reader = new ClassReader(bytes);
            final boolean stackMapFrames = reader.readUnsignedShort(6) >= V1_6;
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            reader.accept(new ClassVisitor(ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, final String method, final String descriptor,
                                                 String signature, String[] exceptions) {
                    MethodVisitor original = super.visitMethod(access, method, descriptor, signature, exceptions);
                    if (minecraft && "()V".equals(descriptor)
                            && ("tick".equals(method) || "runTick".equals(method) || "func_71407_l".equals(method))) {
                        System.out.println("CLIENT_TEST_HOOK " + name + "." + method);
                        return new MethodVisitor(ASM9, original) {
                            @Override
                            public void visitInsn(int opcode) {
                                if (opcode == RETURN) {
                                    visitVarInsn(ALOAD, 0);
                                    visitMethodInsn(INVOKESTATIC, RUNTIME, "tick", "(Ljava/lang/Object;)V", false);
                                }
                                super.visitInsn(opcode);
                            }
                        };
                    }
                    String override = null;
                    if (screen && "()Z".equals(descriptor)
                            && ("hasControlDown".equals(method) || "isControlDown".equals(method)
                            || "isCtrlKeyDown".equals(method) || "func_146271_m".equals(method)))
                        override = "controlState";
                    if (screen && "()Z".equals(descriptor)
                            && ("hasShiftDown".equals(method) || "isShiftKeyDown".equals(method)))
                        override = "shiftState";
                    if (screen && "()Z".equals(descriptor)
                            && ("hasAltDown".equals(method) || "isAltKeyDown".equals(method)))
                        override = "altState";
                    if (keyboard && "getEventKey".equals(method)) override = "eventKey";
                    if (keyboard && "getEventCharacter".equals(method)) override = "eventCharacter";
                    if (keyboard && "getEventKeyState".equals(method)) override = "eventKeyState";
                    if (keyboard && "isRepeatEvent".equals(method)) override = "repeatState";
                    if (keyboard && "isKeyDown".equals(method)) override = "keyDown";
                    if (mouse && "getX".equals(method)) override = "mouseX";
                    if (mouse && "getY".equals(method)) override = "mouseY";
                    if (inputConstants && "isKeyDown".equals(method)
                            && ("(JI)Z".equals(descriptor)
                            || "(Lcom/mojang/blaze3d/platform/Window;I)Z".equals(descriptor)))
                        override = "glfwKeyDown";
                    if (override == null) return original;
                    final String overrideName = override;
                    final int parameterIndex = descriptor.startsWith("(I)") ? 0
                            : "(JI)Z".equals(descriptor) ? 2
                            : "(Lcom/mojang/blaze3d/platform/Window;I)Z".equals(descriptor) ? 1 : -1;
                    return new MethodVisitor(ASM9, original) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            if (parameterIndex >= 0) visitVarInsn(ILOAD, parameterIndex);
                            visitMethodInsn(INVOKESTATIC, RUNTIME, overrideName,
                                    parameterIndex >= 0 ? "(I)I" : "()I", false);
                            visitInsn(DUP);
                            Label fallback = new Label();
                            visitJumpInsn(IFLT, fallback);
                            visitInsn(IRETURN);
                            visitLabel(fallback);
                            if (stackMapFrames) visitFrame(F_SAME1, 0, null, 1, new Object[]{INTEGER});
                            visitInsn(POP);
                        }
                    };
                }
            }, 0);
            return writer.toByteArray();
        } catch (Throwable error) {
            error.printStackTrace();
            throw new IllegalStateException("Could not instrument " + name, error);
        }
    }
}
