package com.github.vertexvolcani.graphics.vulkan;

import com.github.vertexvolcani.util.CleanerObject;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;

import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.VK10.*;

public class VmaAllocator extends CleanerObject {
    private final long handle;
    public VmaAllocator(@Nonnull Instance instance,@Nonnull Device device) {
        super();
        Log.print(Log.Severity.DEBUG,"Vulkan: creating vma allocator...");
        try(MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pAllocator = stack.callocPointer(1);
            VmaVulkanFunctions functions = VmaVulkanFunctions.calloc(stack);
            functions.set(instance.getInstance(),device.getDevice());
            VmaAllocatorCreateInfo pCreateInfo = VmaAllocatorCreateInfo.calloc(stack);
            pCreateInfo.device(device.getDevice());
            pCreateInfo.instance(instance.getInstance());
            pCreateInfo.physicalDevice(device.getPhysicalDevice());
            pCreateInfo.pVulkanFunctions(functions);

            if(vmaCreateAllocator(pCreateInfo,pAllocator) != VK_SUCCESS){
                Log.print(Log.Severity.ERROR,"Vulkan: failed to create vma allocator");
                throw new IllegalStateException("failed to create vma allocator");
            }
            handle = pAllocator.get(0);
        }
        Log.print(Log.Severity.DEBUG,"Vulkan: done creating vma allocator");
    }

    public long getVmaAllocator() {
        return handle;
    }

    @Override
    public void cleanup() {
        if(handle == VK_NULL_HANDLE) {
            return;
        }
        vmaDestroyAllocator(handle);
        Log.print(Log.Severity.DEBUG,"Vulkan: done freeing vma allocator");
    }
}
