package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkEventCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

/**
 * Represents a Vulkan event.
 * An event is a synchronization primitive that can be used to insert a fine-grained dependency between commands
 * submitted to the same queue, or between the host and a queue.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public class Event extends LibCleanable {
    /**
     * The handle to the Vulkan event.
     */
    private final DeviceHandle handle;
    /**
     * Constructs a new Event object.
     *
     * @param device_in The Vulkan device associated with this event.
     * @throws IllegalStateException If the creation of the Vulkan event fails.
     */
    public Event(@Nonnull Device device_in) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            VkEventCreateInfo pCreateInfo = VkEventCreateInfo.calloc(stack);
            pCreateInfo.sType$Default().pNext(NULL);
            // Create the Vulkan event object
            if (device_in.createEvent(pCreateInfo, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: could not create event");
                throw new IllegalStateException("could not create event");
            }
            handle = new DeviceHandle(device_in,buffer.get(0));
        }
    }
    /**
     * Resets the event to the un-signaled state.
     */
    public int reset() {
        return handle.device().resetEvent(handle.handle());
    }
    /**
     * Gets the status of the event.
     */
    public int getStatus() {
        return handle.device().getEventStatus(handle.handle());
    }

    /**
     * To set the state of an event to signaled from the host
     * @return VkResult
     */
    public int set() {
        return handle.device().setEvent(handle.handle());
    }
    /**
     * Gets the handle of the Vulkan event.
     *
     * @return The handle of the Vulkan event.
     */
    public long getEvent() {
        return handle.handle();
    }
    /**
     * Cleans up and destroys the Vulkan event.
     */
    @Override
    public final void free() {
        handle.device().destroyEvent(handle.handle());
    }
}
