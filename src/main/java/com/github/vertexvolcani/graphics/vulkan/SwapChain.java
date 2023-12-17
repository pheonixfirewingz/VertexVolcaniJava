package com.github.vertexvolcani.graphics.vulkan;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.graphics.vulkan.pipeline.Fence;
import com.github.vertexvolcani.graphics.vulkan.pipeline.Semaphore;
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeType;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-05
 */
public class SwapChain extends LibCleanable {
    /**
     * The Vulkan device associated with this swap chain.
     */
    private final Device device;
    /**
     * The Vulkan surface associated with this swap chain.
     */
    private final Surface surface;
    private final SwapChainBuilder builder;
    private final VmaAllocator allocator;
    /**
     * holds the window width locally
     */
    private final int colour_format;
    private final int colour_space;
    /**
     * The handle to the Vulkan swap chain.
     */
    private long handle;
    private long[] swap_chain_images;
    private Image[] swap_chain_images_view;

    public SwapChain(Device device_in, Surface surface_in,VmaAllocator allocator_in, SwapChainBuilder builder_in) {
        super();
        device = device_in;
        surface = surface_in;
        builder = builder_in;
        allocator = allocator_in;
        colour_format = surface_in.getColourFormat();
        colour_space = surface_in.getColourSpace();
        Log.print(Log.Severity.DEBUG, "Vulkan: creating Vulkan swap chain...");
        create(VK_NULL_HANDLE);
        Log.print(Log.Severity.DEBUG, "Vulkan: done creating Vulkan swap chain");
    }

    private void destroy(long local_handle) {
        if (swap_chain_images_view == null)
            return;
        vkDestroySwapchainKHR(device.getDevice(), local_handle, null);
        for (var imageView : swap_chain_images_view)
            imageView.free();
        swap_chain_images_view = null;
        swap_chain_images = null;
    }

