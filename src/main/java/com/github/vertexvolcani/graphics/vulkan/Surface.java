package com.github.vertexvolcani.graphics.vulkan;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.Window;
import com.github.vertexvolcani.util.LibCleanable;
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
import static org.lwjgl.vulkan.VK10.*;

/**
 * Represents a Vulkan surface associated with a GLFW window.
 * This class extends CleanerObject for automatic cleanup.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-01
 */
public class Surface extends LibCleanable {

    /**
     * The Vulkan instance associated with this surface.
     */
    private final Instance instance;

    /**
     * The handle to the Vulkan surface.
     */
    private final DeviceHandle handle;

    private final VkSurfaceFormatKHR.Buffer formats;

    /**
     * Constructs a new Vulkan surface associated with the given window and Vulkan instance.
     *
     * @param window         The GLFW window for which the surface is created.
     * @param instance_in    The Vulkan instance to associate with the surface.
     * @throws IllegalStateException If the surface creation fails.
     */
    public Surface(@Nonnull Window window,@Nonnull Instance instance_in,@Nonnull Device device_in) {
        Log.print(Log.Severity.DEBUG, "Vulkan: creating Vulkan surface...");
        instance = instance_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            IntBuffer pFormatCount = stack.callocInt(1);
            if (glfwCreateWindowSurface(instance.getInstance(), window.getID(), null, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: could not create window binder surface");
                throw new IllegalStateException("Could not create window binder surface");
            }
            handle = new DeviceHandle(device_in,buffer.get(0));

            if (vkGetPhysicalDeviceSurfaceFormatsKHR(handle.device().getPhysicalDevice(), handle.handle(), pFormatCount, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: Failed to query number of physical device surface formats");
                throw new IllegalStateException("Failed to query number of physical device surface formats");
            }

            formats = VkSurfaceFormatKHR.calloc(pFormatCount.get(0));
            int formatCount = pFormatCount.get(0);
            if (vkGetPhysicalDeviceSurfaceFormatsKHR(handle.device().getPhysicalDevice(), handle.handle(), pFormatCount, formats) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: Failed to query physical device surface formats");
                throw new IllegalStateException("Failed to query physical device surface formats");
            }
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: done creating Vulkan surface");
    }

    public int getColourSpace() {
        return formats.get(0).colorSpace();
    }

    public int getColourFormat() {
        int colour_format;
        if (formats.remaining() == 1 && formats.get(0).format() == VK_FORMAT_UNDEFINED) {
            colour_format = VK_FORMAT_B8G8R8A8_UNORM;
        } else {
            colour_format = formats.get(0).format();
        }
        return colour_format;
    }

    /**
     * Gets the handle to the Vulkan surface.
     *
     * @return The handle to the Vulkan surface.
     */
    public long getSurface() {
        return handle.handle();
    }

    public VkExtent2D getSurfaceSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR surfCaps = VkSurfaceCapabilitiesKHR.calloc(stack);
            if (vkGetPhysicalDeviceSurfaceCapabilitiesKHR(handle.device().getPhysicalDevice(), handle.handle(), surfCaps) != VK_SUCCESS) {
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
    public final void free() {
        vkDestroySurfaceKHR(instance.getInstance(), handle.handle(), null);
        formats.free();
        Log.print(Log.Severity.DEBUG, "Vulkan: done freeing Vulkan surface");
    }
}
