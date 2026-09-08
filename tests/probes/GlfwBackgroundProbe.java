import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;

public final class GlfwBackgroundProbe {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(480, 240, "ItemNameCopy background input probe", 0, 0);
        if (window == 0) throw new IllegalStateException("Window creation failed");
        int[] counts = new int[4];
        GLFW.glfwSetKeyCallback(window, (handle, key, scan, action, mods) -> {
            System.out.printf("KEY key=%d action=%d mods=%d ctrlState=%d focused=%d%n", key, action, mods,
                GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL),
                GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_FOCUSED));
            counts[0]++;
            if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS && (mods & GLFW.GLFW_MOD_CONTROL) != 0) counts[1]++;
        });
        GLFW.glfwSetCursorPosCallback(window, (handle, x, y) -> {
            System.out.printf("POINTER x=%.1f y=%.1f%n", x, y);
            counts[2]++;
        });
        GLFW.glfwSetWindowFocusCallback(window, (handle, focused) -> {
            System.out.println("FOCUS " + focused);
            if (focused) counts[3]++;
        });
        GLFW.glfwShowWindow(window);
        System.out.printf("READY pid=%d window_id=%d focused=%d glfw=%s%n", ProcessHandle.current().pid(),
            GLFWNativeWin32.glfwGetWin32Window(window), GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_FOCUSED),
            GLFW.glfwGetVersionString());
        long deadline = System.nanoTime() + 30_000_000_000L;
        while (!GLFW.glfwWindowShouldClose(window) && System.nanoTime() < deadline) {
            GLFW.glfwPollEvents();
            Thread.sleep(5);
        }
        System.out.printf("RESULT keyEvents=%d controlC=%d pointerEvents=%d focusGains=%d%n", counts[0], counts[1], counts[2], counts[3]);
        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}
