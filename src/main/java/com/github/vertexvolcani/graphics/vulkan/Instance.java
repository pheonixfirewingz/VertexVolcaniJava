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
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

// Code adapted from https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/vulkan/HelloVulkan.java
/**
 * A class representing a Vulkan instance.
 * The Vulkan instance is a fundamental object that must be created before interacting with the Vulkan API.
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public class Instance extends LibCleanable {

    /** The Vulkan instance handle. */
    private final VkInstance instance;

    private final boolean debug;

    /** The debug messenger callback function. */
    private final VkDebugUtilsMessengerCallbackEXT dbgFunc = VkDebugUtilsMessengerCallbackEXT.create(
            (messageSeverity, messageTypes, pCallbackData, pUserData) -> {
                // Convert Vulkan debug information to human-readable format
                String severity = getSeverity(messageSeverity);
                String type = getType(messageTypes);
                // Retrieve and print debug message details
                VkDebugUtilsMessengerCallbackDataEXT data = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                Log.logVkDebugMessage(messageSeverity, type + " " + severity + ": [" + data.pMessageIdNameString() + "]\n\t" + data.pMessageString());
                return VK_FALSE;
            }
    );


    private static String getType(int messageTypes) {
        String type;
        if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT) != 0) {
            type = "GENERAL";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) != 0) {
            type = "VALIDATION";
        } else if ((messageTypes & VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT) != 0) {
            type = "PERFORMANCE";
        } else {
            type = "UNKNOWN";
        }
        return type;
    }

    private static String getSeverity(int messageSeverity) {
        String severity;
        if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT) != 0) {
            severity = "VERBOSE";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
            severity = "INFO";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            severity = "WARNING";
        } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            severity = "ERROR";
        } else {
            severity = "UNKNOWN";
        }
        return severity;
    }



    /**
     * Return true if all layer names specified in {@code check_names} can be found in given {@code layer} properties.
     */
    private static PointerBuffer demo_check_layers(MemoryStack stack, VkLayerProperties.Buffer available, String... layers) {
        PointerBuffer required = stack.callocPointer(layers.length);
        for (int i = 0; i < layers.length; i++) {
            boolean found = false;

            // Check if the required layer is present in the available layers
            for (int j = 0; j < available.capacity(); j++) {
                available.position(j);
                if (layers[i].equals(available.layerNameString())) {
                    found = true;
                    break;
                }
            }

            // If the required layer is not found, print an error message and return null
            if (!found) {
                System.err.format("Cannot find layer: %s\n", layers[i]);
                return null;
            }

            // Put the layer name into the required layers buffer
            required.put(i, stack.ASCII(layers[i]));
        }

        return required;
    }

    /**
     * Constructor for creating Vulkan instance
     * @param debug_in should vulkan enabled debug or not
     * @param app_name to tell the instance the name of the app
     */
    public Instance(boolean debug_in,CharSequence app_name) {
        super();
        debug = debug_in;
        Log.print(Log.Severity.DEBUG,"Vulkan: creating Instance");
        if(debug){
            Log.print(Log.Severity.DEBUG,"Vulkan: debugging enabled");
        }
        PointerBuffer handle = MemoryUtil.memCallocPointer(1);
        PointerBuffer extension_names = MemoryUtil.memCallocPointer(64);
        PointerBuffer required_extensions = glfwGetRequiredInstanceExtensions();
        ByteBuffer EXT_debug_utils = null;
        if (debug) {
            EXT_debug_utils = memASCII(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
        }

        if (required_extensions == null) {
            Log.print(Log.Severity.ERROR,"Vulkan Error: glfwGetRequiredInstanceExtensions failed to find the platform surface extensions.");
            throw new IllegalStateException("glfwGetRequiredInstanceExtensions failed to find the platform surface extensions.");
        }

        for (int i = 0; i < required_extensions.capacity(); i++) {
            extension_names.put(required_extensions.get(i));
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ip = memCallocInt(1);
            PointerBuffer requiredLayers = null;

            // Check for validation layers if debug mode is enabled
            if (debug) {
                Log.print(Log.Severity.DEBUG,"Vulkan: setting up debug layers...");
                if (vkEnumerateInstanceLayerProperties(ip, null) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR,"Vulkan Error: failed to enumerate instance");
                    throw new RuntimeException("failed to enumerate instance");
                }

                if (ip.get(0) > 0) {
                    VkLayerProperties.Buffer availableLayers = VkLayerProperties.calloc(ip.get(0), stack);
                    if (vkEnumerateInstanceLayerProperties(ip, availableLayers) != VK_SUCCESS) {
                        Log.print(Log.Severity.ERROR," Vulkan Error: failed to enumerate instance");
                        throw new RuntimeException("failed to enumerate instance");
                    }

                    // Check for the required validation layers
                    requiredLayers = demo_check_layers(stack, availableLayers, "VK_LAYER_KHRONOS_validation");
                    if (requiredLayers == null) {
                        requiredLayers = demo_check_layers(stack, availableLayers, "VK_LAYER_LUNARG_standard_validation");
                    }
                    if (requiredLayers == null) {
                        requiredLayers = demo_check_layers(
                                stack, availableLayers,
                                "VK_LAYER_GOOGLE_threading",
                                "VK_LAYER_LUNARG_parameter_validation",
                                "VK_LAYER_LUNARG_object_tracker",
                                "VK_LAYER_LUNARG_core_validation",
                                "VK_LAYER_GOOGLE_unique_objects"
                        );
                    }
                }

                // Check for instance extensions, especially the debug utils extension
                if (vkEnumerateInstanceExtensionProperties((String) null, ip, null) != VK_SUCCESS) {
                    Log.print(Log.Severity.ERROR,"Vulkan Error: failed to enumerate instance extension properties");
                    throw new RuntimeException("failed to enumerate instance extension properties");
                }

                if (ip.get(0) != 0) {
                    VkExtensionProperties.Buffer instance_extensions = VkExtensionProperties.calloc(ip.get(0), stack);
                    if (vkEnumerateInstanceExtensionProperties((String) null, ip, instance_extensions) != VK_SUCCESS) {
                        Log.print(Log.Severity.ERROR,"Vulkan Error: failed to enumerate instance extension properties");
                        throw new RuntimeException("failed to enumerate instance extension properties");
                    }

                    // Add the debug utils extension if available
                    for (int i = 0; i < ip.get(0); i++) {
                        instance_extensions.position(i);
                        if (VK_EXT_DEBUG_UTILS_EXTENSION_NAME.equals(instance_extensions.extensionNameString())) {
                            extension_names.put(EXT_debug_utils);
                        }
                    }
                }
                Log.print(Log.Severity.DEBUG,"Vulkan: done creating instance");
            }
            // Free allocated memory
            MemoryUtil.memFree(ip);

            // Create Vulkan application and instance information
            ByteBuffer APP_SHORT_NAME = stack.UTF8(app_name);
            ByteBuffer ENG_SHORT_NAME = stack.UTF8("VertexVolcani");
            VkApplicationInfo app = VkApplicationInfo.calloc(stack).sType$Default()
                    .pNext(NULL).pApplicationName(APP_SHORT_NAME)
                    .applicationVersion(0).pEngineName(ENG_SHORT_NAME)
                    .engineVersion(0).apiVersion(VK.getInstanceVersionSupported());

            extension_names.flip();
            VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc(stack).sType$Default()
                    .pNext(NULL).flags(0).pApplicationInfo(app);
                    if(requiredLayers != null) {
                        pCreateInfo.ppEnabledLayerNames(requiredLayers);
                    }
                    pCreateInfo.ppEnabledExtensionNames(extension_names);


            // Add debug messenger if debug mode is enabled
            VkDebugUtilsMessengerCreateInfoEXT dbgCreateInfo;
            if (debug) {
                dbgCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                        .sType$Default().pNext(NULL).flags(0).messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                        .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT |
                                VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                        .pfnUserCallback(dbgFunc).pUserData(NULL);

                pCreateInfo.pNext(dbgCreateInfo.address());
            }
            // Create Vulkan instance
            if (vkCreateInstance(pCreateInfo, null, handle) != VK_SUCCESS) {
                Log.print(Log.Severity.ERROR,"Vulkan Error: could not make Vulkan instance");
                throw new RuntimeException("could not make Vulkan instance");
            }
            instance = new VkInstance(handle.get(0), pCreateInfo);
            extension_names.clear();
            // Free allocated memory
            MemoryUtil.memFree(extension_names);
        }
        // Free allocated memory
        if (EXT_debug_utils != null) {
            MemoryUtil.memFree(EXT_debug_utils);
        }
        MemoryUtil.memFree(handle);
        Log.print(Log.Severity.DEBUG,"Vulkan: instance setup done");
    }

    /**
     * Gets the internal Vulkan instance handle.
     *
     * @return The Vulkan instance handle.
     */
    public VkInstance getInstance() {
        return instance;
    }

    /**
     * Cleans up resources associated with the Vulkan instance.
     * This method should be called when the instance is no longer needed to release Vulkan resources.
     */
    @Override
    public final void free() {
        vkDestroyInstance(instance, null);
        Log.print(Log.Severity.DEBUG,"Vulkan: instance free memory done");
        dbgFunc.free();
    }

    public boolean getDebug() {
        return debug;
    }
}