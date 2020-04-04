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
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Node for rendering an augmented image.
 */
class AugmentedImageNode extends AnchorNode {
  static class ARObject {
    String text;
    ModelRenderable renderable;
    Node node;
    Vector3 position;

    ARObject(String text, Vector3 position) {
      this.text = text;
      this.position = position;
    }
  }

  private static final String TAG = "AugmentedImageNode";
  private CompletableFuture<Material> redMaterialFuture;
  private CompletableFuture<Material> whiteMaterialFuture;

  // Coordinates:
  // x: positive - right, negative - left
  // y: positive - behind, negative - forward
  // z: positive - down, negative - up
  private ARObject[] arObjectList = {
    new ARObject("1", new Vector3(2.5f, 2.0f, 1)),
    new ARObject("2", new Vector3(2.5f, -3.0f, 1)),
    new ARObject("3", new Vector3(-4.0f, -3.5f, 1)),
    new ARObject("4", new Vector3(-5.5f, -4.0f, 1)),
    new ARObject("5", new Vector3(-6.0f, -3.0f, 1)),
    new ARObject("up", new Vector3(3.5f, 8.0f, 1))
  };

  AugmentedImageNode(Anchor anchor, Context context) {
    super(anchor);

    Color red = new Color(android.graphics.Color.RED);
    redMaterialFuture = MaterialFactory.makeOpaqueWithColor(context, red);
    Color white = new Color(android.graphics.Color.WHITE);
    whiteMaterialFuture = MaterialFactory.makeOpaqueWithColor(context, white);
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  void populateScene() {
    boolean allDone = redMaterialFuture.isDone() && whiteMaterialFuture.isDone();
    // If any of the models are not loaded, then recurse when all are loaded.
    if (!allDone) {
      CompletableFuture.allOf(redMaterialFuture, whiteMaterialFuture)
        .thenAccept((Void aVoid) -> populateScene())
        .exceptionally(
          throwable -> {
            Log.e(TAG, "Exception building scene", throwable);
            return null;
          });
    }

    try {
      Material redMaterial = redMaterialFuture.get();
      Material whiteMaterial = whiteMaterialFuture.get();

      for (ARObject arObject : arObjectList) {
        arObject.renderable = ShapeFactory.makeCube(
                new Vector3(0.5f, 1, 0.01f),
                new Vector3(0.0f, 0.0f, 0.0f),
                redMaterial
        );

        // TODO: add numbers onto the billboard
        arObject.renderable = ShapeFactory.makeCylinder(
                0.5f,
                0.5f,
                new Vector3(0, 0, 0),
                whiteMaterial
        );

        arObject.node = new BillBoardNode();
        arObject.node.setParent(this);
        arObject.node.setRenderable(arObject.renderable);
        arObject.node.setLocalPosition(arObject.position);
      }
    }
    catch (ExecutionException | InterruptedException e) {
      Log.e(TAG, "Scene populating exception " + e.toString());
    }
  }
}
