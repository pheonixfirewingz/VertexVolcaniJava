package com.github.vertexvolcani;

import com.github.vertexvolcani.message.Dispatcher;
import com.github.vertexvolcani.message.IHasDispatcher;
import com.github.vertexvolcani.message.IListener;
import com.github.vertexvolcani.message.WindowCanClosingEvent;
import com.github.vertexvolcani.message.events.*;
import jakarta.annotation.Nullable;
import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryUtil.NULL;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public class Window extends Thread implements IListener, IHasDispatcher {
    private final Dispatcher dispatcher = new Dispatcher();
    private static Window window;
    private boolean stop_thread = false;
    private final CharSequence title;
    private final long window_id;

    public Window(int width,int height,CharSequence title_in) {
        window = this;
        title = title_in;
        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit())
            throw new RuntimeException("could not boot glfw");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        window_id = glfwCreateWindow(width,height, title,NULL,NULL);

        if(window_id == NULL) {
            glfwTerminate();
            throw new RuntimeException("could not create glfw window");
        }

        if (!glfwVulkanSupported()) {
            glfwTerminate();
            throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)");
        }

        glfwSetWindowSizeLimits(window_id, 640, 480, GLFW_DONT_CARE, GLFW_DONT_CARE);
        glfwSetWindowAspectRatio(window_id, 16, 9);

        // Set up callbacks
        glfwSetKeyCallback(window_id, (window, key, scancode, action, mods) -> dispatcher.dispatch(new KeyEvent(key,scancode,mods,action == GLFW_PRESS)));
        glfwSetCursorPosCallback(window_id,(window,pos_x,pos_y) -> dispatcher.dispatch(new MouseMoveEvent(pos_x, pos_y)));
        glfwSetMouseButtonCallback(window_id,(window,button,action,mods) -> dispatcher.dispatch(new MousePressEvent(button, action,mods)));
        glfwSetFramebufferSizeCallback(window_id,(window,_width,_height) -> dispatcher.dispatch(new WindowResizeEvent(_width,_height)));
        glfwFocusWindow(window_id);
    }

    public static @Nullable Window getWindow() {
        return window;
    }

    public CharSequence getTitle() {
        return title;
    }

    protected void close(){
        dispatcher.dispatch(new WindowIsClosingEvent());
        stop_thread = true;
    }

    @Override
    public void run() {
        while (!stop_thread) {
            glfwPollEvents();
            if(glfwWindowShouldClose(window_id)){
                dispatcher.dispatch(new WindowIsClosingEvent());
            }
        }
        window = null;
        glfwTerminate();
    }

    @Override
    public void eventExe(IEvent event) {
        if(event.getID() == StopThread.ID || event.getID() == WindowCanClosingEvent.ID) {
            Window.getWindow().close();
        }
    }

    @Override
    public void subscribe(IListener listener) {
        dispatcher.subscribe(listener);
    }

    @Override
    public void unSubscribe(IListener listener) {
        dispatcher.unSubscribe(listener);
    }

    public long getID() {
        return window_id;
    }
}
