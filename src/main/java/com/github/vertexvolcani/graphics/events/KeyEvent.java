package com.github.vertexvolcani.graphics.events;
/*
 * @author Luke Shore
 * @version 1.0
 * @since 2023-11-29
 */
public class KeyEvent implements IEvent{
    public static final long ID = KeyEvent.class.getName().hashCode();
    public final int key;
    public final int scancode;
    public final int mods;
    public final boolean pressed;
    public KeyEvent(int key_in, int scancode_in,int mods_in,boolean pressed_in) {
        key = key_in;
        scancode =scancode_in;
        mods = mods_in;
        pressed = pressed_in;
    }
    @Override
    public long getID() {
        return ID;
    }
}
