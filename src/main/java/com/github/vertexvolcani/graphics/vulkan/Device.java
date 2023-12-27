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
import jakarta.annotation.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.VK_ERROR_OUT_OF_POOL_MEMORY;

// Code adapted from https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/vulkan/HelloVulkan.java
public class Device extends LibCleanable {
    private final VkDevice device;
    private final VkPhysicalDevice physical_device;
    private final VkPhysicalDeviceProperties properties = VkPhysicalDeviceProperties.calloc();
    private final boolean debug;
    private int graphics_index;
    private int result = VK_SUCCESS;

    public Device(@Nonnull Instance instance) {
        debug = instance.getDebug();
        Log.print(Log.Severity.DEBUG, "Vulkan: creating Device...");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer handle = stack.mallocPointer(1);
            IntBuffer ip = stack.callocInt(1);
            if (vkEnumeratePhysicalDevices(instance.getInstance(), ip, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to enumerate physical devices");
                throw new IllegalStateException("failed to enumerate physical devices");
            }

            if (ip.get(0) > 0) {
                PointerBuffer physical_devices = stack.mallocPointer(ip.get(0));
                if (vkEnumeratePhysicalDevices(instance.getInstance(), ip, physical_devices) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: failed to enumerate physical devices");
                    throw new IllegalStateException("failed to enumerate physical devices");
                }
                physical_device = selectPhysicalDevice(instance.getInstance());
                Log.print(Log.Severity.DEBUG, "Vulkan: physical device chosen and retrieved");
            } else {
                Log.print(Log.Severity.ERROR, "Vulkan: no physical devices vulkan compatible");
                throw new IllegalStateException("vkEnumeratePhysicalDevices reported zero accessible devices.");
            }

            /* Look for device extensions */
            boolean found_swapchain = false;
            if (vkEnumerateDeviceExtensionProperties(physical_device, (String) null, ip, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to enumerate physical devices extension properties");
                throw new IllegalStateException("failed to enumerate physical devices  extension properties");
            }

            PointerBuffer extension_names = stack.mallocPointer(64);
            ByteBuffer KHR_swapchain = stack.ASCII(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            if (ip.get(0) > 0) {
                VkExtensionProperties.Buffer device_extensions = VkExtensionProperties.malloc(ip.get(0), stack);
                if (vkEnumerateDeviceExtensionProperties(physical_device, (String) null, ip, device_extensions) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR, "Vulkan: failed to enumerate physical devices extension properties");
                    throw new IllegalStateException("failed to enumerate physical devices  extension properties");
                }

                for (int i = 0; i < ip.get(0); i++) {
                    device_extensions.position(i);
                    if (VK_KHR_SWAPCHAIN_EXTENSION_NAME.equals(device_extensions.extensionNameString())) {
                        found_swapchain = true;
                        extension_names.put(KHR_swapchain);
                    }
                }
            }

            if (!found_swapchain) {
                throw new IllegalStateException("vkEnumerateDeviceExtensionProperties failed to find the " + VK_KHR_SWAPCHAIN_EXTENSION_NAME + " extension.");
            }


            vkGetPhysicalDeviceQueueFamilyProperties(physical_device, ip, null);

            VkQueueFamilyProperties.Buffer queue_family_properties = VkQueueFamilyProperties.malloc(ip.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physical_device, ip, queue_family_properties);
            if (ip.get(0) == 0) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to get physical device queue family properties");
                throw new IllegalStateException("failed to get physical device queue family properties");
            }

            VkPhysicalDeviceFeatures gpu_features = VkPhysicalDeviceFeatures.malloc(stack);
            vkGetPhysicalDeviceFeatures(physical_device, gpu_features);
            graphics_index = Integer.MAX_VALUE;
            int i = 0;
            for (var queue_family : queue_family_properties) {
                if ((queue_family.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    graphics_index = i;
                    break;
                }
                i++;
            }

            if (graphics_index == Integer.MAX_VALUE) {
                Log.print(Log.Severity.ERROR, "Vulkan: failed to find a graphics queue index");
                throw new IllegalStateException("Could not find a graphics queue index");
            }

            VkDeviceQueueCreateInfo.Buffer queue = VkDeviceQueueCreateInfo.malloc(1, stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .queueFamilyIndex(graphics_index)
                    .pQueuePriorities(stack.floats(0.0f));

            VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.calloc(stack);
            if (gpu_features.shaderClipDistance()) {
                features.shaderClipDistance(true);
            }


            extension_names.flip();
            VkDeviceCreateInfo pCreateInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queue)
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(extension_names)
                    .pEnabledFeatures(features);

            vkGetPhysicalDeviceProperties(physical_device, properties);

            // Create Vulkan device
            if (vkCreateDevice(physical_device, pCreateInfo, null, handle) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan Error: could not make Vulkan device");
                throw new RuntimeException("could not make Vulkan device");
            }
            device = new VkDevice(handle.get(0), physical_device, pCreateInfo);
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: device setup done");
    }

    private static VkPhysicalDevice selectPhysicalDevice(@Nonnull VkInstance instance) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Enumerate physical devices
            IntBuffer pPhysicalDeviceCount = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, null);

            if (pPhysicalDeviceCount.get(0) == 0) {
                throw new RuntimeException("No Vulkan-compatible physical devices found.");
            }

            PointerBuffer pPhysicalDevices = stack.mallocPointer(pPhysicalDeviceCount.get(0));
            vkEnumeratePhysicalDevices(instance, pPhysicalDeviceCount, pPhysicalDevices);

            VkPhysicalDevice selectedDevice = null;
            int selectedDeviceType = Integer.MAX_VALUE;

            // Iterate through physical devices and select the one with the desired properties
            for (int i = 0; i < pPhysicalDeviceCount.get(0); i++) {
                VkPhysicalDevice currentDevice = new VkPhysicalDevice(pPhysicalDevices.get(i), instance);
                int currentDeviceType = getDeviceType(currentDevice);

                if (currentDeviceType < selectedDeviceType) {
                    selectedDevice = currentDevice;
                    selectedDeviceType = currentDeviceType;
                }
            }

            return selectedDevice;
        }
    }

    private static int getDeviceType(@Nonnull VkPhysicalDevice physicalDevice) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.calloc(stack);
            vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);

            // Prioritize integrated GPUs over CPU-based and dedicated GPUs over integrated ones
            if (deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU) {
                return 0; // Integrated GPU
            } else if (deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                return 1; // Dedicated GPU
            } else {
                return 2; // Other (CPU-based, etc.)
            }
        }
    }

    public int getResult() {
        return result;
    }

    public boolean didErrorOccur() {
        return result != VK_SUCCESS;
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physical_device;
    }

    public int getGraphicsIndex() {
        return graphics_index;
    }

    public VkQueue getDeviceQueue(int queueFamilyIndex, int queueIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, queueFamilyIndex, queueIndex, pQueue);
            return new VkQueue(pQueue.get(0), device);
        }
    }

    public void flushMappedMemoryRanges(@NativeType("VkMappedMemoryRange const *") VkMappedMemoryRange.Buffer pMemoryRanges) {
        result = vkFlushMappedMemoryRanges(device, pMemoryRanges);
    }

    public void invalidateMappedMemoryRanges(@NativeType("VkMappedMemoryRange const *") VkMappedMemoryRange.Buffer pMemoryRanges) {
        result = vkInvalidateMappedMemoryRanges(device, pMemoryRanges);
    }

    public void invalidateMappedMemoryRanges(@NativeType("VkMappedMemoryRange const *") VkMappedMemoryRange pMemoryRange) {
        result = vkInvalidateMappedMemoryRanges(device, pMemoryRange);
    }

    public void getBufferMemoryRequirements(DeviceHandle buffer, @NativeType("VkMemoryRequirements *") VkMemoryRequirements pMemoryRequirements) {
        vkGetBufferMemoryRequirements(device, buffer.handle(), pMemoryRequirements);
    }

    public void getImageMemoryRequirements(DeviceHandle image, @NativeType("VkMemoryRequirements *") VkMemoryRequirements pMemoryRequirements) {
        vkGetImageMemoryRequirements(device, image.handle(), pMemoryRequirements);
    }

    public DeviceHandle createFence(@NativeType("VkFenceCreateInfo const *") VkFenceCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateFence(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));
        }
    }

    public void destroyFence(DeviceHandle fence) {
        vkDestroyFence(device, fence.handle(), null);
    }

    public void resetFences(DeviceHandle pFences) {
        result = vkResetFences(device, pFences.handle());
    }

    public void getFenceStatus(DeviceHandle fence) {
        result = vkGetFenceStatus(device, fence.handle());
    }

    public void waitForFences(DeviceHandle pFences, boolean waitAll, @NativeType("uint64_t") long timeout) {
        result = vkWaitForFences(device, pFences.handle(), waitAll, timeout);
    }

    public DeviceHandle createSemaphore(@NativeType("VkSemaphoreCreateInfo const *") VkSemaphoreCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateSemaphore(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));
        }
    }

    public void destroySemaphore(DeviceHandle semaphore) {
        vkDestroySemaphore(device, semaphore.handle(), null);
    }

    public DeviceHandle createEvent(@NativeType("VkEventCreateInfo const *") VkEventCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateEvent(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyEvent(DeviceHandle event) {
        vkDestroyEvent(device, event.handle(), null);
    }

    public void getEventStatus(DeviceHandle event) {
        result = vkGetEventStatus(device, event.handle());
    }

    public void setEvent(DeviceHandle event) {
        result = vkSetEvent(device, event.handle());
    }

    public void resetEvent(DeviceHandle event) {
        result = vkResetEvent(device, event.handle());
    }

    public DeviceHandle createQueryPool(@NativeType("VkQueryPoolCreateInfo const *") VkQueryPoolCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateQueryPool(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyQueryPool(DeviceHandle queryPool) {
        vkDestroyQueryPool(device, queryPool.handle(), null);
    }

    public void getQueryPoolResults(DeviceHandle queryPool, int firstQuery, int queryCount, @NativeType("void *") ByteBuffer pData, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        result = vkGetQueryPoolResults(device, queryPool.handle(), firstQuery, queryCount, pData, stride, flags);
    }

    public void getQueryPoolResults(DeviceHandle queryPool, int firstQuery, int queryCount, @NativeType("void *") int[] pData, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        result = vkGetQueryPoolResults(device, queryPool.handle(), firstQuery, queryCount, pData, stride, flags);
    }

    public DeviceHandle createBufferView(@NativeType("VkBufferViewCreateInfo const *") VkBufferViewCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateBufferView(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyBufferView(DeviceHandle bufferView) {
        vkDestroyBufferView(device, bufferView.handle(), null);
    }

    public DeviceHandle createImage(@NativeType("VkImageCreateInfo const *") VkImageCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateImage(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyImage(DeviceHandle image) {
        vkDestroyImage(device, image.handle(), null);
    }

    public void getImageSubresourceLayout(DeviceHandle image, @NativeType("VkImageSubresource const *") VkImageSubresource pSubresource, @NativeType("VkSubresourceLayout *") VkSubresourceLayout pLayout) {
        vkGetImageSubresourceLayout(device, image.handle(), pSubresource, pLayout);
    }

    public DeviceHandle createImageView(@NativeType("VkImageViewCreateInfo const *") VkImageViewCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateImageView(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyImageView(DeviceHandle imageView) {
        vkDestroyImageView(device, imageView.handle(), null);
    }

    public DeviceHandle createShaderModule(@NativeType("VkShaderModuleCreateInfo const *") VkShaderModuleCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateShaderModule(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyShaderModule(DeviceHandle shaderModule) {
        vkDestroyShaderModule(device, shaderModule.handle(), null);
    }

    public DeviceHandle createPipelineCache(@NativeType("VkPipelineCacheCreateInfo const *") VkPipelineCacheCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreatePipelineCache(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyPipelineCache(DeviceHandle pipelineCache) {
        vkDestroyPipelineCache(device, pipelineCache.handle(), null);
    }

    public void getPipelineCacheData(DeviceHandle pipelineCache, @NativeType("size_t *") PointerBuffer pDataSize, @Nullable @NativeType("void *") ByteBuffer pData) {
        result = vkGetPipelineCacheData(device, pipelineCache.handle(), pDataSize, pData);
    }

    public void mergePipelineCaches(DeviceHandle dstCache, @NativeType("VkPipelineCache const *") @Nonnull long[] pSrcCaches) {
        result = vkMergePipelineCaches(device, dstCache.handle(), pSrcCaches);
    }

    public void createGraphicsPipelines(@Nullable DeviceHandle pipelineCache, @NativeType("VkGraphicsPipelineCreateInfo const *") VkGraphicsPipelineCreateInfo.Buffer pCreateInfos, long[] handle) {
        if (isDebug()) {
            //If the flags member of any element of pCreateInfos contains the VK_PIPELINE_CREATE_DERIVATIVE_BIT flag, and the basePipelineIndex member of that same element is not -1, basePipelineIndex must be less than the index into pCreateInfos that corresponds to that element
            if (pCreateInfos.get(0).flags() == VK_PIPELINE_CREATE_DERIVATIVE_BIT && pCreateInfos.get(0).basePipelineIndex() != -1) {
                Log.print(Log.Severity.ERROR, "Vulkan: basePipelineIndex must be less than the index into pCreateInfos that corresponds to that element");
                throw new IllegalStateException("basePipelineIndex must be less than the index into pCreateInfos that corresponds to that element");
            }

            //If the flags member of any element of pCreateInfos contains the VK_PIPELINE_CREATE_DERIVATIVE_BIT flag, the base pipeline must have been created with the VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT flag set
            if (pCreateInfos.get(0).flags() == VK_PIPELINE_CREATE_DERIVATIVE_BIT && pCreateInfos.get(0).basePipelineHandle() != VK_NULL_HANDLE) {
                Log.print(Log.Severity.ERROR, "Vulkan: base pipeline must have been created with the VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT flag set");
                throw new IllegalStateException("base pipeline must have been created with the VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT flag set");
            }

            //If pipelineCache was created with VK_PIPELINE_CACHE_CREATE_EXTERNALLY_SYNCHRONIZED_BIT, host access to pipelineCache must be externally synchronized
            if (pipelineCache != null) {
                if (pipelineCache.handle() != VK_NULL_HANDLE && (pCreateInfos.get(0).flags() & 0x00000001) == 0x00000001) {
                    Log.print(Log.Severity.ERROR, "Vulkan: host access to pipelineCache must be externally synchronized");
                    throw new IllegalStateException("host access to pipelineCache must be externally synchronized");
                }
            }
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(pCreateInfos.remaining());
            result = vkCreateGraphicsPipelines(device, pipelineCache == null ? VK_NULL_HANDLE : pipelineCache.handle(), pCreateInfos, null, pBuffer);
            for (int i = 0; i < pBuffer.remaining(); i++) {
                handle[i] = pBuffer.get(i);
            }

        }
    }

    public void createComputePipelines(@Nullable DeviceHandle pipelineCache, @NativeType("VkComputePipelineCreateInfo const *") VkComputePipelineCreateInfo.Buffer pCreateInfos, long[] handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(pCreateInfos.remaining());
            result = vkCreateComputePipelines(device, pipelineCache == null ? VK_NULL_HANDLE : pipelineCache.handle(), pCreateInfos, null, pBuffer);
            for (int i = 0; i < pBuffer.remaining(); i++) {
                handle[i] = pBuffer.get(i);
            }
        }
    }

    public void destroyPipeline(DeviceHandle pipeline) {
        vkDestroyPipeline(device, pipeline.handle(), null);
    }

    public DeviceHandle createPipelineLayout(@NativeType("VkPipelineLayoutCreateInfo const *") VkPipelineLayoutCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreatePipelineLayout(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyPipelineLayout(DeviceHandle pipelineLayout) {
        vkDestroyPipelineLayout(device, pipelineLayout.handle(), null);
    }

    public DeviceHandle createSampler(@NativeType("VkSamplerCreateInfo const *") VkSamplerCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateSampler(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroySampler(DeviceHandle sampler) {
        vkDestroySampler(device, sampler.handle(), null);
    }

    public DeviceHandle createDescriptorSetLayout(@NativeType("VkDescriptorSetLayoutCreateInfo const *") VkDescriptorSetLayoutCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateDescriptorSetLayout(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyDescriptorSetLayout(DeviceHandle descriptorSetLayout) {
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout.handle(), null);
    }

    public DeviceHandle createDescriptorPool(@NativeType("VkDescriptorPoolCreateInfo const *") VkDescriptorPoolCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateDescriptorPool(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyDescriptorPool(DeviceHandle descriptorPool) {
        vkDestroyDescriptorPool(device, descriptorPool.handle(), null);
    }

    public void resetDescriptorPool(DeviceHandle descriptorPool, int flags) {
        result = vkResetDescriptorPool(device, descriptorPool.handle(), flags);
    }

    public void allocateDescriptorSets(@NativeType("VkDescriptorSetAllocateInfo const *") VkDescriptorSetAllocateInfo pCreateInfo, long[] handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(pCreateInfo.descriptorSetCount());
            result = vkAllocateDescriptorSets(device, pCreateInfo, pBuffer);
            if(result == VK_ERROR_OUT_OF_POOL_MEMORY) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to allocate descriptor sets due to out of pool memory");
                throw new IllegalStateException("Failed to allocate descriptor sets due to out of pool memory");
            }
            for (int i = 0; i < pCreateInfo.descriptorSetCount(); i++) {
                handle[i] = pBuffer.get(i);
            }

        }
    }

    public void freeDescriptorSets(DeviceHandle descriptorPool, DeviceHandle pDescriptorSets) {
        result = vkFreeDescriptorSets(device, descriptorPool.handle(), pDescriptorSets.handle());
    }

    public void updateDescriptorSets(@Nonnull @NativeType("VkWriteDescriptorSet const *") VkWriteDescriptorSet.Buffer pDescriptorWrites, @Nullable @NativeType("VkCopyDescriptorSet const *") VkCopyDescriptorSet.Buffer pDescriptorCopies) {
        vkUpdateDescriptorSets(device, pDescriptorWrites, pDescriptorCopies);
    }

    public DeviceHandle createFramebuffer(@NativeType("VkFramebufferCreateInfo const *") VkFramebufferCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateFramebuffer(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyFramebuffer(DeviceHandle framebuffer) {
        vkDestroyFramebuffer(device, framebuffer.handle(), null);
    }

    public DeviceHandle createRenderPass(@NativeType("VkRenderPassCreateInfo const *") VkRenderPassCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateRenderPass(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));
        }
    }

    public void destroyRenderPass(DeviceHandle renderPass) {
        vkDestroyRenderPass(device, renderPass.handle(), null);
    }

    public void getRenderAreaGranularity(DeviceHandle renderPass, @NativeType("VkExtent2D *") VkExtent2D pGranularity) {
        vkGetRenderAreaGranularity(device, renderPass.handle(), pGranularity);
    }

    public DeviceHandle createCommandPool(@NativeType("VkCommandPoolCreateInfo const *") VkCommandPoolCreateInfo pCreateInfo) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            result = vkCreateCommandPool(device, pCreateInfo, null, pBuffer);
            return new DeviceHandle(this, pBuffer.get(0));

        }
    }

    public void destroyCommandPool(DeviceHandle commandPool) {
        vkDestroyCommandPool(device, commandPool.handle(), null);
    }

    public void resetCommandPool(DeviceHandle commandPool, int flags) {
        result = vkResetCommandPool(device, commandPool.handle(), flags);
    }

    public void allocateCommandBuffers(@NativeType("VkCommandBufferAllocateInfo const *") VkCommandBufferAllocateInfo pAllocateInfo, @NativeType("VkCommandBuffer *") PointerBuffer pCommandBuffers) {
        result = vkAllocateCommandBuffers(device, pAllocateInfo, pCommandBuffers);
    }

    public void freeCommandBuffers(DeviceHandle commandPool, @NativeType("VkCommandBuffer const *") VkCommandBuffer pCommandBuffers) {
        vkFreeCommandBuffers(device, commandPool.handle(), pCommandBuffers);
    }

    public void waitIdle() {
        result = vkDeviceWaitIdle(device);
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    protected final void free() {
        waitIdle();
        properties.free();
        vkDestroyDevice(device, null);
        Log.print(Log.Severity.DEBUG, "Vulkan: device free memory done");
    }

    public VkPhysicalDeviceLimits getLimits() {
        return properties.limits();
    }
}
