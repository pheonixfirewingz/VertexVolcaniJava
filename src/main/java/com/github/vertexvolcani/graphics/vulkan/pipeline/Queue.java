package com.github.vertexvolcani.graphics.vulkan.pipeline;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.vulkan.Device;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class Queue {
    private final VkQueue queue;

    public Queue(Device device,int family,int index) {
        queue = device.getDeviceQueue(family,index);
    }

    public int submit(PointerBuffer command_buffers, IntBuffer wait_dst_stage_mask, @Nonnull Semaphore[] wait_semaphores, Semaphore[] signal_semaphores, @Nullable Fence fence){
        try(MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer wait_semaphores_buffer = stack.mallocLong(wait_semaphores.length);
            LongBuffer signal_semaphores_buffer = stack.mallocLong(signal_semaphores.length);
            for (int i = 0; i < wait_semaphores.length; i++) {
                wait_semaphores_buffer.put(i, wait_semaphores[i].getSemaphore().handle());
            }
            for (int i = 0; i < signal_semaphores.length; i++) {
                signal_semaphores_buffer.put(i, signal_semaphores[i].getSemaphore().handle());
            }
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType$Default().waitSemaphoreCount(wait_semaphores.length).pWaitSemaphores(wait_semaphores_buffer)
                .pWaitDstStageMask(wait_dst_stage_mask).pCommandBuffers(command_buffers).pSignalSemaphores(signal_semaphores_buffer);
        return vkQueueSubmit(queue, submitInfo,fence != null?fence.getFence().handle():VK_NULL_HANDLE);
        }
    }

    public void waitIdle() {
        vkQueueWaitIdle(queue);
    }

    public VkQueue getQueue() {
        return queue;
    }
}
