package com.github.vertexvolcani.util;

import com.github.vertexvolcani.graphics.vulkan.buffer.Buffer;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public record Vertices (Buffer buffer,Buffer index_buffer,VkVertexInputBindingDescription.Buffer bindingDescriptor,VkVertexInputAttributeDescription.Buffer attributeDescriptions) {
}
