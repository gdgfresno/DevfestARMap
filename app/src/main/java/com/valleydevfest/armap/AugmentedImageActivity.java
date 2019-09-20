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

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.core.Pose;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 *
 * <p>In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * ArAugmentedImage_getTrackingMethod() and render only when the tracking method equals to
 * AR_AUGMENTED_IMAGE_TRACKING_METHOD_FULL_TRACKING. See details in <a
 * href="https://developers.google.com/ar/develop/c/augmented-images/">Recognize and Augment
 * Images</a>.
 */
public class AugmentedImageActivity extends AppCompatActivity {
  private static final String TAG = "AugmentedImageActivity";

  private ArFragment arFragment;
  private ImageView fitToScanView;

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Registered with the Sceneform Scene object, this method is called at the start of each frame.
   *
   * @param frameTime - time since last frame.
   */
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    // If there is no frame, just return.
    if (frame == null) {
      return;
    }

    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {
      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          String text = "Detected Image " + augmentedImage.getIndex();
          Snackbar.make(findViewById(android.R.id.content),
                  text, Snackbar.LENGTH_LONG).setAction("Action", null).show();

          break;

        case TRACKING:
          // Have to switch to UI Thread to update View.
          fitToScanView.setVisibility(View.GONE);

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage)) {
            AugmentedImageNode node = new AugmentedImageNode(this);
            node.setImage(augmentedImage);
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);
          } else {
            // If the image anchor is already created
            AugmentedImageNode node = augmentedImageMap.get(augmentedImage);
            try {
              // node.updateBallPose(physicsController.getBallPose());

              // Use real world gravity, (0, -10, 0) as gravity
              // Convert to Physics world coordinate (because Maze mesh has to be static)
              // Use it as a force to move the ball
              Pose worldGravityPose = Pose.makeTranslation(0, -10f, 0);
//              // Fun experiment, use camera direction as gravity
//              float cameraZDir[] = frame.getCamera().getPose().getZAxis();
//              Vector3 cameraZVector = new Vector3(cameraZDir[0], cameraZDir[1], cameraZDir[2]);
//              Vector3 cameraGravity = cameraZVector.negated().scaled(10);
//              Pose worldGravityPose = Pose.makeTranslation(
//                      cameraGravity.x, cameraGravity.y, cameraGravity.z);

//              Pose mazeGravityPose = augmentedImage.getCenterPose().inverse().compose(worldGravityPose);
//              float mazeGravity[] = mazeGravityPose.getTranslation();
            }
            catch (NullPointerException e) {
              Log.e(TAG, "onUpdateFrame TRACKING - could not update ball pose", e);
            }
          }
          break;

        case STOPPED:
          AugmentedImageNode node = augmentedImageMap.get(augmentedImage);
          augmentedImageMap.remove(augmentedImage);
          if (node != null)
            arFragment.getArSceneView().getScene().removeChild(node);
          break;
      }
    }
  }
}
