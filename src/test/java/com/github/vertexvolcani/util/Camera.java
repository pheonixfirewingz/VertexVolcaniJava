package com.github.vertexvolcani.util;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    public Vector3f position;
    public float pitch;
    public float yaw;
    public float roll;

    public Camera() {
        position = new Vector3f(0, 3, -3);
        pitch = 0;
        yaw = 45;
        roll = 180;
    }

    public void updatePosition(float x, float y, float z) {
        position.x = x;
        position.y = y;
        position.z = z;
    }

    public void updateRotation(float p, float y, float r) {
        pitch = p;
        yaw = y;
        roll = r;
    }

    public void update(float x, float y, float z, float p, float y2, float r) {
        updatePosition(x, y, z);
        updateRotation(p, y2, r);
    }

    public Matrix4f getViewMatrix() {
        Matrix4f view = new Matrix4f();
        view.identity();
        view.lookAt(position, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        view.rotate((float) Math.toRadians(pitch), new Vector3f(1, 0, 0));
        view.rotate((float) Math.toRadians(yaw), new Vector3f(0, 1, 0));
        view.rotate((float) Math.toRadians(roll), new Vector3f(0, 0, 1));
        view.translate(-position.x, -position.y, -position.z);
        return view;
    }

}
