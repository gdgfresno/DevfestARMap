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
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * Node for basically the whole scene.
 */
class AugmentedImageNode extends AnchorNode {

  class ARObject {
    int resourceId;
    String fileName;
    CompletableFuture<Texture> texture;
    CompletableFuture<Material> material;
    ModelRenderable renderable;
    Node node;
    Vector3 position;
    boolean done;

    ARObject(int resourceId, String fileName, Vector3 position) {
      this.resourceId = resourceId;
      this.fileName = fileName;
      this.position = position;
    }

    void setTexture(CompletableFuture<Texture> texture) {
      Log.d(TAG, String.format("Texture set for %d", resourceId));
      this.texture = texture;
    }

    CompletableFuture<Texture> getTexture() {
      return texture;
    }

    void setMaterial(CompletableFuture<Material> material) {
      Log.d(TAG, String.format("Material set for %d", resourceId));
      this.material = material;
    }

    CompletableFuture<Material> getMaterial() {
      return material;
    }

    void setDone(boolean done) {
      this.done = done;
    }
  }

  private static final String TAG = "AugmentedImageNode";

  // Coordinates:
  // x: positive - right, negative - left
  // y: positive - behind, negative - forward
  // z: positive - down, negative - up
  private ARObject[] arObjectList = {
    new ARObject(R.drawable.room1, "room1.png", new Vector3(2.5f, 2.0f, 1)),
    new ARObject(R.drawable.room2, "room2.png", new Vector3(2.5f, -3.0f, 1)),
    new ARObject(R.drawable.room3, "room3.png", new Vector3(-4.0f, -3.5f, 1)),
    new ARObject(R.drawable.room4, "room4.png", new Vector3(-5.5f, -4.0f, 1)),
    new ARObject(R.drawable.room5, "room5.png", new Vector3(-6.0f, -3.0f, 1)),
    new ARObject(R.drawable.upstairs, "upstairs.png", new Vector3(3.5f, 8.0f, 1))
  };

  AugmentedImageNode(Anchor anchor, Scene scene) {
    super(anchor);

    setParent(scene);

//    for (ARObject arObject : arObjectList) {
//      Texture.Builder textureBuilder = Texture.builder();
//      textureBuilder.setSource(context, arObject.resourceId);
//      CompletableFuture<Texture> texturePromise = textureBuilder.build();
//      arObject.setTexture(texturePromise);
//      texturePromise.thenAccept(texture -> {
//        CompletableFuture<Material> materialPromise =
//                MaterialFactory.makeOpaqueWithTexture(context, texture);
//        arObject.setMaterial(materialPromise);
//      });
//    }
  }

  private void afterMaterialsLoaded() {
    // Step 3: composing scene objects
    // Get a handler that can be used to post to the main thread
    Log.d(TAG, "Making cubes...");
    for (ARObject arObject : arObjectList) {
      try {
        Material textureMaterial = arObject.getMaterial().get();
        Log.d(TAG, String.format("Making cube for %d %s %s", arObject.resourceId, Integer.toHexString(System.identityHashCode(arObject.getMaterial())), Integer.toHexString(System.identityHashCode(arObject.getTexture()))));
      }
      catch (ExecutionException | InterruptedException e) {
        Log.e(TAG, "Scene populating exception " + e.toString());
      }
    }
  }

