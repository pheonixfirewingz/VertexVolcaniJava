package com.github.vertexvolcani.graphics;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.events.*;
import com.github.vertexvolcani.graphics.vulkan.*;
import com.github.vertexvolcani.graphics.vulkan.pipeline.Queue;
import com.github.vertexvolcani.graphics.vulkan.pipeline.Semaphore;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Nonnull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.NativeType;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;

/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public final class VVWindow extends LibCleanable {

    private float fov = 90.0f;
    private float near_depth = 0.01f;
    private float far_depth = 1000.0f;
    private Matrix4f projection;
    private final String title;
    private final long window_id;
    private final Surface surface;
    private final SwapChain swap_chain;
    private final VmaAllocator allocator;

    public static void PrimeGLFW() {
        GLFWErrorCallback.createPrint(System.err).set();
        if(!glfwInit())
            throw new RuntimeException("could not boot glfw");
    }
    public VVWindow(int width, int height, String title_in, @Nonnull EventsCallback callback, @Nonnull Instance instance,@Nonnull Device device,@Nonnull SwapChain.SwapChainBuilder swap_chain_builder) {
        super();
        title = title_in;
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


        surface = new Surface(this,instance, device);
        allocator = new VmaAllocator(instance,device);
        swap_chain = new SwapChain(device, surface,allocator,swap_chain_builder);

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

    public Surface getSurface() {
        return surface;
    }

    public SwapChain getSwapChain() {
        return swap_chain;
    }

    public VmaAllocator getAllocator() {
        return allocator;
    }

    public void changeTitle(String title_in) {
        GLFW.glfwSetWindowTitle(window_id, title_in);
    }

    public Matrix4f getProjection() {
        return projection;
    }

    public void poll() {
        glfwPollEvents();
    }

    public int swapBuffers(@Nonnull Queue queue, @Nonnull Semaphore[] pWaitSemaphores, @NativeType("uint32_t const *") @Nonnull IntBuffer pImageIndex) {
        int ret = swap_chain.queuePresent(queue.getQueue(), pWaitSemaphores, pImageIndex);
        if(ret == VK_SUBOPTIMAL_KHR || ret == VK_ERROR_OUT_OF_DATE_KHR) {
            queue.waitIdle();
            swap_chain.recreate();
        }
        return ret;
    }

    public long getID() {
        return window_id;
    }

    @Override
    public void free() {
        swap_chain.free();
        surface.free();
        allocator.free();
        glfwTerminate();
    }

    public boolean ShouldClose() {
        return glfwWindowShouldClose(window_id);
    }

    public void refreshSwapChain() {
        swap_chain.recreate();
    }

    public void resizeWindow(int width, int height) {
        glfwSetWindowSize(window_id, width, height);
        refreshSwapChain();
        projection = new Matrix4f().identity();
        projection.perspective(fov, (float) width / (float) height, near_depth, far_depth,true);
    }

    public void moveWindow(int x, int y) {
        glfwSetWindowPos(window_id, x, y);
    }

    public void offsetWindow(int x, int y) {
        int[] x_offset = new int[1];
        int[] y_offset = new int[1];
        glfwGetWindowPos(window_id, x_offset, y_offset);
        x += x_offset[0];
        y += y_offset[0];
        glfwSetWindowPos(window_id, x, y);
    }
    public interface EventsCallback{
        void eventCallbacks(IEvent event);
    }
}