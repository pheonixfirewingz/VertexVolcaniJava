package com.github.vertexvolcani.message;

import com.github.vertexvolcani.message.events.IEvent;

/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-12-5
 */
public class WindowCanClosingEvent implements IEvent {
    public static final long ID = WindowCanClosingEvent.class.getName().hashCode();
    @Override
    public long getID() {
        return ID;
    }
}
