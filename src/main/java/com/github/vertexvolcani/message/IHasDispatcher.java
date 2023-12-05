package com.github.vertexvolcani.message;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public interface IHasDispatcher {
    void subscribe(IListener listener);
    void unSubscribe(IListener listener);
}
