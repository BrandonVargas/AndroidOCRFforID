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

import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.text.TextBlock;

import mx.brandonvargas.ocrforid.camera.GraphicOverlay;
import mx.brandonvargas.ocrforid.util.Constants;
import mx.brandonvargas.ocrforid.util.DocumentIdentifier;

/**
 * A very simple Processor which gets detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock>, MultiProcessor.Factory<Face> {

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private DocumentIdentifier documentIdentifier;
    private GraphicOverlay mGraphicOverlay2;

    public OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, DocumentIdentifier documentIdentifier) {
        mGraphicOverlay = ocrGraphicOverlay;
        this.documentIdentifier = documentIdentifier;
    }

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        mGraphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        for (int i = 0; i < items.size(); ++i) {
            TextBlock item = items.valueAt(i);
            if (item != null && item.getValue() != null) {
                if (item.getValue().contains("INSTITUTO FEDERAL ELECTORAL")) {
                    documentIdentifier.setType(Constants.IFEB);
                } else if (item.getValue().contains("INSTITUTO NACIONAL ELECTORAL")) {
                    Log.d("OcrDetectorProcessor", "INE E " + item.getValue());
                    documentIdentifier.setType(Constants.IFEE);
                }
            }
            //OcrGraphic graphic = new OcrGraphic(mGraphicOverlay, item);
            //mGraphicOverlay.add(graphic);
        }
    }

    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        mGraphicOverlay.clear();
    }

    @Override
    public Tracker<Face> create(Face face) {
        return null;
    }
}
