/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.valleydevfest.armap;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.rendering.Texture;

/**
 * Node for rendering an augmented image.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {
  class ARObject {
    public int resourceId;
    public ModelRenderable renderable;
    public Node node;

    public ARObject(int rId) {
      this.resourceId = rId;
      this.renderable = null;
      this.node = null;
    }
  }

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  private ARObject[] arObjectList = {
    new ARObject(R.drawable.room1),
    new ARObject(R.drawable.room2),
    new ARObject(R.drawable.room3),
    new ARObject(R.drawable.room4),
    new ARObject(R.drawable.room5),
    new ARObject(R.drawable.room6),
    new ARObject(R.drawable.upstairs)
  };

  private CompletableFuture<ModelRenderable> arrowRenderable;
  private Node arrowNode;
  private ModelRenderable rayRenderable;
  private Node rayNode;

  private float mazeScale = 0.0f;

  public AugmentedImageNode(Context context) {
    arrowRenderable = ModelRenderable.builder()
      .setSource(context, Uri.parse("arrow.sfb"))
      .build();

    MaterialFactory.makeOpaqueWithColor(context,
      new Color(android.graphics.Color.RED)).thenAccept(material -> {
        rayRenderable = ShapeFactory.makeCylinder(0.01f, 1.5f,
                new Vector3(0, 0, 0), material); });

    Texture.Builder textureBuilder = Texture.builder();
    for (ARObject arObject : arObjectList) {
//      MaterialFactory.makeOpaqueWithColor(context,
//        new Color(android.graphics.Color.BLUE)).thenAccept(material -> {
//          arObject.renderable = ShapeFactory.makeCube(
//            new Vector3(0.5f, 1, 0.01f),
//            new Vector3(0.0f, 0.0f, 0.0f),
//            material
//          );
//        });

      textureBuilder.setSource(context, arObject.resourceId);
      textureBuilder.build().thenAccept(texture ->
        MaterialFactory.makeOpaqueWithTexture(context, texture).thenAccept(material -> {
          arObject.renderable = ShapeFactory.makeCube(
            new Vector3(0.5f, 1, 0.01f),
            new Vector3(0.0f, 0.0f, 0.0f),
            material
          );
        })
      );
    }
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image) {
    this.image = image;

    // Initialize mazeNode and set its parents and the Renderable.
    // If any of the models are not loaded, process this function
    // until they all are loaded.
    if (!arrowRenderable.isDone()) {
      CompletableFuture.allOf(arrowRenderable)
              .thenAccept((Void aVoid) -> setImage(image))
              .exceptionally(
                      throwable -> {
                        Log.e(TAG, "Exception loading", throwable);
                        return null;
                      });
      return;
    }

    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    final float mazeEdgeSize = 492.65f;
    final float maxImageEdge = Math.max(image.getExtentX(), image.getExtentZ());
    mazeScale = maxImageEdge / mazeEdgeSize;
    Log.w(TAG, String.format("Scale %f", mazeScale));

    arrowNode = new BoardNode();
    arrowNode.setParent(this);
    arrowNode.setRenderable(arrowRenderable.getNow(null));
    arrowNode.setLocalPosition(new Vector3(0, 0.1f, 0.5f));
//    arrowNode.setLookDirection(new Vector3(0, 0, -1));

    rayNode = new BoardNode();
    rayNode.setParent(this);
    rayNode.setRenderable(rayRenderable);
//    rayNode.setLocalPosition(new Vector3(0, 0.5f, 0));

    //rayNode.setLookDirection();

//    for (ARObject arObject : arObjectList) {
    ARObject arObject = arObjectList[0];
      arObject.node = new BoardNode();
      arObject.node.setParent(this);
      arObject.node.setRenderable(arObject.renderable);
      arObject.node.setLocalPosition(new Vector3(0, 0, -0.5f));
//    }

    // Scale Y an extra 10 times to lower the maze wall.
//    mazeNode.setLocalScale(new Vector3(maze_scale, maze_scale * 0.1f, maze_scale));

//    // Make the 4 corner nodes.
//    Vector3 localPosition = new Vector3();
//    Node cornerNode;
//
//    // Upper left corner.
//    localPosition.set(-0.5f * image.getExtentX(), 0.0f, -0.5f * image.getExtentZ());
//    cornerNode = new Node();
//    cornerNode.setParent(this);
//    cornerNode.setLocalPosition(localPosition);
//    cornerNode.setRenderable(ulCorner.getNow(null));
//
//    // Upper right corner.
//    localPosition.set(0.5f * image.getExtentX(), 0.0f, -0.5f * image.getExtentZ());
//    cornerNode = new Node();
//    cornerNode.setParent(this);
//    cornerNode.setLocalPosition(localPosition);
//    cornerNode.setRenderable(urCorner.getNow(null));
//
//    // Lower right corner.
//    localPosition.set(0.5f * image.getExtentX(), 0.0f, 0.5f * image.getExtentZ());
//    cornerNode = new Node();
//    cornerNode.setParent(this);
//    cornerNode.setLocalPosition(localPosition);
//    cornerNode.setRenderable(lrCorner.getNow(null));
//
//    // Lower left corner.
//    localPosition.set(-0.5f * image.getExtentX(), 0.0f, 0.5f * image.getExtentZ());
//    cornerNode = new Node();
//    cornerNode.setParent(this);
//    cornerNode.setLocalPosition(localPosition);
//    cornerNode.setRenderable(llCorner.getNow(null));
  }

  public AugmentedImage getImage() {
    return image;
  }

  public void updateArrowPose(Pose pose) {
    if (arrowNode == null)
      return;

    // arrowNode.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
    arrowNode.setLocalRotation(new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
  }
}
