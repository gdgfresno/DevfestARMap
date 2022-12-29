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

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.InstructionsController;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
public class AugmentedImageActivity extends AppCompatActivity implements FragmentOnAttachListener, BaseArFragment.OnSessionConfigurationListener {
  private static final String TAG = "AugmentedImageActivity";

  private ArFragment arFragment;
  private ImageView fitToScanView;
  private boolean imageDetected = false;

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getSupportFragmentManager().addFragmentOnAttachListener(this);
    if (savedInstanceState == null) {
      if (Sceneform.isSupported(this)) {
        getSupportFragmentManager().beginTransaction()
            .add(R.id.arFragment, ArFragment.class, null)
            .commit();
      }
    }

    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    showARWarning();
  }

  private void showARWarning() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.WarningDialogStyle);
    String arWarning = getResources().getString(R.string.ar_warning);
    String title = getResources().getString(R.string.ar_warning_title);
    builder.setMessage(arWarning).setTitle(title).setPositiveButton("OK", null);
    AlertDialog dialog = builder.create();
    dialog.show();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }

  public void onAugmentedImageTrackingUpdate(AugmentedImage augmentedImage) {
    // If the images is already detected, for better CPU usage we do not need to scan for it
    if (imageDetected) {
      return;
    }

    if (augmentedImage.getTrackingState() == TrackingState.TRACKING
        && augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {

      // Have to switch to UI Thread to update View.
      fitToScanView.setVisibility(View.GONE);

      // Create a new anchor for newly found images.
      if (!augmentedImageMap.containsKey(augmentedImage)) {
        ArSceneView arSceneView = arFragment.getArSceneView();
        Session session = arSceneView.getSession();
        assert session != null;
        Anchor anchor = session.createAnchor(augmentedImage.getCenterPose());
        Scene scene = arSceneView.getScene();
        AugmentedImageNode augmentedImageNode = new AugmentedImageNode(anchor, scene);
        augmentedImageNode.populateScene(this);
        augmentedImageMap.put(augmentedImage, augmentedImageNode);
        arSceneView.getPlaneRenderer().setShadowReceiver(false);
      }

      arFragment.getInstructionsController().setEnabled(
          InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false);
      imageDetected = true;
    }
  }

  @Override
  public void onSessionConfiguration(Session session, Config config) {
    // From https://github.com/SceneView/sceneform-android/blob/master/samples/augmented-images/src/main/java/com/google/ar/sceneform/samples/augmentedimages/MainActivity.java
    config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);

    // Use setFocusMode to configure auto-focus.
    config.setFocusMode(Config.FocusMode.AUTO);
    config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);

    // Load a pre-built AugmentedImageDatabase
    try (InputStream is = getResources().openRawResource(R.raw.activator)) {
      AugmentedImageDatabase augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
      config.setAugmentedImageDatabase(augmentedImageDatabase);
    } catch (IOException e) {
      Log.e(TAG, "IO exception loading augmented image database.", e);
    }

    // Check for image detection
    arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
  }

  @Override
  public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
    if (fragment.getId() == R.id.arFragment) {
      arFragment = (ArFragment) fragment;
      arFragment.setOnSessionConfigurationListener(this);
    }
  }
}
