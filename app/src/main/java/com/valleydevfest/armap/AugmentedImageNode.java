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

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;

/**
 * Node for basically the whole scene.
 */
class AugmentedImageNode extends AnchorNode {

  static class ARObject {
    int resourceId;
    Vector3 position;

    ARObject(int resourceId, Vector3 position) {
      this.resourceId = resourceId;
      this.position = position;
    }
  }

  private static final String TAG = "AugmentedImageNode";

  // Coordinates:
  // x: positive - right, negative - left
  // y: positive - behind, negative - forward
  // z: positive - down, negative - up
  private final ARObject[] arObjectList = {
      new ARObject(R.drawable.room1, new Vector3(2.5f, 2.0f, 1)),
      new ARObject(R.drawable.room2, new Vector3(2.5f, -3.0f, 1)),
      new ARObject(R.drawable.room3, new Vector3(-4.0f, -3.5f, 1)),
      new ARObject(R.drawable.room4, new Vector3(-5.5f, -4.0f, 1)),
      new ARObject(R.drawable.room5, new Vector3(-6.0f, -3.0f, 1)),
      new ARObject(R.drawable.upstairs, new Vector3(3.5f, 8.0f, 1))
  };

  AugmentedImageNode(Anchor anchor, Scene scene) {
    super(anchor);

    setParent(scene);
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
//      .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
//      .build();

    for (ARObject arObject : arObjectList) {
      Texture.builder()
        .setSource(context, arObject.resourceId)
//        .setSampler(sampler)
        .build()
        .thenAccept(texture -> MaterialFactory.makeOpaqueWithTexture(context, texture)
          .thenAccept(material -> {
            ModelRenderable renderable = ShapeFactory.makeCube(
              new Vector3(2, 4, 0.01f),
              new Vector3(0.0f, 0.0f, 0.0f),
              material
            );
            renderable.setShadowCaster(false);
            renderable.setShadowReceiver(false);

            Node node = new BillBoardNode();
            node.setParent(this);
            node.setRenderable(renderable);
            node.setLocalPosition(arObject.position);
          })
        );
    }
  }
}
