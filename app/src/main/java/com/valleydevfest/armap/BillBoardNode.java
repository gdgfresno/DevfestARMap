package com.valleydevfest.armap;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class BillBoardNode extends Node {
    @Override
    public void onUpdate(FrameTime frameTime) {
        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        Scene scene = getScene();
        if (scene == null)
            return;

        Camera camera = scene.getCamera();
        if (camera == null)
            return;

        Vector3 cameraPosition = camera.getWorldPosition();
        Vector3 boardPosition = this.getWorldPosition();
        cameraPosition.y = boardPosition.y;
        Vector3 direction = Vector3.subtract(boardPosition, cameraPosition);

        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        this.setWorldRotation(lookRotation);
    }
}
