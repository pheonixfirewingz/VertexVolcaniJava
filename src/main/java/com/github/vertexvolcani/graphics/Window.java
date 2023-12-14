package com.github.vertexvolcani.graphics;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.events.*;
import com.github.vertexvolcani.util.LibCleanable;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryUtil.NULL;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public final class Window extends LibCleanable {

    private float fov = 90.0f;
    private float near_depth = 0.01f;
    private float far_depth = 1000.0f;
    private Matrix4f projection;
    private final CharSequence title;
    private final long window_id;

    public Window(int width,int height,CharSequence title_in,EventsCallback callback) {
        super();
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

        glfwSetWindowSizeLimits(window_id, 640, 360, GLFW_DONT_CARE, GLFW_DONT_CARE);
        glfwSetWindowAspectRatio(window_id, 16, 9);

        projection = new Matrix4f().identity();
        projection.perspective(fov, (float) width / (float) height, near_depth, far_depth,true);

        // Set up callbacks
        glfwSetKeyCallback(window_id, (window, key, scancode, action, mods) -> callback.eventCallbacks(new KeyEvent(key,scancode,mods,action == GLFW_PRESS)));
        glfwSetCursorPosCallback(window_id,(window,pos_x,pos_y) -> callback.eventCallbacks(new MouseMoveEvent(pos_x, pos_y)));
        glfwSetMouseButtonCallback(window_id,(window,button,action,mods) -> callback.eventCallbacks(new MousePressEvent(button, action,mods)));
        glfwSetFramebufferSizeCallback(window_id,(window,_width,_height) -> {
            projection = new Matrix4f().identity();
            projection.perspective(90f, (float) _width / (float) _height, near_depth, far_depth,true);
            callback.eventCallbacks(new WindowResizeEvent(_width,_height));
        });
        glfwSetWindowCloseCallback(window_id,window -> { callback.eventCallbacks(new WindowClosingEvent());});
        glfwFocusWindow(window_id);
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov_in) {
        fov = fov_in;
    }

    public float getNearDepth() {
        return near_depth;
    }

    public void setNearDepth(float near_depth_in) {
        near_depth = near_depth_in;
    }

    public float getFarDepth() {
        return far_depth;
    }

    public void setFarDepth(float far_depth_in) {
        far_depth = far_depth_in;
    }

    public CharSequence getTitle() {
        return title;
    }

    public void updateExtendedTile(CharSequence title_in) {
        StringBuilder builder = new StringBuilder();
        builder.append(title);
        builder.append(title_in);
        ByteBuffer byteBuffer = MemoryUtil.memCalloc(builder.length());
        byteBuffer.put(builder.toString().getBytes(StandardCharsets.UTF_8));
        glfwSetWindowTitle(window_id,byteBuffer);
        MemoryUtil.memFree(byteBuffer);
    }

    public Matrix4f getProjection() {
        return projection;
    }

    public void poll() {
        glfwPollEvents();
    }

    public long getID() {
        return window_id;
    }

    @Override
    public void free() {
        glfwTerminate();
    }

    public boolean ShouldClose() {
        return glfwWindowShouldClose(window_id);
    }

    public interface EventsCallback{
        public void eventCallbacks(IEvent event);
    }
}