  private Long waitForMaterials() {
    while (!Stream.of(arObjectList).allMatch(arObject -> arObject.getMaterial() != null)) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }
    return 0L;
  }

  private void afterTexturesSet() {
    boolean materialsDone = Stream.of(arObjectList).allMatch(arObject -> arObject.getMaterial() != null && arObject.getMaterial().isDone());
    // If any of the materials are not loaded, then recurse until all are loaded.
    Log.d(TAG, String.format("materialsDone %s", materialsDone ? "true" : "false"));
    if (!materialsDone) {
      CompletableFuture<Texture>[] materialPromises =
        Stream.of(arObjectList).map(ARObject::getMaterial).toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(materialPromises)
        .thenAccept((Void aVoid) -> afterMaterialsLoaded())
        .exceptionally(
          throwable -> {
            Log.e(TAG, "Exception building scene", throwable);
            return null;
          });
    } else {
      afterMaterialsLoaded();
    }
  }

  private void afterTexturesLoaded() {
    // Step 2: material loading
    CompletableFuture materialsSetPromise = CompletableFuture.supplyAsync(this::waitForMaterials);
    CompletableFuture.allOf(materialsSetPromise)
      .thenAccept((Void aVoid) -> afterTexturesSet())
      .exceptionally(
        throwable -> {
          Log.e(TAG, "Exception building scene", throwable);
          return null;
        });
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  void populateScene(Context context) {
//    final Texture.Sampler sampler = Texture.Sampler.builder()
//      .setMinFilter(Texture.Sampler.MinFilter.LINEAR_MIPMAP_LINEAR)
//      .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
//      .setWrapModeR(Texture.Sampler.WrapMode.CLAMP_TO_EDGE)
//      .setWrapModeS(Texture.Sampler.WrapMode.CLAMP_TO_EDGE)
//      .setWrapModeT(Texture.Sampler.WrapMode.CLAMP_TO_EDGE)
//      .build();

    for (ARObject arObject : arObjectList) {
//      Texture.builder()
//        .setSource(context, arObject.resourceId)
//        .setSampler(sampler)
//        .build()
//        .thenAccept(texture -> MaterialFactory.makeOpaqueWithTexture(context, texture)
//          .thenAccept(material -> {
//            arObject.renderable = ShapeFactory.makeCube(
//              new Vector3(0.5f, 1, 0.01f),
//              new Vector3(0.0f, 0.0f, 0.0f),
//              material
//            );
//
//            arObject.node = new BillBoardNode();
//            arObject.node.setParent(this);
//            arObject.node.setRenderable(arObject.renderable);
//            arObject.node.setLocalPosition(arObject.position);
//          })
//        );

      MaterialFactory.makeOpaqueWithColor(context, new Color(android.graphics.Color.RED)).thenAccept(material -> {
        arObject.renderable = ShapeFactory.makeCube(
          new Vector3(0.5f, 1, 0.01f),
          new Vector3(0.0f, 0.0f, 0.0f),
          material
        );

        arObject.node = new BillBoardNode();
        arObject.node.setParent(this);
        arObject.node.setRenderable(arObject.renderable);
        arObject.node.setLocalPosition(arObject.position);
        arObject.node.setEnabled(true);
        Log.d(TAG, String.format("ARObj: %s n %s p %s %s s %s r %s",
                arObject.node.isActive() ? "active" : "inactive",
                arObject.node.getName(),
                Integer.toHexString(System.identityHashCode(arObject.node.getParent().getName())),
                Integer.toHexString(System.identityHashCode(arObject.node.getParent())),
                Integer.toHexString(System.identityHashCode(arObject.node.getScene())),
                Integer.toHexString(System.identityHashCode(arObject.node.getRenderable()))
        ));
      });

      /*
      Texture.Builder textureBuilder = Texture.builder();
      textureBuilder.setSource(context, arObject.resourceId);
      CompletableFuture<Texture> texturePromise = textureBuilder.build();
      arObject.setTexture(texturePromise);
      texturePromise.thenAccept(texture -> {
        CompletableFuture<Material> materialPromise =
                MaterialFactory.makeOpaqueWithTexture(context, texture);
        arObject.setMaterial(materialPromise);
      });
      */
    }

    /*
    // Step 1: texture loading
    boolean texturesDone = Stream.of(arObjectList).allMatch(arObject -> arObject.getTexture() != null && arObject.getTexture().isDone());
    // If any of the textures are not loaded, then recurse until all are loaded.
    Log.d(TAG, String.format("texturesDone %s", texturesDone ? "true" : "false"));
    if (!texturesDone) {
      CompletableFuture<Texture>[] texturePromises =
        Stream.of(arObjectList).map(ARObject::getTexture).toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(texturePromises)
        .thenAccept((Void aVoid) -> afterTexturesLoaded())
        .exceptionally(
          throwable -> {
            Log.e(TAG, "Exception building scene", throwable);
            return null;
          });
    } else {
      afterTexturesLoaded();
    }
    */
  }
}
