/*
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

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Quaternion;

/**
 * Node for rendering an augmented image.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {
  class ARObject {
    public String fileName;
    public CompletableFuture<ModelRenderable> renderable;
    public Node node;
    public Vector3 position;

    public ARObject(String fileName, Vector3 position) {
      this.fileName = fileName;
      this.renderable = null;
      this.node = null;
      this.position = position;
    }
  }

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  // Coordinates:
  // x: positive - right, negative - left
  // y: positive - behind, negative - forward
  // z: positive - down, negative - up
  private ARObject[] arObjectList = {
    new ARObject("room1.sfb", new Vector3(2.5f, 2.0f, 1)),
    new ARObject("room2.sfb", new Vector3(2.5f, -3.0f, 1)),
    new ARObject("room3.sfb", new Vector3(-4.0f, -3.5f, 1)),
    new ARObject("room4.sfb", new Vector3(-5.5f, -4.0f, 1)),
    new ARObject("room5.sfb", new Vector3(-6.0f, -3.0f, 1)),
    new ARObject("upstairs.sfb", new Vector3(3.5f, 8.0f, 1))
  };

  private CompletableFuture<ModelRenderable> arrowRenderable;
  private Node arrowNode;

  public AugmentedImageNode(Context context) {
    arrowRenderable = ModelRenderable.builder()
      .setSource(context, Uri.parse("arrow.sfb"))
      .build();

    for (ARObject arObject : arObjectList) {
      arObject.renderable = ModelRenderable.builder()
              .setSource(context, Uri.parse(arObject.fileName))
              .build();
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

    boolean allDone = Stream.of(arObjectList).allMatch(arObject -> arObject.renderable.isDone());
    // If any of the models are not loaded, then recurse when all are loaded.
    if (!allDone) {
      CompletableFuture.allOf(arObjectList[0].renderable, arObjectList[1].renderable,
              arObjectList[2].renderable, arObjectList[3].renderable,
              arObjectList[4].renderable, arObjectList[5].renderable, arrowRenderable)
          .thenAccept((Void aVoid) -> setImage(image))
          .exceptionally(
              throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
              });
    }

    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    arrowNode = new DirectionalNode(false);
    arrowNode.setParent(this);
    arrowNode.setEnabled(false);
    arrowNode.setRenderable(arrowRenderable.getNow(null));
    arrowNode.setLocalPosition(new Vector3(0, 0.1f, 0.5f));

    for (ARObject arObject : arObjectList) {
      arObject.node = new DirectionalNode(true);
      arObject.node.setParent(this);
      arObject.node.setRenderable(arObject.renderable.getNow(null));
      arObject.node.setLocalPosition(arObject.position);
    }
  }

  public void updateArrowPose(Pose pose) {
    if (arrowNode == null)
      return;

    // arrowNode.setLocalPosition(new Vector3(pose.tx(), pose.ty(), pose.tz()));
    arrowNode.setLocalRotation(new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw()));
  }
}
