package com.valleydevfest.armap;

import android.util.Log;

import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class PointerNode extends Node {
    private Node trackedNode;

    @Override
    public void onUpdate(FrameTime frameTime) {
        // Typically, getScene() will never return null because onUpdate() is only called when the node
        // is in the scene.
        // However, if onUpdate is called explicitly or if the node is removed from the scene on a
        // different thread during onUpdate, then getScene may be null.
        if (trackedNode == null)
            return;

        Scene scene = getScene();
        if (scene == null)
            return;

        Camera camera = scene.getCamera();
        if (camera == null)
            return;

        Vector3 cameraPosition = camera.getWorldPosition();
        Vector3 targetPosition =  trackedNode.getWorldPosition();
        cameraPosition.z = targetPosition.z;
        Vector3 direction = Vector3.subtract(cameraPosition, targetPosition);

        float magnitude = (float)Math.sqrt(direction.x * direction.x + direction.y * direction.y);

//        Vector3 c2 = camera.getLocalPosition();
//        this.setLocalPosition(new Vector3(
//                c2.x, // + direction.x / magnitude,
//                c2.y, // + direction.y / magnitude,
//                c2.z + 0.5f));

//        Vector3 ownPosition = this.getWorldPosition();
//        this.setWorldPosition(new Vector3(
//                ownPosition.x, // + direction.x / magnitude,
//                ownPosition.y, // + direction.y / magnitude,
//                ownPosition.z));

        this.setWorldPosition(new Vector3(
                cameraPosition.x, // + direction.x / magnitude,
                cameraPosition.y, // + direction.y / magnitude,
                cameraPosition.z - 0.5f));

        Log.i("onArrowUpdate", String.format("trg %f %f %f, cam %f %f %f",
                targetPosition.x, targetPosition.y, targetPosition.z,
                cameraPosition.x, cameraPosition.y, cameraPosition.z));

        Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
        this.setWorldRotation(lookRotation);
    }

    public void setTrackedNode(Node nodeToTrack) {
        trackedNode = nodeToTrack;
    }
}
