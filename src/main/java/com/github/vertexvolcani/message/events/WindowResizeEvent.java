package com.github.vertexvolcani.message.events;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class WindowResizeEvent implements IEvent{
    public static final long ID = WindowResizeEvent.class.getName().hashCode();
    public final int width;
    public final int height;

    public WindowResizeEvent(int width_in, int height_in) {
        width = width_in;
        height = height_in;
    }

    @Override
    public long getID() {
        return 0;
    }
}
