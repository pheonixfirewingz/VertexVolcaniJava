package com.github.vertexvolcani.graphics.vulkan.pipeline;

import com.github.vertexvolcani.graphics.vulkan.Device;
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

    public int submit(PointerBuffer command_buffers, IntBuffer wait_dst_stage_mask, LongBuffer wait_semaphores, LongBuffer signal_semaphores, @Nullable Fence fence){
        try(MemoryStack stack = MemoryStack.stackPush()) {
        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack).sType$Default().waitSemaphoreCount(wait_semaphores.remaining()).pWaitSemaphores(wait_semaphores)
                .pWaitDstStageMask(wait_dst_stage_mask).pCommandBuffers(command_buffers).pSignalSemaphores(signal_semaphores);
        return vkQueueSubmit(queue, submitInfo,fence != null?fence.getFence():VK_NULL_HANDLE);
        }
    }

    public void waitIdle() {
        vkQueueWaitIdle(queue);
    }

    public VkQueue getQueue() {
        return queue;
    }
}
