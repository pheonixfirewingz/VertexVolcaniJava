package com.github.vertexvolcani.graphics.vulkan.pipeline.descriptors;
/* Vertex Volcani - LICENCE
 *
 * GNU Lesser General Public License Version 3.0
 *
 * Copyright Luke Shore (c) 2023, 2024
 */
/**
 * Represents a size configuration for a descriptor pool in Vulkan.
 *
 * @param type the type of the descriptor
 * @param size the number of descriptors of this type in the pool
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-20
 */
public record DescriptorPoolSize(DescriptorType type, int size) {
}