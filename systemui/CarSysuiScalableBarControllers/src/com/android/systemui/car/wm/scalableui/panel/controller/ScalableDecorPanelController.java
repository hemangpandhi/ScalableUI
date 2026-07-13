/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.systemui.car.wm.scalableui.panel.controller;

import android.content.Context;
import android.view.View;

/**
 * Contract for scalable UI DecorPanel / TaskPanel controllers.
 * Mirrors the AOSP car-wm-shell-lib DecorPanelController interface.
 */
public interface ScalableDecorPanelController {
    /** Inflate or return the cached panel view (RRO-bound). */
    View getView();

    /** Called when the panel is attached to the window hierarchy. */
    void onPanelAttached();

    /** Called when the panel is detached; release all subscriptions. */
    void onPanelDetached();

    /** Panel identifier from the XML {@code id} attribute. */
    String getPanelId();
}
