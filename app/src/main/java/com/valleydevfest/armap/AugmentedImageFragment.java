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

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.ux.ArFragment;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extend the ArFragment to customize the ARCore session configuration to include Augmented Images.
 */
public class AugmentedImageFragment extends ArFragment {
  private static final String TAG = "AugmentedImageFragment";

  // This is a pre-created database containing the sample image.
  private static final String ACTIVATOR_IMAGE_DATABASE = "activator.imgdb";

  // Do a runtime check for the OpenGL level available at runtime to avoid Sceneform crashing the
  // application.
  private static final double MIN_OPENGL_VERSION = 3.0;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);

    View contentView = requireActivity().findViewById(android.R.id.content);

    String openGlVersionString =
        ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
            .getDeviceConfigurationInfo()
            .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      String openGLCheckMessage = "Sceneform requires OpenGL ES 3.0 or later";
      Log.e(TAG, openGLCheckMessage);
      Snackbar.make(contentView, openGLCheckMessage, Snackbar.LENGTH_LONG)
              .setAction("Action", null).show();
    }
  }

  @Override
  public View onCreateView(
          LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    // Turn off the plane discovery since we're only looking for images
    getInstructionsController().setEnabled(false);
    getInstructionsController().setVisible(false);
    getArSceneView().getPlaneRenderer().setEnabled(false);
    return view;
  }

  @Override
  protected Config getSessionConfiguration(Session session) {
    Config config = super.getSessionConfiguration(session);

    // Use setFocusMode to configure auto-focus.
    config.setFocusMode(Config.FocusMode.AUTO);

    config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);

    if (!setupAugmentedImageDatabase(config, session)) {
      try {
        Snackbar.make(requireActivity().findViewById(android.R.id.content),
                "Could not setup augmented image database",
                Snackbar.LENGTH_LONG).setAction("Action", null).show();
      }
      catch (NullPointerException e) {
        Log.e(TAG, String.format("Cannot get content view %s", e.toString()));
      }
    }
    return config;
  }

  private boolean setupAugmentedImageDatabase(Config config, Session session) {
    AugmentedImageDatabase augmentedImageDatabase;

    AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
    if (assetManager == null) {
      Log.e(TAG, "Context is null, cannot initialize image database.");
      return false;
    }

    // Load a pre-built AugmentedImageDatabase
    try (InputStream is = getContext().getAssets().open(ACTIVATOR_IMAGE_DATABASE)) {
      augmentedImageDatabase = AugmentedImageDatabase.deserialize(session, is);
    } catch (IOException e) {
      Log.e(TAG, "IO exception loading augmented image database.", e);
      return false;
    }

    config.setAugmentedImageDatabase(augmentedImageDatabase);
    return true;
  }
}
