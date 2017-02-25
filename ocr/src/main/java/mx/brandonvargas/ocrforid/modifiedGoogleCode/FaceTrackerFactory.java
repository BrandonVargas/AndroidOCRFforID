/*
 * Copyright (C) The Android Open Source Project
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
package mx.brandonvargas.ocrforid.modifiedGoogleCode;

import android.graphics.Canvas;
import android.util.Log;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import mx.brandonvargas.ocrforid.camera.GraphicOverlay;
import mx.brandonvargas.ocrforid.util.Constants;
import mx.brandonvargas.ocrforid.util.DocumentIdentifier;

/**
 * Factory for creating a tracker and associated graphic to be associated with a new face.  The
 * multi-processor uses this factory to create face trackers as needed -- one for each individual.
 */
public class FaceTrackerFactory implements MultiProcessor.Factory<Face> {
    private GraphicOverlay mGraphicOverlay;
    private DocumentIdentifier documentIdentifier;

    public FaceTrackerFactory(GraphicOverlay graphicOverlay, DocumentIdentifier documentIdentifier) {
        mGraphicOverlay = graphicOverlay;
        this.documentIdentifier = documentIdentifier;
    }

    @Override
    public Tracker<Face> create(Face face) {
        FaceGraphic graphic = new FaceGraphic(mGraphicOverlay, documentIdentifier);
        return new GraphicTracker<>(mGraphicOverlay, graphic);
    }
}

/**
 * Graphic instance for rendering face position, size, and ID within an associated graphic overlay
 * view.
 */
class FaceGraphic extends TrackedGraphic<Face> {


    private DocumentIdentifier documentIdentifier;

    private volatile Face mFace;

    FaceGraphic(GraphicOverlay overlay, DocumentIdentifier documentIdentifier) {
        super(overlay);
        this.documentIdentifier = documentIdentifier;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateItem(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position, size, and ID on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }
        float cx = translateX(face.getPosition().x + face.getWidth() / 2);
        if (cx > Constants.OFFSET_HEAD) {
            documentIdentifier.setFaceSide(Constants.FACE_RIGHT);
            Log.e("FACE ", "toRight");
        } else {
            documentIdentifier.setFaceSide(Constants.FACE_LEFT);
            Log.e("FACE ", "toLeft");
        }

    }

    @Override
    public boolean contains(float x, float y) {
        return false;
    }
}
