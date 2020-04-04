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

/**
 * Node for rendering an augmented image.
 */
@SuppressWarnings({"AndroidApiChecker"})
class AugmentedImageNode extends AnchorNode {
  class ARObject {
    String fileName;
    CompletableFuture<ModelRenderable> renderable;
    Node node;
    Vector3 position;

    ARObject(String fileName, Vector3 position) {
      this.fileName = fileName;
      this.position = position;
    }
  }

  private static final String TAG = "AugmentedImageNode";

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

  AugmentedImageNode(Context context) {
    for (ARObject arObject : arObjectList) {
      arObject.renderable = ModelRenderable.builder()
              .setSource(context, Uri.parse(arObject.fileName))
              .build();
    }
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  void setImage(AugmentedImage image) {
    boolean allDone = Stream.of(arObjectList).allMatch(arObject -> arObject.renderable.isDone());
    // If any of the models are not loaded, then recurse when all are loaded.
    if (!allDone) {
      CompletableFuture.allOf(arObjectList[0].renderable, arObjectList[1].renderable,
              arObjectList[2].renderable, arObjectList[3].renderable,
              arObjectList[4].renderable, arObjectList[5].renderable)
          .thenAccept((Void aVoid) -> setImage(image))
          .exceptionally(
              throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
              });
    }

    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    for (ARObject arObject : arObjectList) {
      arObject.node = new BillBoardNode();
      arObject.node.setParent(this);
      arObject.node.setRenderable(arObject.renderable.getNow(null));
      arObject.node.setLocalPosition(arObject.position);
    }
  }
}
