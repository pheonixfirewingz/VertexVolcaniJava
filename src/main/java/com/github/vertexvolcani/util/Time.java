package com.github.vertexvolcani.util;

import org.lwjgl.glfw.GLFW;

public class Time {
    private static long boot_time = 0;
    static {
        //time in seconds
        boot_time = System.currentTimeMillis() / 1000;
    }
    public static long getTime() {
        return System.currentTimeMillis() / 1000 - boot_time;
    }
    public static class Limiter {
        private double last;
        private double limit;
        public Limiter(long limit_in) {
            limit = 1.0 / (double)limit_in;
            last = getTime();
        }

        public void updateLimit(long limit_in) {
            limit = 1.0 / (double)limit_in;
        }

        public void timeControl() {
            double delta_time = last + 1.0 / (double)limit;
            for (double current = getTime(); current < last; current = getTime()) {
                GLFW.glfwWaitEventsTimeout(last - current);
            }
            last = delta_time;
        }
    }
}
