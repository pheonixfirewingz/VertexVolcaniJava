package com.github.vertexvolcani.graphics.vulkan;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
import com.github.vertexvolcani.util.LibCleanable;
import com.github.vertexvolcani.util.Log;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeType;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * A class representing a Vulkan image managed by Vulkan Memory Allocator (VMA).
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-14
 */
public class Image extends LibCleanable {
    /**
     * The Vulkan Memory Allocator used for managing the image.
     */
    private final VmaAllocator allocator;

    /**
     * dose Image own the main image
     */
    private final boolean owned;
    /**
     * The handle to the Vulkan image.
     */
    private final DeviceHandle handle;
    /**
     * The handle to the Vulkan image view.
     */
    private final DeviceHandle view;
    /**
     * The handle to the memory allocation associated with the image.
     */
    private final long allocation;
    /**
     * stores the data object count;
     */
    private final long size;

    /**
     * Constructs a new Image instance.
     *
     * @param allocator_in The Vulkan Memory Allocator.
     */
    public Image(VmaAllocator allocator_in, ImageInformation image_information, @NativeType("VkDeviceSize") long size_in, @NativeType("VmaMemoryUsage") int vma_usage) {
        owned = true;
        allocator = allocator_in;
        size = size_in;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.callocLong(1);
            PointerBuffer pAllocation = stack.callocPointer(1);

            VkImageCreateInfo buffer_create_info = VkImageCreateInfo.calloc(stack).sType$Default().pNext(0).flags(0);
            buffer_create_info.imageType(image_information.image_type).extent(image_information.extent).mipLevels(image_information.mip_levels)
                    .arrayLayers(image_information.array_layers).format(image_information.format).samples(image_information.samples)
                    .usage(image_information.usage).initialLayout(image_information.initial_layout)
                    .queueFamilyIndexCount(image_information.queue_family_index_count).pQueueFamilyIndices(image_information.ptr_queue_family_indices)
                    .sharingMode(image_information.sharing_mode).tiling(image_information.tiling);

            VmaAllocationCreateInfo allocation_create_info = VmaAllocationCreateInfo.calloc(stack);
            allocation_create_info.usage(vma_usage);

            if (vmaCreateImage(allocator.getVmaAllocator(), buffer_create_info, allocation_create_info, pBuffer, pAllocation, null) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create Vulkan image with VMA.");
                throw new RuntimeException("Failed to create Vulkan image with VMA.");
            }
            handle = new DeviceHandle(allocator.getDev(), pBuffer.get(0));

            VkImageViewCreateInfo view_create_info = VkImageViewCreateInfo.calloc(stack).sType$Default().image(handle.handle()).flags(0);
            view_create_info.viewType(image_information.view_type).format(image_information.format).subresourceRange(image_information.subresource_range);

            view = handle.device().createImageView(view_create_info);
            if (handle.device().didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create Vulkan image view.");
                throw new RuntimeException("Failed to create Vulkan image view.");
            }
            allocation = pAllocation.get(0);
        }
    }

    /**
     * Constructs a new Image view instance.
     *
     * @param allocator_in The Vulkan Memory Allocator.
     */
    public Image(VmaAllocator allocator_in, ImageInformation image_information, Image image,boolean owned_in) {
        owned = owned_in;
        allocator = allocator_in;
        size = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.callocLong(1);
            handle = new DeviceHandle(allocator.getDev(), VK_NULL_HANDLE);
            VkImageViewCreateInfo view_create_info = VkImageViewCreateInfo.calloc(stack).sType$Default().image(image.getImage()).flags(0);
            view_create_info.viewType(image_information.view_type).format(image_information.format).subresourceRange(image_information.subresource_range);
            view = handle.device().createImageView(view_create_info);
            if (handle.device().didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create Vulkan image view.");
                throw new RuntimeException("Failed to create Vulkan image view.");
            }
            allocation = VK_NULL_HANDLE;
        }
    }

    /**
     * Constructs a new Image view instance.
     *
     * @param allocator_in The Vulkan Memory Allocator.
     */
    public Image(VmaAllocator allocator_in, ImageInformation image_information, long image) {
        owned = false;
        allocator = allocator_in;
        size = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.callocLong(1);
            handle = new DeviceHandle(allocator.getDev(), image);
            VkImageViewCreateInfo view_create_info = VkImageViewCreateInfo.calloc(stack).sType$Default().image(image).flags(0);
            view_create_info.viewType(image_information.view_type).format(image_information.format).subresourceRange(image_information.subresource_range);
            view = handle.device().createImageView(view_create_info);
            if (handle.device().didErrorOccur()) {
                Log.print(Log.Severity.ERROR, "Vulkan: Failed to create Vulkan image view.");
                throw new RuntimeException("Failed to create Vulkan image view.");
            }
            allocation = VK_NULL_HANDLE;
        }
    }

    /**
     * Maps the image memory into the application's address space.
     *
     * @return A mapped ByteBuffer.
     */
    private ByteBuffer map() {
        PointerBuffer ppData = MemoryUtil.memCallocPointer(1);
        vmaMapMemory(allocator.getVmaAllocator(), allocation, ppData);
        ByteBuffer image = ppData.getByteBuffer(0, (int) getMemorySize());
        MemoryUtil.memFree(ppData);
        return image;
    }

    /**
     * transfer data to image
     *
     * @param src this is the pointer to the data to be copied
     */
    public Image load(ByteBuffer src) {
        final var image = map();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(image), src.remaining());
        unmap();
        return this;
    }

    /**
     * transfer data to image
     *
     * @param src this is the pointer to the data to be copied
     */
    public Image load(FloatBuffer src) {
        final var image = map();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(image), (long) src.remaining() * Float.BYTES);
        unmap();
        return this;
    }

    /**
     * transfer data to image
     *
     * @param src this is the pointer to the data to be copied
     */
    public Image load(IntBuffer src) {
        final var image = map();
        MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(image), (long) src.remaining() * Integer.BYTES);
        unmap();
        return this;
    }

    /**
     * Unmaps the image memory.
     */
    private void unmap() {
        vmaUnmapMemory(allocator.getVmaAllocator(), allocation);
    }

    /**
     * Gets the size of the image memory.
     *
     * @return The size of the image memory.
     */
    protected long getMemorySize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryRequirements memory_requirements = VkMemoryRequirements.calloc(stack);
            handle.device().getImageMemoryRequirements(handle, memory_requirements);
            return memory_requirements.size();
        }
    }

    /**
     * Gets the size of the image.
     *
     * @return The size of the image.
     */
    public long getSize() {
        return size;
    }

    /**
     * returns the image handle
     *
     * @return VkImage handle of the vulkan object
     */
    public long getImage() {
        return handle.handle();
    }

    /**
     * returns the image view handle
     *
     * @return VkImageView handle of the vulkan object
     */
    public DeviceHandle getImageView() {
        return view;
    }

    /**
     * Cleans up resources associated with the image.
     */
    @Override
    public final void free() {
        handle.device().waitIdle();
        handle.device().destroyImageView(view);
        if (allocation != VK_NULL_HANDLE && owned) {
            vmaDestroyImage(allocator.getVmaAllocator(), handle.handle(), allocation);
        }
    }

    /**
     * Represents information about a Vulkan image.
     * This class is used to configure and create Vulkan images.
     * Extends LibCleanable to manage cleanup of Vulkan resources.
     */
    public static class ImageInformation extends LibCleanable {
        // Fields to store image information with default values
        private int image_type = VK_IMAGE_TYPE_2D;
        private int view_type = VK_IMAGE_VIEW_TYPE_2D;
        private VkComponentMapping.Buffer components = null;
        private VkImageSubresourceRange subresource_range = null;
        private int format = VK_FORMAT_R8G8B8A8_UNORM;
        private VkExtent3D extent = null;
        private int mip_levels = 1;
        private int array_layers = 1;
        private int samples = VK_SAMPLE_COUNT_1_BIT;
        private int tiling = VK_IMAGE_TILING_OPTIMAL;
        private int usage  = VK_IMAGE_USAGE_SAMPLED_BIT;
        private int sharing_mode = VK_SHARING_MODE_EXCLUSIVE;
        private int queue_family_index_count = 0;
        private IntBuffer ptr_queue_family_indices = null;
        private int initial_layout = VK_IMAGE_LAYOUT_UNDEFINED;

        /**
         * Sets the image type.
         *
         * @param image_type_in The image type.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setImageType(int image_type_in) {
            image_type = image_type_in;
            return this;
        }

        /**
         * Sets the view type.
         *
         * @param view_type_in The view type.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setViewType(int view_type_in) {
            view_type = view_type_in;
            return this;
        }

        /**
         * Sets the component mapping.
         *
         * @param components_in The component mapping.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setComponents(VkComponentMapping.Buffer components_in) {
            components = components_in;
            return this;
        }

        /**
         * Sets the image subresource range.
         *
         * @param subresource_range_in The subresource range.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setSubResourceRange(VkImageSubresourceRange subresource_range_in) {
            subresource_range = subresource_range_in;
            return this;
        }

        /**
         * Sets the image format.
         *
         * @param format_in The image format.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setFormat(int format_in) {
            format = format_in;
            return this;
        }

        /**
         * Sets the image extent.
         *
         * @param extent_in The image extent.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setExtent(VkExtent3D extent_in) {
            extent = extent_in;
            return this;
        }

        /**
         * Sets the number of mip levels.
         *
         * @param mip_levels_in The number of mip levels.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setMipLevels(int mip_levels_in) {
            mip_levels = mip_levels_in;
            return this;
        }

        /**
         * Sets the number of array layers.
         *
         * @param array_layers_in The number of array layers.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setArrayLayers(int array_layers_in) {
            array_layers = array_layers_in;
            return this;
        }

        /**
         * Sets the number of samples.
         *
         * @param samples_in The number of samples.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setSamples(int samples_in) {
            samples = samples_in;
            return this;
        }

        /**
         * Sets the image tiling.
         *
         * @param tiling_in The image tiling.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setTiling(int tiling_in) {
            tiling = tiling_in;
            return this;
        }

        /**
         * Sets the image usage.
         *
         * @param usage_in The image usage.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setUsage(int usage_in) {
            usage = usage_in;
            return this;
        }

        /**
         * Sets the image sharing mode.
         *
         * @param sharing_mode_in The image sharing mode.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setSharingMode(boolean sharing_mode_in) {
            sharing_mode = sharing_mode_in ? VK_SHARING_MODE_CONCURRENT : VK_SHARING_MODE_EXCLUSIVE;
            return this;
        }

        /**
         * Sets the queue family index count.
         *
         * @param queue_family_index_count_in The queue family index count.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setQueueFamilyIndexCount(int queue_family_index_count_in) {
            queue_family_index_count = queue_family_index_count_in;
            return this;
        }

        /**
         * Sets the pointer to the queue family indices.
         *
         * @param ptr_queue_family_indices_in The pointer to the queue family indices.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setPtrQueueFamilyIndices(IntBuffer ptr_queue_family_indices_in) {
            ptr_queue_family_indices = ptr_queue_family_indices_in;
            return this;
        }

        /**
         * Sets the initial layout of the image.
         *
         * @param initial_layout_in The initial layout of the image.
         * @return This ImageInformation instance for method chaining.
         */
        public ImageInformation setInitialLayout(int initial_layout_in) {
            initial_layout = initial_layout_in;
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void free() {
            components.free();
            subresource_range.free();
            extent.free();
        }
    }
}