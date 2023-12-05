package com.github.vertexvolcani.message;

import com.github.vertexvolcani.message.events.IEvent;
import java.util.concurrent.CopyOnWriteArrayList;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public class Dispatcher
{
    CopyOnWriteArrayList<IListener> listeners = new CopyOnWriteArrayList<>();
    public void subscribe(IListener listener){
        listeners.add(listener);
    }

    public void dispatch(IEvent event) {
        for (var listener:listeners)
            listener.eventExe(event);
    }

    public void unSubscribe(IListener listener) {
        listeners.remove(listener);
    }
}
