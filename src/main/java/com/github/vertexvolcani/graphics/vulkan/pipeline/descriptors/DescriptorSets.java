package com.github.vertexvolcani.graphics.vulkan.pipeline.descriptors;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public final class DescriptorSets extends LibCleanable {
    private final Device device;
    private final DescriptorPool pool;
    private final long[] handles;

    public DescriptorSets(Device device_in, DescriptorPool pool_in, DescriptorLayout[] layouts) {
        handles = new long[layouts.length];
        device = device_in;
        pool = pool_in;
        try(VkDescriptorSetAllocateInfo.Buffer pCreateInfo = VkDescriptorSetAllocateInfo.calloc(1)) {
            pCreateInfo.sType$Default();
            pCreateInfo.descriptorPool(pool.getHandle().handle());
            LongBuffer pBuffer = MemoryUtil.memCallocLong(layouts.length);
            for (int i = 0; i < layouts.length; i++) {
                pBuffer.put(i, layouts[i].getHandle().handle());
            }
            pCreateInfo.pSetLayouts(pBuffer);
            device_in.allocateDescriptorSets(pCreateInfo.get(0),handles);
            if(device_in.didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to allocate descriptor sets");
                throw new IllegalStateException("Failed to allocate descriptor sets");
            }
        }
    }

    public DescriptorSets writeBuffer(int dstSet, int dstBinding, int dstArrayElement,boolean dynamic, @Nonnull VkDescriptorBufferInfo.Buffer buffer) {
            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1);
            descriptorWrite.sType$Default();
            descriptorWrite.dstSet(handles[dstSet]);
            descriptorWrite.dstBinding(dstBinding);
            descriptorWrite.dstArrayElement(dstArrayElement);
            descriptorWrite.descriptorType(dynamic ? VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC : VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            descriptorWrite.pBufferInfo(buffer);
            device.updateDescriptorSets(descriptorWrite, null);
            descriptorWrite.free();
        return this;
    }

    public DescriptorSets writeImage(int dstSet, int dstBinding, int dstArrayElement, DescriptorType type, @Nonnull VkDescriptorImageInfo.Buffer buffer) {
        VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1);
        descriptorWrite.sType$Default();
        descriptorWrite.dstSet(handles[dstSet]);
        descriptorWrite.dstBinding(dstBinding);
        descriptorWrite.dstArrayElement(dstArrayElement);
        descriptorWrite.descriptorType(type.getDescriptorType());
        descriptorWrite.pImageInfo(buffer);
        device.updateDescriptorSets(descriptorWrite, null);
        descriptorWrite.free();
        return this;
    }

    public long getHandle(int index) {
        return handles[index];
    }

    @Override
    protected void free() {
        for (long handle : handles) {
            device.freeDescriptorSets(pool.getHandle(),new DeviceHandle(device,handle));
        }
    }
}