    private void create(long old_handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer = stack.callocLong(1);
            VkSwapchainCreateInfoKHR pCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack).sType$Default();
            VkSurfaceCapabilitiesKHR surfCaps = VkSurfaceCapabilitiesKHR.calloc(stack);
            if (vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.getPhysicalDevice(), surface.getSurface(), surfCaps) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to get physical device surface capabilities");
                throw new IllegalStateException("Failed to get physical device surface capabilities");
            }

            int desired_number_images = surfCaps.minImageCount();
            if ((surfCaps.maxImageCount() > 0) && (desired_number_images > surfCaps.maxImageCount())) {
                desired_number_images = surfCaps.maxImageCount();
            }

            int pre_transform = (surfCaps.supportedTransforms() & VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR) != 0 ? VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR : surfCaps.currentTransform();

            pCreateInfo.oldSwapchain(old_handle);
            pCreateInfo.surface(surface.getSurface());
            pCreateInfo.preTransform(pre_transform);
            pCreateInfo.minImageCount(desired_number_images);
            pCreateInfo.imageFormat(colour_format);
            pCreateInfo.imageColorSpace(colour_space);
            pCreateInfo.imageUsage(builder.image_usage);
            pCreateInfo.imageArrayLayers(builder.image_array_layers);
            pCreateInfo.imageSharingMode(builder.image_sharing_mode);
            pCreateInfo.presentMode(builder.present_mode);
            pCreateInfo.queueFamilyIndexCount(builder.queue_family_index_count);
            pCreateInfo.pQueueFamilyIndices(builder.queue_Family_indices);
            pCreateInfo.clipped(builder.clipped);
            pCreateInfo.compositeAlpha(builder.composite_alpha);

            VkExtent2D currentExtent = surface.getSurfaceSize();
            pCreateInfo.imageExtent().width(Math.max(currentExtent.width(), 10)).height(Math.max(currentExtent.height(), 10));
            if (vkCreateSwapchainKHR(device.getDevice(), pCreateInfo, null, buffer) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: could not create swap chain");
                throw new IllegalStateException("could not create swap chain");
            }
            if (old_handle != VK_NULL_HANDLE) {
                destroy(old_handle);
                Log.print(Log.Severity.DEBUG, "Vulkan: done freeing old swap chain");
            }
            handle = buffer.get(0);
            IntBuffer pImageCount = stack.callocInt(1);
            if (vkGetSwapchainImagesKHR(device.getDevice(), handle, pImageCount, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to get number of swap chain images");
                throw new IllegalStateException("Failed to get number of swap chain images");
            }
            int imageCount = pImageCount.get(0);

            LongBuffer pSwapChainImages = stack.callocLong(imageCount);
            if (vkGetSwapchainImagesKHR(device.getDevice(), handle, pImageCount, pSwapChainImages) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to get swap chain images");
                throw new IllegalStateException("Failed to get swap chain images");
            }

            long[] images = new long[imageCount];
            Image[] imageViews = new Image[imageCount];

            Image.ImageInformation info = new Image.ImageInformation();
            info.setFormat(colour_format);
            VkImageSubresourceRange colorAttachmentView = VkImageSubresourceRange.calloc(stack)
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).layerCount(1);
            info.setSubResourceRange(colorAttachmentView);
            for (int i = 0; i < imageCount; i++) {
               imageViews[i] = new Image(allocator,info,pSwapChainImages.get(i));
            }
            swap_chain_images = images;
            swap_chain_images_view = imageViews;
        }
    }

    public void recreate() {
        Log.print(Log.Severity.DEBUG, "Vulkan: recreating Vulkan swap chain...");
        create(handle);
    }

    public long getSwapChain() {
        return handle;
    }

    public long[] getImages() {
        return swap_chain_images;
    }

    public Image[] getImagesViews() {
        return swap_chain_images_view;
    }

    @NativeType("VkResult")
    public int acquireNextImage(@Nullable Fence fence, @Nonnull Semaphore semaphore, @NativeType("uint32_t const *") @Nonnull java.nio.IntBuffer pImageIndex) {
        return vkAcquireNextImageKHR(device.getDevice(), handle, Long.MAX_VALUE, semaphore.getSemaphore().handle(), fence != null ? fence.getFence().handle() : VK_NULL_HANDLE, pImageIndex);
    }

    @NativeType("VkResult")
    public int queuePresent(@Nonnull VkQueue queue, @Nonnull Semaphore[] pWaitSemaphores, @NativeType("uint32_t const *") @Nonnull IntBuffer pImageIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer buffer_swap = stack.callocLong(1);
            LongBuffer semaphore = stack.callocLong(pWaitSemaphores.length);
            for (int i = 0; i < pWaitSemaphores.length; i++) {
                semaphore.put(i, pWaitSemaphores[i].getSemaphore().handle());
            }
            buffer_swap.put(0, handle);
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack).sType$Default().pWaitSemaphores(semaphore)
                    .swapchainCount(1).pSwapchains(buffer_swap).pImageIndices(pImageIndex);
            return vkQueuePresentKHR(queue, presentInfo);
        }
    }

    @Override
    public final void free() {
        device.waitIdle();
        destroy(handle);
        Log.print(Log.Severity.DEBUG, "Vulkan: done freeing swap chain");
    }

    public static class SwapChainBuilder {
        private int image_usage = 0;
        private int image_array_layers = 0;
        private int image_sharing_mode = 0; //VK_SHARING_MODE_EXCLUSIVE default
        private int present_mode = 0;
        private int queue_family_index_count = 0;
        private IntBuffer queue_Family_indices = null;

        private boolean clipped = false;

        private int composite_alpha = 0;

        public SwapChainBuilder() {

        }

        public SwapChainBuilder imageUsage(int image_usage_in) {
            image_usage = image_usage_in;
            return this;
        }

        public SwapChainBuilder imageArrayLayers(int image_array_layers_in) {
            image_array_layers = image_array_layers_in;
            return this;
        }

        public SwapChainBuilder imageSharingMode(int image_sharing_mode_in) {
            image_sharing_mode = image_sharing_mode_in;
            return this;
        }

        public SwapChainBuilder presentMode(int present_mode_in) {
            present_mode = present_mode_in;
            return this;
        }

        public SwapChainBuilder queueFamilyIndexCount(int queue_family_index_count_in) {
            queue_family_index_count = queue_family_index_count_in;
            return this;
        }

        public SwapChainBuilder queueFamilyIndices(@Nullable IntBuffer queue_Family_indices_in) {
            queue_Family_indices = queue_Family_indices_in;
            return this;
        }

        public SwapChainBuilder clipped() {
            clipped = true;
            return this;
        }

        public SwapChainBuilder compositeAlpha(int composite_alpha_in) {
            composite_alpha = composite_alpha_in;
            return this;
        }
    }
}
