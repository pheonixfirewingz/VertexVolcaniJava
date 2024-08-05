package com.github.vertexvolcani.graphics.vulkan.pipeline.descriptors;

import com.github.vertexvolcani.graphics.vulkan.pipeline.ShaderType;
import com.github.vertexvolcani.util.Nullable;

import java.nio.LongBuffer;

public record LayoutBinding(int binding, int descriptorCount, DescriptorType descriptorType, ShaderType stageFlags, @Nullable LongBuffer pImmutableSamplers) {
}
