package com.github.vertexvolcani.graphics.vulkan;

import com.github.vertexvolcani.Window;
import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Represents a Vulkan surface associated with a GLFW window.
 * This class extends CleanerObject for automatic cleanup.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-01
 */
public class Surface extends CleanerObject {

    /**
     * The Vulkan instance associated with this surface.
     */
    private final Instance instance;
    /**
     * The Vulkan device associated with this surface.
     */
    private final Device device;

    /**
     * The handle to the Vulkan surface.
     */
    private final long handle;

    private final VkSurfaceFormatKHR.Buffer formats;

    /**
     * Constructs a new Vulkan surface associated with the given window and Vulkan instance.
     *
     * @param window         The GLFW window for which the surface is created.
     * @param instance_in    The Vulkan instance to associate with the surface.
     * @throws IllegalStateException If the surface creation fails.
     */
    public Surface(@Nonnull Window window,@Nonnull Instance instance_in,@Nonnull Device device_in) {
        super();
        Log.print(Log.Severity.DEBUG, "Vulkan: creating Vulkan surface...");
        instance = instance_in;
        device = device_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            IntBuffer pFormatCount = stack.callocInt(1);
            if (glfwCreateWindowSurface(instance.getInstance(), window.getID(), null, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: could not create window binder surface");
                throw new IllegalStateException("Could not create window binder surface");
            }
            handle = buffer.get(0);

            if (vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), handle, pFormatCount, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: Failed to query number of physical device surface formats");
                throw new IllegalStateException("Failed to query number of physical device surface formats");
            }

            formats = VkSurfaceFormatKHR.calloc(pFormatCount.get(0));
            int formatCount = pFormatCount.get(0);
            if (vkGetPhysicalDeviceSurfaceFormatsKHR(device.getPhysicalDevice(), handle, pFormatCount, formats) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: Failed to query physical device surface formats");
                throw new IllegalStateException("Failed to query physical device surface formats");
            }
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: done creating Vulkan surface");
    }

    /**
     * Gets the handle to the Vulkan surface.
     *
     * @return The handle to the Vulkan surface.
     */
    public long getSurface() {
        return handle;
    }

    public VkSurfaceFormatKHR.Buffer getSurfaceFormats() {
        return formats;
    }

    public VkExtent2D getSurfaceSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR surfCaps = VkSurfaceCapabilitiesKHR.calloc(stack);
            if (vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getPhysicalDevice(), handle, surfCaps) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to get physical device surface capabilities");
                throw new IllegalStateException("Failed to get physical device surface capabilities");
            }
            return surfCaps.currentExtent();
        }
    }

    /**
     * Cleans up and destroys the Vulkan surface.
     */
    @Override
    public final void cleanup() {
        vkDestroySurfaceKHR(instance.getInstance(), handle, null);
        formats.free();
        Log.print(Log.Severity.DEBUG, "Vulkan: done freeing Vulkan surface");
    }
}
