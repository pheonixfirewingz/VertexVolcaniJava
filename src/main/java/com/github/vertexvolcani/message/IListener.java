package com.github.vertexvolcani.message;

import com.github.vertexvolcani.message.events.IEvent;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public interface IListener {
    void eventExe(IEvent event);
}
