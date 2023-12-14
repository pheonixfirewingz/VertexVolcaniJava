package com.github.vertexvolcani.graphics.events;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class WindowClosingEvent implements IEvent{
    public static final long ID = WindowClosingEvent.class.getName().hashCode();
    @Override
    public long getID() {
        return ID;
    }
}
