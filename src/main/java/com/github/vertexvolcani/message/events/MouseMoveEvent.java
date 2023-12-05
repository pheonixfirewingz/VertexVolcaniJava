package com.github.vertexvolcani.message.events;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class MouseMoveEvent implements IEvent{
    public static final long ID = MouseMoveEvent.class.getName().hashCode();
    public final double position_x;
    public final double position_y;
    public MouseMoveEvent(double position_x_in, double position_y_in) {
        position_y = position_y_in;
        position_x = position_x_in;
    }
    @Override
    public long getID() {
        return ID;
    }
}
