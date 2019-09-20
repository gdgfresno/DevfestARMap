package com.valleydevfest.armap;

import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class DirectionalNode extends Node {
    private boolean vertical = false;

    public DirectionalNode(boolean vertical) {
        this.vertical = vertical;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        if (getScene() == null) {
            return;
        }

        Vector3 cameraPosition = getScene().getCamera().getWorldPosition();
        Vector3 boardPosition = this.getWorldPosition();
        Vector3 direction = null;
        if (vertical) {
            cameraPosition.y = boardPosition.y;
            direction = Vector3.subtract(boardPosition, cameraPosition);
        } else {
            cameraPosition.z = boardPosition.z;
            direction = Vector3.subtract(cameraPosition, boardPosition);
        }
        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        this.setWorldRotation(lookRotation);
    }
}
