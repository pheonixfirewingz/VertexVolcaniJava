package com.github.vertexvolcani.message.events;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public class StopThread implements IEvent{
    public static final long ID = KeyEvent.class.getName().hashCode();
    @Override
    public long getID() {
        return ID;
    }
}
