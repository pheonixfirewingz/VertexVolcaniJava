package com.github.vertexvolcani.graphics.vulkan;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;

import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.VK10.*;

public class VmaAllocator extends LibCleanable {
    private final DeviceHandle handle;
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
            handle = new DeviceHandle(device,pAllocator.get(0));
        }
        Log.print(Log.Severity.DEBUG,"Vulkan: done creating vma allocator");
    }

    public long getVmaAllocator() {
        return handle.handle();
    }

    @Override
    public final void free() {
        vmaDestroyAllocator(handle.handle());
        Log.print(Log.Severity.DEBUG,"Vulkan: done freeing vma allocator");
    }

    public Device getDev() {
        return handle.device();
    }
}
