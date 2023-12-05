package com.github.vertexvolcani.message.events;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class WindowIsClosingEvent implements IEvent{
    public static final long ID = WindowIsClosingEvent.class.getName().hashCode();
    @Override
    public long getID() {
        return ID;
    }
}
