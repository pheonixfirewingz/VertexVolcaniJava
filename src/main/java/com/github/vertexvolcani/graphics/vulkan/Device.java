package com.github.vertexvolcani.graphics.vulkan;

import com.github.vertexvolcani.message.IListener;
import com.github.vertexvolcani.message.events.IEvent;
import com.github.vertexvolcani.util.CleanerObject;
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

// Code adapted from https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/vulkan/HelloVulkan.java
public class Device extends CleanerObject {
    private final VkDevice device;
    private final VkPhysicalDevice physical_device;
    private int graphics_index;
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
                VkPhysicalDevice currentDevice = new VkPhysicalDevice(pPhysicalDevices.get(i),instance);
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

    public Device(@Nonnull Instance instance) {
        super();
        Log.print(Log.Severity.DEBUG, "Vulkan: creating Device...");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer handle = stack.mallocPointer(1);
            IntBuffer ip = stack.callocInt(1);
            if(vkEnumeratePhysicalDevices(instance.getInstance(), ip, null)!= VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: failed to enumerate physical devices");
                throw new IllegalStateException("failed to enumerate physical devices");
            }

            if (ip.get(0) > 0) {
                PointerBuffer physical_devices = stack.mallocPointer(ip.get(0));
                if(vkEnumeratePhysicalDevices(instance.getInstance(), ip, physical_devices) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR,"Vulkan: failed to enumerate physical devices");
                    throw new IllegalStateException("failed to enumerate physical devices");
                }
                physical_device = selectPhysicalDevice(instance.getInstance());
                Log.print(Log.Severity.DEBUG, "Vulkan: physical device chosen and retrieved");
            } else {
                Log.print(Log.Severity.ERROR,"Vulkan: no physical devices vulkan compatible");
                throw new IllegalStateException("vkEnumeratePhysicalDevices reported zero accessible devices.");
            }

