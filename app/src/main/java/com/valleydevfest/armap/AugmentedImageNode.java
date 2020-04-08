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
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * Node for rendering an augmented image.
 */
class AugmentedImageNode extends AnchorNode {
  class ARObject {
    int resourceId;
    CompletableFuture<Texture> texture;
    CompletableFuture<Material> material;
    ModelRenderable renderable;
    Node node;
    Vector3 position;

    ARObject(int resourceId, Vector3 position) {
      this.resourceId = resourceId;
      this.position = position;
    }

    void setTexture(CompletableFuture<Texture> texture) {
      this.texture = texture;
    }

    CompletableFuture<Texture> getTexture() {
      return texture;
    }

    void setMaterial(CompletableFuture<Material> material) {
      this.material = material;
    }

    CompletableFuture<Material> getMaterial() {
      return material;
    }
  }

  private static final String TAG = "AugmentedImageNode";
  private CompletableFuture<Material> redMaterialFuture;

  // Coordinates:
  // x: positive - right, negative - left
  // y: positive - behind, negative - forward
  // z: positive - down, negative - up
  private ARObject[] arObjectList = {
    new ARObject(R.drawable.room1, new Vector3(2.5f, 2.0f, 1)),
    new ARObject(R.drawable.room2, new Vector3(2.5f, -3.0f, 1)),
    new ARObject(R.drawable.room3, new Vector3(-4.0f, -3.5f, 1)),
    new ARObject(R.drawable.room4, new Vector3(-5.5f, -4.0f, 1)),
    new ARObject(R.drawable.room5, new Vector3(-6.0f, -3.0f, 1)),
    new ARObject(R.drawable.upstairs, new Vector3(3.5f, 8.0f, 1))
  };

  AugmentedImageNode(Anchor anchor, Context context) {
    super(anchor);

    for (ARObject arObject : arObjectList) {
      Texture.Builder textureBuilder = Texture.builder();
      textureBuilder.setSource(context, arObject.resourceId);
      CompletableFuture<Texture> texturePromise = textureBuilder.build();
      arObject.setTexture(texturePromise);
      texturePromise.thenAccept(texture -> {
        CompletableFuture<Material> materialPromise =
                MaterialFactory.makeOpaqueWithTexture(context, texture);
        arObject.setMaterial(materialPromise);
      });
    }

  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  void populateScene() {
    boolean texturesDone = Stream.of(arObjectList).allMatch(arObject -> arObject.getTexture().isDone());
    // If any of the textures are not loaded, then recurse until all are loaded.
    if (!texturesDone) {
      CompletableFuture.allOf(
              arObjectList[0].getTexture(), arObjectList[1].getTexture(),
              arObjectList[2].getTexture(), arObjectList[3].getTexture(),
              arObjectList[4].getTexture(), arObjectList[5].getTexture())
        .thenAccept((Void aVoid) -> populateScene())
        .exceptionally(
          throwable -> {
            Log.e(TAG, "Exception building scene", throwable);
            return null;
          });
    }

    boolean materialsDone = Stream.of(arObjectList).allMatch(arObject -> arObject.getMaterial().isDone());
    if (!materialsDone) {
      CompletableFuture.allOf(
            arObjectList[0].getMaterial(), arObjectList[1].getMaterial(),
            arObjectList[2].getMaterial(), arObjectList[3].getMaterial(),
            arObjectList[4].getMaterial(), arObjectList[5].getMaterial())
        .thenAccept((Void aVoid) -> populateScene())
        .exceptionally(
          throwable -> {
            Log.e(TAG, "Exception building scene", throwable);
            return null;
          });
    }

    try {
      for (ARObject arObject : arObjectList) {
        Material textureMaterial = arObject.getMaterial().get();

        arObject.renderable = ShapeFactory.makeCube(
          new Vector3(0.5f, 1, 0.01f),
          new Vector3(0.0f, 0.0f, 0.0f),
          textureMaterial
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
