package com.github.vertexvolcani.message.events;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-30
 */
public class MousePressEvent implements IEvent {
    public static final long ID = MousePressEvent.class.getName().hashCode();
    public final int button;
    public final int action;
    public final int mods;
    public MousePressEvent(int button_in, int action_in, int mods_in) {
        button = button_in;
        action = action_in;
        mods = mods_in;
    }
    @Override
    public long getID() {
        return ID;
    }
}
