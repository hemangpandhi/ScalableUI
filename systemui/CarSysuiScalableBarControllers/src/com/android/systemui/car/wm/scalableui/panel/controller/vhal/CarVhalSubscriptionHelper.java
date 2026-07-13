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

package com.android.systemui.car.wm.scalableui.panel.controller.vhal;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Lifecycle-aware wrapper for {@link CarPropertyManager} subscriptions.
 * Ensures callbacks are unregistered on detach and surfaces disconnect state to UI.
 */
public final class CarVhalSubscriptionHelper {

    private static final String TAG = "CarVhalSubscription";

    public interface VhalConnectionListener {
        void onVhalConnected();
        void onVhalDisconnected();
    }

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final SparseArray<CarPropertyManager.CarPropertyEventCallback> mCallbacks =
            new SparseArray<>();
    private final List<VhalConnectionListener> mConnectionListeners = new ArrayList<>();

    private Car mCar;
    private CarPropertyManager mCarPropertyManager;
    private boolean mConnected;
    private boolean mLifecycleActive;

    private final Car.CarServiceLifecycleListener mCarLifecycleListener = (car, ready) -> {
        if (!mLifecycleActive) {
            return;
        }
        if (ready) {
            onCarReady(car);
        } else {
            onCarDisconnected();
        }
    };

    public void start(Context context) {
        if (mLifecycleActive) {
            return;
        }
        mLifecycleActive = true;
        try {
            mCar = Car.createCar(context, mMainHandler, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT,
                    mCarLifecycleListener);
            if (mCar != null && mCar.isConnected()) {
                onCarReady(mCar);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create Car connection", e);
            notifyDisconnected();
        }
    }

    public void stop() {
        mLifecycleActive = false;
        unregisterAll();
        if (mCar != null) {
            try {
                mCar.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Error disconnecting Car", e);
            }
            mCar = null;
        }
        mCarPropertyManager = null;
        mConnected = false;
    }

    public boolean isConnected() {
        return mConnected && mCarPropertyManager != null;
    }

    public void addConnectionListener(VhalConnectionListener listener) {
        mConnectionListeners.add(listener);
        if (mConnected) {
            listener.onVhalConnected();
        }
    }

    public void removeConnectionListener(VhalConnectionListener listener) {
        mConnectionListeners.remove(listener);
    }

    /**
     * Register a property callback tied to this helper's lifecycle.
     * Returns false if VHAL is unavailable — caller should show disabled UI.
     */
    public boolean subscribe(int propertyId, int areaId,
            CarPropertyManager.CarPropertyEventCallback callback) {
        if (mCarPropertyManager == null) {
            Log.w(TAG, "subscribe: CarPropertyManager unavailable for property " + propertyId);
            return false;
        }
        try {
            mCallbacks.put(propertyId, callback);
            mCarPropertyManager.registerCallback(callback, propertyId,
                    CarPropertyManager.SENSOR_RATE_ONCHANGE);
            return true;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "CarNotConnectedException subscribing to " + propertyId, e);
            notifyDisconnected();
            return false;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Property not available: " + propertyId, e);
            return false;
        }
    }

    public void unsubscribe(int propertyId) {
        if (mCarPropertyManager == null) {
            return;
        }
        CarPropertyManager.CarPropertyEventCallback callback = mCallbacks.get(propertyId);
        if (callback != null) {
            try {
                mCarPropertyManager.unregisterCallback(callback, propertyId);
            } catch (CarNotConnectedException e) {
                Log.w(TAG, "CarNotConnectedException unsubscribing " + propertyId, e);
            }
            mCallbacks.remove(propertyId);
        }
    }

    public Float getFloatProperty(int propertyId, int areaId) {
        if (mCarPropertyManager == null) {
            return null;
        }
        try {
            return mCarPropertyManager.getFloatProperty(propertyId, areaId);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "CarNotConnectedException reading " + propertyId, e);
            notifyDisconnected();
            return null;
        }
    }

    public boolean setFloatProperty(int propertyId, int areaId, float value) {
        if (mCarPropertyManager == null) {
            return false;
        }
        try {
            mCarPropertyManager.setFloatProperty(propertyId, areaId, value);
            return true;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "CarNotConnectedException writing " + propertyId, e);
            notifyDisconnected();
            return false;
        }
    }

    public Integer getIntProperty(int propertyId, int areaId) {
        if (mCarPropertyManager == null) {
            return null;
        }
        try {
            return mCarPropertyManager.getIntProperty(propertyId, areaId);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "CarNotConnectedException reading " + propertyId, e);
            notifyDisconnected();
            return null;
        }
    }

    public boolean setIntProperty(int propertyId, int areaId, int value) {
        if (mCarPropertyManager == null) {
            return false;
        }
        try {
            mCarPropertyManager.setIntProperty(propertyId, areaId, value);
            return true;
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "CarNotConnectedException writing " + propertyId, e);
            notifyDisconnected();
            return false;
        }
    }

    private void onCarReady(Car car) {
        try {
            mCarPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
            if (mCarPropertyManager != null) {
                mConnected = true;
                notifyConnected();
            } else {
                notifyDisconnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to obtain CarPropertyManager", e);
            notifyDisconnected();
        }
    }

    private void onCarDisconnected() {
        mConnected = false;
        mCarPropertyManager = null;
        notifyDisconnected();
    }

    private void unregisterAll() {
        if (mCarPropertyManager == null) {
            mCallbacks.clear();
            return;
        }
        for (int i = 0; i < mCallbacks.size(); i++) {
            int propertyId = mCallbacks.keyAt(i);
            CarPropertyManager.CarPropertyEventCallback callback = mCallbacks.valueAt(i);
            try {
                mCarPropertyManager.unregisterCallback(callback, propertyId);
            } catch (CarNotConnectedException e) {
                Log.w(TAG, "Unregister failed for " + propertyId, e);
            }
        }
        mCallbacks.clear();
    }

    private void notifyConnected() {
        for (VhalConnectionListener listener : mConnectionListeners) {
            listener.onVhalConnected();
        }
    }

    private void notifyDisconnected() {
        for (VhalConnectionListener listener : mConnectionListeners) {
            listener.onVhalDisconnected();
        }
    }
}
