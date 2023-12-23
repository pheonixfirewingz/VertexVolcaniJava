package com.github.vertexvolcani.graphics.vulkan.pipeline;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkEventCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Represents a Vulkan event.
 * An event is a synchronization primitive that can be used to insert a fine-grained dependency between commands
 * submitted to the same queue, or between the host and a queue.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-03
 */
public final class Event extends LibCleanable {
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
            handle = device_in.createEvent(pCreateInfo);
            if (device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: could not create event");
                throw new IllegalStateException("could not create event");
            }
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: created event");
    }
    /**
     * Resets the event to the un-signaled state.
     */
    public void reset() {
        handle.device().resetEvent(handle);
    }
    /**
     * Gets the status of the event.
     */
    public void getStatus() {
         handle.device().getEventStatus(handle);
    }

    /**
     * To set the state of an event to signaled from the host
     */
    public void set() {
         handle.device().setEvent(handle);
    }
    /**
     * Gets the handle of the Vulkan event.
     *
     * @return The handle of the Vulkan event.
     */
    public DeviceHandle getEvent() {
        return handle;
    }
    /**
     * Cleans up and destroys the Vulkan event.
     */
    @Override
    protected void free() {
        handle.device().destroyEvent(handle);
        Log.print(Log.Severity.DEBUG, "Vulkan: done freeing event");
    }
}