            /* Look for device extensions */
            boolean found_swapchain = false;
            if(vkEnumerateDeviceExtensionProperties(physical_device, (String)null, ip, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan: failed to enumerate physical devices extension properties");
                throw new IllegalStateException("failed to enumerate physical devices  extension properties");
            }

            PointerBuffer extension_names = stack.mallocPointer(64);
            ByteBuffer KHR_swapchain   = stack.ASCII(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
            if (ip.get(0) > 0) {
                VkExtensionProperties.Buffer device_extensions = VkExtensionProperties.malloc(ip.get(0), stack);
                if(vkEnumerateDeviceExtensionProperties(physical_device, (String)null, ip, device_extensions) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR,"Vulkan: failed to enumerate physical devices extension properties");
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

            VkQueueFamilyProperties.Buffer queue_family_properties = VkQueueFamilyProperties.malloc(ip.get(0),stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physical_device, ip, queue_family_properties);
            if (ip.get(0) == 0) {
                Log.print(Log.Severity.ERROR,"Vulkan: failed to get physical device queue family properties");
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
                Log.print(Log.Severity.ERROR,"Vulkan: failed to find a graphics queue index");
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
            VkDeviceCreateInfo pCreateInfo = VkDeviceCreateInfo.malloc(stack)
                    .sType$Default()
                    .pNext(NULL)
                    .flags(0)
                    .pQueueCreateInfos(queue)
                    .ppEnabledLayerNames(null)
                    .ppEnabledExtensionNames(extension_names)
                    .pEnabledFeatures(features);

            // Create Vulkan device
            if (vkCreateDevice(physical_device, pCreateInfo, null, handle) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan Error: could not make Vulkan device");
                throw new RuntimeException("could not make Vulkan device");
            }
            device = new VkDevice(handle.get(0), physical_device, pCreateInfo);
        }
        Log.print(Log.Severity.DEBUG, "Vulkan: device setup done");
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

    public VkQueue getDeviceQueue(@NativeType("uint32_t") int queueFamilyIndex, @NativeType("uint32_t") int queueIndex) {
        VkQueue queue = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pQueue = stack.mallocPointer(1);
            vkGetDeviceQueue(device, queueFamilyIndex, queueIndex, pQueue);
            queue = new VkQueue(pQueue.get(0),device);
        }
        return queue;
    }

    @NativeType("VkResult")
    public int flushMappedMemoryRanges(@NativeType("VkMappedMemoryRange const *") VkMappedMemoryRange.Buffer pMemoryRanges){
        return vkFlushMappedMemoryRanges(device,pMemoryRanges);
    }

    @NativeType("VkResult")
    public int invalidateMappedMemoryRanges(@NativeType("VkMappedMemoryRange const *") VkMappedMemoryRange.Buffer pMemoryRanges) {
        return vkInvalidateMappedMemoryRanges(device,pMemoryRanges);
    }

    @NativeType("VkResult")
    public int invalidateMappedMemoryRanges(@NativeType("VkMappedMemoryRange const *") VkMappedMemoryRange pMemoryRange) {
        return vkInvalidateMappedMemoryRanges(device,pMemoryRange);
    }
    public void getBufferMemoryRequirements(@NativeType("VkBuffer") long buffer, @NativeType("VkMemoryRequirements *") VkMemoryRequirements pMemoryRequirements) {
        vkGetBufferMemoryRequirements(device,buffer,pMemoryRequirements);
    }

    public void getImageMemoryRequirements(@NativeType("VkImage") long image, @NativeType("VkMemoryRequirements *") VkMemoryRequirements pMemoryRequirements) {
        vkGetImageMemoryRequirements(device,image,pMemoryRequirements);
    }

    public void getImageSparseMemoryRequirements(@NativeType("VkImage") long image, @NativeType("uint32_t *") IntBuffer pSparseMemoryRequirementCount, @Nullable @NativeType("VkSparseImageMemoryRequirements *") VkSparseImageMemoryRequirements.Buffer pSparseMemoryRequirements) {
        vkGetImageSparseMemoryRequirements(device,image,pSparseMemoryRequirementCount,pSparseMemoryRequirements);
    }
    @NativeType("VkResult")
    public int createFence(@NativeType("VkFenceCreateInfo const *") VkFenceCreateInfo pCreateInfo, @NativeType("VkFence *") @Nonnull LongBuffer pFence) {
        return vkCreateFence(device,pCreateInfo,null,pFence);
    }

    public void destroyFence(@NativeType("VkFence") long fence) {
        vkDestroyFence(device,fence,null);
    }

    @NativeType("VkResult")
    public int resetFences(@NativeType("VkFence const *") long pFences) {
        return vkResetFences(device,pFences);
    }

    @NativeType("VkResult")
    public int getFenceStatus(@NativeType("VkFence") long fence) {
        return vkGetFenceStatus(device,fence);
    }

    @NativeType("VkResult")
    public int waitForFences(@NativeType("VkFence const *") long pFences, @NativeType("VkBool32") boolean waitAll, @NativeType("uint64_t") long timeout) {
        return vkWaitForFences(device,pFences,waitAll,timeout);
    }

    @NativeType("VkResult")
    public int createSemaphore(@NativeType("VkSemaphoreCreateInfo const *") VkSemaphoreCreateInfo pCreateInfo, @NativeType("VkSemaphore *") @Nonnull LongBuffer pSemaphore) {
        return vkCreateSemaphore(device,pCreateInfo,null,pSemaphore);
    }

    public void destroySemaphore(@NativeType("VkSemaphore") long semaphore) {
        vkDestroySemaphore(device,semaphore,null);
    }

    @NativeType("VkResult")
    public int createEvent(@NativeType("VkEventCreateInfo const *") VkEventCreateInfo pCreateInfo, @NativeType("VkEvent *") @Nonnull LongBuffer pEvent) {
        return vkCreateEvent(device,pCreateInfo,null,pEvent);
    }

    public void destroyEvent(@NativeType("VkEvent") long event) {
        vkDestroyEvent(device,event,null);
    }

    @NativeType("VkResult")
    public int getEventStatus(@NativeType("VkEvent") long event) {
        return vkGetEventStatus(device,event);
    }

    @NativeType("VkResult")
    public int setEvent(@NativeType("VkEvent") long event) {
        return vkSetEvent(device,event);
    }

    @NativeType("VkResult")
    public int resetEvent(@NativeType("VkEvent") long event) {
        return vkResetEvent(device,event);
    }

    @NativeType("VkResult")
    public int createQueryPool(@NativeType("VkQueryPoolCreateInfo const *") VkQueryPoolCreateInfo pCreateInfo,@NativeType("VkQueryPool *") @Nonnull LongBuffer pQueryPool) {
        return vkCreateQueryPool(device,pCreateInfo,null,pQueryPool);
    }

    public void destroyQueryPool(@NativeType("VkQueryPool") long queryPool) {
        vkDestroyQueryPool(device,queryPool,null);
    }

    @NativeType("VkResult")
    public int getQueryPoolResults(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int firstQuery, @NativeType("uint32_t") int queryCount, @NativeType("void *") ByteBuffer pData, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        return vkGetQueryPoolResults(device,queryPool,firstQuery,queryCount,pData,stride,flags);
    }

    @NativeType("VkResult")
    public int getQueryPoolResults(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int firstQuery, @NativeType("uint32_t") int queryCount, @NativeType("void *") LongBuffer pData, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        return vkGetQueryPoolResults(device,queryPool,firstQuery,queryCount,pData,stride,flags);
    }

    @NativeType("VkResult")
    public int getQueryPoolResults(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int firstQuery, @NativeType("uint32_t") int queryCount, @NativeType("void *") IntBuffer pData, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        return vkGetQueryPoolResults(device,queryPool,firstQuery,queryCount,pData,stride,flags);
    }

    @NativeType("VkResult")
    public int getQueryPoolResults(@NativeType("VkQueryPool") long queryPool, @NativeType("uint32_t") int firstQuery, @NativeType("uint32_t") int queryCount, @NativeType("void *") int[] pData, @NativeType("VkDeviceSize") long stride, @NativeType("VkQueryResultFlags") int flags) {
        return vkGetQueryPoolResults(device,queryPool,firstQuery,queryCount,pData,stride,flags);
    }

    @NativeType("VkResult")
    public int createBufferView(@NativeType("VkBufferViewCreateInfo const *") VkBufferViewCreateInfo pCreateInfo, @NativeType("VkBufferView *") @Nonnull LongBuffer pView) {
        return vkCreateBufferView(device,pCreateInfo,null,pView);
    }

    public void destroyBufferView(@NativeType("VkBufferView") long bufferView) {
        vkDestroyBufferView(device,bufferView,null);
    }

    @NativeType("VkResult")
    public int createImage(@NativeType("VkImageCreateInfo const *") VkImageCreateInfo pCreateInfo,@NativeType("VkImage *") @Nonnull LongBuffer pImage) {
        return vkCreateImage(device,pCreateInfo,null,pImage);
    }
    
    public void destroyImage(@NativeType("VkImage") long image) {
        vkDestroyImage(device,image,null);
    }

    public void getImageSubresourceLayout(@NativeType("VkImage") long image, @NativeType("VkImageSubresource const *") VkImageSubresource pSubresource, @NativeType("VkSubresourceLayout *") VkSubresourceLayout pLayout) {
        vkGetImageSubresourceLayout(device,image,pSubresource,pLayout);
    }

    @NativeType("VkResult")
    public int createImageView(@NativeType("VkImageViewCreateInfo const *") VkImageViewCreateInfo pCreateInfo, @NativeType("VkImageView *") @Nonnull LongBuffer pView) {
        return vkCreateImageView(device,pCreateInfo,null,pView);
    }

    public void destroyImageView(@NativeType("VkImageView") long imageView) {
        vkDestroyImageView(device,imageView,null);
    }

    @NativeType("VkResult")
    public int createShaderModule(@NativeType("VkShaderModuleCreateInfo const *") VkShaderModuleCreateInfo pCreateInfo, @NativeType("VkShaderModule *") @Nonnull LongBuffer pShaderModule) {
        return vkCreateShaderModule(device,pCreateInfo,null,pShaderModule);
    }

    public void destroyShaderModule(@NativeType("VkShaderModule") long shaderModule) {
        vkDestroyShaderModule(device,shaderModule,null);
    }

    @NativeType("VkResult")
    public int createPipelineCache(@NativeType("VkPipelineCacheCreateInfo const *") VkPipelineCacheCreateInfo pCreateInfo, @NativeType("VkPipelineCache *") @Nonnull LongBuffer pPipelineCache) {
        return vkCreatePipelineCache(device,pCreateInfo,null,pPipelineCache);
    }

    public void destroyPipelineCache(@NativeType("VkPipelineCache") long pipelineCache) {
        vkDestroyPipelineCache(device, pipelineCache, null);
    }

    @NativeType("VkResult")
    public int getPipelineCacheData(@NativeType("VkPipelineCache") long pipelineCache, @NativeType("size_t *") PointerBuffer pDataSize, @Nullable @NativeType("void *") ByteBuffer pData) {
        return vkGetPipelineCacheData(device,pipelineCache,pDataSize,pData);
    }

    @NativeType("VkResult")
    public int mergePipelineCaches(@NativeType("VkPipelineCache") long dstCache, @NativeType("VkPipelineCache const *") @Nonnull LongBuffer pSrcCaches) {
        return vkMergePipelineCaches(device,dstCache,pSrcCaches);
    }

    @NativeType("VkResult")
    public int createGraphicsPipelines(@NativeType("VkPipelineCache") long pipelineCache, @NativeType("VkGraphicsPipelineCreateInfo const *") VkGraphicsPipelineCreateInfo.Buffer pCreateInfos, @NativeType("VkPipeline *") @Nonnull LongBuffer pPipelines) {
        return vkCreateGraphicsPipelines(device,pipelineCache,pCreateInfos,null,pPipelines);
    }

    @NativeType("VkResult")
    public int createComputePipelines(@NativeType("VkPipelineCache") long pipelineCache, @NativeType("VkComputePipelineCreateInfo const *") VkComputePipelineCreateInfo.Buffer pCreateInfos, @NativeType("VkPipeline *") @Nonnull LongBuffer pPipelines) {
        return vkCreateComputePipelines(device,pipelineCache,pCreateInfos,null,pPipelines);
    }

    public void destroyPipeline(@NativeType("VkPipeline") long pipeline) {
        vkDestroyPipeline(device,pipeline,null);
    }

    @NativeType("VkResult")
    public int createPipelineLayout(@NativeType("VkPipelineLayoutCreateInfo const *") VkPipelineLayoutCreateInfo pCreateInfo, @NativeType("VkPipelineLayout *") @Nonnull LongBuffer pPipelineLayout) {
        return vkCreatePipelineLayout(device,pCreateInfo,null,pPipelineLayout);
    }

    public void destroyPipelineLayout(@NativeType("VkPipelineLayout") long pipelineLayout) {
        vkDestroyPipelineLayout(device,pipelineLayout,null);
    }

    @NativeType("VkResult")
    public int createSampler(@NativeType("VkSamplerCreateInfo const *") VkSamplerCreateInfo pCreateInfo, @NativeType("VkSampler *") @Nonnull LongBuffer pSampler) {
        return vkCreateSampler(device,pCreateInfo,null,pSampler);
    }

    public void destroySampler(@NativeType("VkSampler") long sampler) {
        vkDestroySampler(device,sampler,null);
    }

    @NativeType("VkResult")
    public int createDescriptorSetLayout(@NativeType("VkDescriptorSetLayoutCreateInfo const *") VkDescriptorSetLayoutCreateInfo pCreateInfo, @NativeType("VkDescriptorSetLayout *") @Nonnull LongBuffer pSetLayout) {
        return vkCreateDescriptorSetLayout(device,pCreateInfo,null,pSetLayout);
    }

    public void destroyDescriptorSetLayout(@NativeType("VkDescriptorSetLayout") long descriptorSetLayout) {
        vkDestroyDescriptorSetLayout(device,descriptorSetLayout,null);
    }

    @NativeType("VkResult")
    public int createDescriptorPool(@NativeType("VkDescriptorPoolCreateInfo const *") VkDescriptorPoolCreateInfo pCreateInfo, @NativeType("VkDescriptorPool *") @Nonnull LongBuffer pDescriptorPool) {
        return vkCreateDescriptorPool(device,pCreateInfo,null,pDescriptorPool);
    }

    public void destroyDescriptorPool(@NativeType("VkDescriptorPool") long descriptorPool) {
        vkDestroyDescriptorPool(device,descriptorPool,null);
    }

    @NativeType("VkResult")
    public int resetDescriptorPool(@NativeType("VkDescriptorPool") long descriptorPool, @NativeType("VkDescriptorPoolResetFlags") int flags) {
        return vkResetDescriptorPool(device,descriptorPool,flags);
    }

    @NativeType("VkResult")
    public int allocateDescriptorSets(@NativeType("VkDescriptorSetAllocateInfo const *") VkDescriptorSetAllocateInfo pAllocateInfo,@NativeType("VkDescriptorSet *") @Nonnull LongBuffer pDescriptorSets) {
        return vkAllocateDescriptorSets(device,pAllocateInfo,pDescriptorSets);
    }

    @NativeType("VkResult")
    public int freeDescriptorSets(@NativeType("VkDescriptorPool") long descriptorPool, @Nullable @NativeType("VkDescriptorSet const *") @Nonnull LongBuffer pDescriptorSets) {
        return vkFreeDescriptorSets(device,descriptorPool,pDescriptorSets);
    }

    @NativeType("VkResult")
    public int freeDescriptorSets(@NativeType("VkDescriptorPool") long descriptorPool, @Nullable @NativeType("VkDescriptorSet const *") long pDescriptorSets) {
        return vkFreeDescriptorSets(device,descriptorPool,pDescriptorSets);
    }

    public void updateDescriptorSets(@Nullable @NativeType("VkWriteDescriptorSet const *") VkWriteDescriptorSet.Buffer pDescriptorWrites, @Nullable @NativeType("VkCopyDescriptorSet const *") VkCopyDescriptorSet.Buffer pDescriptorCopies) {
        vkUpdateDescriptorSets(device,pDescriptorWrites,pDescriptorCopies);
    }

    @NativeType("VkResult")
    public int createFramebuffer(@NativeType("VkFramebufferCreateInfo const *") VkFramebufferCreateInfo pCreateInfo, @NativeType("VkFramebuffer *") @Nonnull LongBuffer pFramebuffer) {
        return vkCreateFramebuffer(device,pCreateInfo,null,pFramebuffer);
    }

    public void destroyFramebuffer(@NativeType("VkFramebuffer") long framebuffer) {
        vkDestroyFramebuffer(device,framebuffer,null);
    }

    @NativeType("VkResult")
    public int createRenderPass(@NativeType("VkRenderPassCreateInfo const *") VkRenderPassCreateInfo pCreateInfo, @NativeType("VkRenderPass *") @Nonnull LongBuffer pRenderPass) {
        return vkCreateRenderPass(device,pCreateInfo,null,pRenderPass);
    }

    public void destroyRenderPass(@NativeType("VkRenderPass") long renderPass) {
        vkDestroyRenderPass(device,renderPass,null);
    }

    public void getRenderAreaGranularity(@NativeType("VkRenderPass") long renderPass, @NativeType("VkExtent2D *") VkExtent2D pGranularity) {
        vkGetRenderAreaGranularity(device,renderPass,pGranularity);
    }

    @NativeType("VkResult")
    public int createCommandPool(@NativeType("VkCommandPoolCreateInfo const *") VkCommandPoolCreateInfo pCreateInfo, @NativeType("VkCommandPool *") @Nonnull LongBuffer pCommandPool) {
        return vkCreateCommandPool(device,pCreateInfo,null,pCommandPool);
    }

    public void destroyCommandPool(@NativeType("VkCommandPool") long commandPool) {
        vkDestroyCommandPool(device,commandPool,null);
    }

    @NativeType("VkResult")
    public int resetCommandPool(@NativeType("VkCommandPool") long commandPool, @NativeType("VkCommandPoolResetFlags") int flags) {
        return vkResetCommandPool(device,commandPool,flags);
    }

    @NativeType("VkResult")
    public int allocateCommandBuffers(@NativeType("VkCommandBufferAllocateInfo const *") VkCommandBufferAllocateInfo pAllocateInfo, @NativeType("VkCommandBuffer *") PointerBuffer pCommandBuffers) {
        return vkAllocateCommandBuffers(device,pAllocateInfo,pCommandBuffers);
    }

    public void freeCommandBuffers(@NativeType("VkCommandPool") long commandPool, @Nullable @NativeType("VkCommandBuffer const *") PointerBuffer pCommandBuffers) {
        vkFreeCommandBuffers(device,commandPool,pCommandBuffers);
    }

    public void freeCommandBuffers(@NativeType("VkCommandPool") long commandPool, @NativeType("VkCommandBuffer const *") VkCommandBuffer pCommandBuffers) {
        vkFreeCommandBuffers(device,commandPool,pCommandBuffers);
    }

    @NativeType("VkResult")
    public int deviceWaitIdle() {
        return vkDeviceWaitIdle(device);
    }

    @Override
    public final void cleanup() {
        deviceWaitIdle();
        vkDestroyDevice(device, null);
        Log.print(Log.Severity.DEBUG, "Vulkan: device free memory done");
    }
}
