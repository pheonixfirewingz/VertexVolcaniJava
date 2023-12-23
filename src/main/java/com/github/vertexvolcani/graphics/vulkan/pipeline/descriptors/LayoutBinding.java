package com.github.vertexvolcani.graphics.vulkan.pipeline.descriptors;

import com.github.vertexvolcani.graphics.vulkan.pipeline.ShaderType;
import jakarta.annotation.Nullable;

import java.nio.LongBuffer;

public record LayoutBinding(int binding, int descriptorCount, DescriptorType descriptorType, ShaderType stageFlags, @Nullable LongBuffer pImmutableSamplers) {
}
