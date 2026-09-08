import dev.e2e.driver.FileDriver;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public final class GlfwInputProbe {
    public static void main(String[] args) throws Exception {
        if (!GLFW.glfwInit()) throw new IllegalStateException("GLFW initialization failed");
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window = GLFW.glfwCreateWindow(480, 240, "External input probe", 0, 0);
        if (window == 0) throw new IllegalStateException("Window creation failed");
        int[] counts = new int[3];
        String expected = "外部入力テスト: Oak Log\n";
        try {
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("OPENGL " + GL11.glGetString(GL11.GL_VERSION) + " / " + GL11.glGetString(GL11.GL_RENDERER));
            GL11.glClearColor(0.07f, 0.13f, 0.2f, 1.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwSetKeyCallback(window, (handle, key, scan, action, mods) -> {
                System.out.printf("KEY key=%d action=%d mods=%d%n", key, action, mods);
                if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_PRESS && mods == GLFW.GLFW_MOD_CONTROL) {
                    counts[0]++;
                    GLFW.glfwSetClipboardString(handle, expected);
                }
                if (key == GLFW.GLFW_KEY_C && action == GLFW.GLFW_RELEASE) counts[1]++;
            });
            GLFW.glfwSetCursorPosCallback(window, (handle, x, y) -> {
                System.out.printf("POINTER x=%.1f y=%.1f%n", x, y);
                if (Math.abs(x - 120) < 1 && Math.abs(y - 100) < 1) counts[2]++;
            });
            GLFW.glfwSetClipboardString(window, "before-external-input");
            FileDriver driver = new FileDriver();
            driver.call("hover", ",\"x\":120,\"y\":100", GLFW::glfwPollEvents);
            driver.call("hotkey", ",\"keys\":\"ctrl+c\"", GLFW::glfwPollEvents);
            driver.call("clipboard", ",\"expected\":" + FileDriver.quote(expected), GLFW::glfwPollEvents);
            driver.call("screenshot", "", GLFW::glfwPollEvents);
            boolean mismatchRejected = false;
            try {
                driver.call("clipboard", ",\"expected\":" + FileDriver.quote(expected.trim()), GLFW::glfwPollEvents);
            } catch (AssertionError error) {
                if (!error.getMessage().contains("Clipboard content does not match")) throw error;
                mismatchRejected = true;
            }
            if (!mismatchRejected) throw new AssertionError("Clipboard comparison ignored the trailing newline");
            GLFW.glfwPollEvents();
            if (counts[0] != 1 || counts[1] != 1 || counts[2] < 1
                    || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) != GLFW.GLFW_RELEASE
                    || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_C) != GLFW.GLFW_RELEASE) {
                throw new AssertionError("Unexpected event counts/state: Ctrl+C=" + counts[0] + ", release=" + counts[1] + ", hover=" + counts[2]);
            }
            System.out.println("PASS OpenGL, external Ctrl+C, pointer, key release, Unicode clipboard");
        } finally {
            Callbacks.glfwFreeCallbacks(window);
            GLFW.glfwDestroyWindow(window);
            GLFW.glfwTerminate();
        }
    }
}
