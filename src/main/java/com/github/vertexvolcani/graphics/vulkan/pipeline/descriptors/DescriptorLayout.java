package com.github.vertexvolcani.graphics.vulkan.pipeline.descriptors;

import com.github.vertexvolcani.graphics.vulkan.Device;
import com.github.vertexvolcani.graphics.vulkan.DeviceHandle;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

public final class DescriptorLayout extends LibCleanable {

    private final DeviceHandle handle;
    public DescriptorLayout(Device device_in,LayoutBinding [] bindings_in,int flags) {
        try(VkDescriptorSetLayoutCreateInfo pCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc()) {
            try(VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindings_in.length)) {
                for (int i = 0; i < bindings_in.length; i++) {
                    bindings.get(i).binding(bindings_in[i].binding()).descriptorType(bindings_in[i].descriptorType().getDescriptorType())
                            .descriptorCount(bindings_in[i].descriptorCount()).stageFlags(bindings_in[i].stageFlags().getValue())
                            .pImmutableSamplers(bindings_in[i].pImmutableSamplers());
                }
                pCreateInfo.sType$Default();
                pCreateInfo.flags(flags);
                pCreateInfo.pBindings(bindings);
                handle = device_in.createDescriptorSetLayout(pCreateInfo);
                if (device_in.didErrorOccur()) {
                    Log.print(Log.Severity.ERROR, "Vulkan: could not create descriptor set layout");
                    throw new IllegalStateException("could not create descriptor set layout");
                }
            }
        }
        Log.print(Log.Severity.DEBUG, "Vulkan:created descriptor set layout");
    }
    public DeviceHandle getHandle() {
        return handle;
    }

    @Override
    protected void free() {
        handle.device().destroyDescriptorSetLayout(handle);
        Log.print(Log.Severity.DEBUG, "Vulkan: done freeing descriptor set layout");
    }
}
