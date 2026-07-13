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

import android.car.VehicleAreaSeat;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.PrintWriter;
import java.util.Locale;

/**
 * Controller for the Pleos dual-zone header (driver + passenger HVAC and seat temperature).
 * Inflates RRO layouts via dynamic binding; all bounds are declared in panel XML variants.
 */
public class CarSysuiScalableHeaderController extends CarSysuiScalableBarControllerBase {

    private static final String TAG = "CarSysuiHeaderCtrl";
    private static final String DEFAULT_RRO_PACKAGE =
            "com.android.systemui.rro.scalableUI.sysuiBars";
    private static final float HVAC_STEP_CELSIUS = 0.5f;
    private static final int SEAT_TEMP_STEP = 1;
    private static final int SEAT_TEMP_MIN = 0;
    private static final int SEAT_TEMP_MAX = 3;

    private static final int AREA_DRIVER = VehicleAreaSeat.SEAT_ROW_1_LEFT;
    private static final int AREA_PASSENGER = VehicleAreaSeat.SEAT_ROW_1_RIGHT;

    private float mDriverHvacTemp = 22.0f;
    private float mPassengerHvacTemp = 22.0f;
    private int mDriverSeatTemp = 2;
    private int mPassengerSeatTemp = 2;

    // Driver zone views
    private View mDriverZoneRoot;
    private TextView mDriverHvacTempText;
    private ImageButton mDriverHvacUp;
    private ImageButton mDriverHvacDown;
    private TextView mDriverSeatTempText;
    private ImageButton mDriverSeatUp;
    private ImageButton mDriverSeatDown;

    // Passenger zone views
    private View mPassengerZoneRoot;
    private TextView mPassengerHvacTempText;
    private ImageButton mPassengerHvacUp;
    private ImageButton mPassengerHvacDown;
    private TextView mPassengerSeatTempText;
    private ImageButton mPassengerSeatUp;
    private ImageButton mPassengerSeatDown;

    // Chrome scrims
    private View mHeaderUxScrim;
    private View mHeaderVhalScrim;

    private final CarPropertyManager.CarPropertyEventCallback mHvacCallback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {
                    if (value == null) {
                        return;
                    }
                    int area = value.getAreaId();
                    if (value.getPropertyId() == VehiclePropertyIds.HVAC_TEMPERATURE_SET) {
                        float temp = (Float) value.getValue();
                        mContext.getMainExecutor().execute(() -> updateHvacDisplay(area, temp));
                    } else if (value.getPropertyId() == VehiclePropertyIds.HVAC_SEAT_TEMPERATURE) {
                        int seatTemp = (Integer) value.getValue();
                        mContext.getMainExecutor().execute(() -> updateSeatDisplay(area, seatTemp));
                    }
                }

                @Override
                public void onErrorEvent(int propertyId, int zone) {
                    Log.e(TAG, "VHAL error property=" + propertyId + " zone=" + zone);
                }
            };

    public CarSysuiScalableHeaderController(Context context) {
        this(context, "sysui_scalable_header_panel", DEFAULT_RRO_PACKAGE);
    }

    public CarSysuiScalableHeaderController(Context context, String panelId, String rroPackage) {
        super(context, panelId, rroPackage);
    }

    @Override
    protected String getLayoutResourceName() {
        if ("sysui_driver_zone_panel".equals(mPanelId)) {
            return "car_sysui_scalable_header_driver_zone";
        }
        if ("sysui_passenger_zone_panel".equals(mPanelId)) {
            return "car_sysui_scalable_header_passenger_zone";
        }
        return "car_sysui_scalable_header";
    }

    @Override
    protected void bindViews(View root) {
        bindDriverZone(root);
        bindPassengerZone(root);
        bindChromeScrim(root);
        wireClickListeners();
    }

    private void bindDriverZone(View root) {
        int rootId = resolveRroId("sysui_driver_zone_root");
        mDriverZoneRoot = rootId != 0 ? root.findViewById(rootId) : null;

        int hvacTempId = resolveRroId("sysui_driver_hvac_temp");
        mDriverHvacTempText = hvacTempId != 0 ? root.findViewById(hvacTempId) : null;
        mDriverHvacUp = findButton(root, "sysui_driver_hvac_up");
        mDriverHvacDown = findButton(root, "sysui_driver_hvac_down");

        int seatTempId = resolveRroId("sysui_driver_seat_temp");
        mDriverSeatTempText = seatTempId != 0 ? root.findViewById(seatTempId) : null;
        mDriverSeatUp = findButton(root, "sysui_driver_seat_up");
        mDriverSeatDown = findButton(root, "sysui_driver_seat_down");
    }

    private void bindPassengerZone(View root) {
        int rootId = resolveRroId("sysui_passenger_zone_root");
        mPassengerZoneRoot = rootId != 0 ? root.findViewById(rootId) : null;

        int hvacTempId = resolveRroId("sysui_passenger_hvac_temp");
        mPassengerHvacTempText = hvacTempId != 0 ? root.findViewById(hvacTempId) : null;
        mPassengerHvacUp = findButton(root, "sysui_passenger_hvac_up");
        mPassengerHvacDown = findButton(root, "sysui_passenger_hvac_down");

        int seatTempId = resolveRroId("sysui_passenger_seat_temp");
        mPassengerSeatTempText = seatTempId != 0 ? root.findViewById(seatTempId) : null;
        mPassengerSeatUp = findButton(root, "sysui_passenger_seat_up");
        mPassengerSeatDown = findButton(root, "sysui_passenger_seat_down");
    }

    private void bindChromeScrim(View root) {
        int uxId = resolveRroId("sysui_header_ux_scrim");
        mHeaderUxScrim = uxId != 0 ? root.findViewById(uxId) : null;
        int vhalId = resolveRroId("sysui_header_vhal_scrim");
        mHeaderVhalScrim = vhalId != 0 ? root.findViewById(vhalId) : null;
    }

    private ImageButton findButton(View root, String name) {
        int id = resolveRroId(name);
        return id != 0 ? root.findViewById(id) : null;
    }

    private void wireClickListeners() {
        if (mDriverHvacUp != null) {
            mDriverHvacUp.setOnClickListener(v -> adjustHvac(AREA_DRIVER, HVAC_STEP_CELSIUS));
        }
        if (mDriverHvacDown != null) {
            mDriverHvacDown.setOnClickListener(v -> adjustHvac(AREA_DRIVER, -HVAC_STEP_CELSIUS));
        }
        if (mDriverSeatUp != null) {
            mDriverSeatUp.setOnClickListener(v -> adjustSeat(AREA_DRIVER, SEAT_TEMP_STEP));
        }
        if (mDriverSeatDown != null) {
            mDriverSeatDown.setOnClickListener(v -> adjustSeat(AREA_DRIVER, -SEAT_TEMP_STEP));
        }
        if (mPassengerHvacUp != null) {
            mPassengerHvacUp.setOnClickListener(v -> adjustHvac(AREA_PASSENGER, HVAC_STEP_CELSIUS));
        }
        if (mPassengerHvacDown != null) {
            mPassengerHvacDown.setOnClickListener(
                    v -> adjustHvac(AREA_PASSENGER, -HVAC_STEP_CELSIUS));
        }
        if (mPassengerSeatUp != null) {
            mPassengerSeatUp.setOnClickListener(v -> adjustSeat(AREA_PASSENGER, SEAT_TEMP_STEP));
        }
        if (mPassengerSeatDown != null) {
            mPassengerSeatDown.setOnClickListener(
                    v -> adjustSeat(AREA_PASSENGER, -SEAT_TEMP_STEP));
        }
    }

    @Override
    protected void onControllerAttached() {
        mVhalHelper.subscribe(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0, mHvacCallback);
        mVhalHelper.subscribe(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, 0, mHvacCallback);
        readInitialValues();
        refreshAllDisplays();
    }

    @Override
    protected void onControllerDetached() {
        mVhalHelper.unsubscribe(VehiclePropertyIds.HVAC_TEMPERATURE_SET);
        mVhalHelper.unsubscribe(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE);
    }

    private void readInitialValues() {
        Float driverTemp = mVhalHelper.getFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET, AREA_DRIVER);
        if (driverTemp != null) {
            mDriverHvacTemp = driverTemp;
        }
        Float passengerTemp = mVhalHelper.getFloatProperty(
                VehiclePropertyIds.HVAC_TEMPERATURE_SET, AREA_PASSENGER);
        if (passengerTemp != null) {
            mPassengerHvacTemp = passengerTemp;
        }
        Integer driverSeat = mVhalHelper.getIntProperty(
                VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, AREA_DRIVER);
        if (driverSeat != null) {
            mDriverSeatTemp = driverSeat;
        }
        Integer passengerSeat = mVhalHelper.getIntProperty(
                VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, AREA_PASSENGER);
        if (passengerSeat != null) {
            mPassengerSeatTemp = passengerSeat;
        }
    }

    private void adjustHvac(int area, float delta) {
        if (mFullyRestricted || !mVhalConnected) {
            return;
        }
        float current = area == AREA_DRIVER ? mDriverHvacTemp : mPassengerHvacTemp;
        float next = current + delta;
        if (mVhalHelper.setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, area, next)) {
            updateHvacDisplay(area, next);
        }
    }

    private void adjustSeat(int area, int delta) {
        if (mFullyRestricted || !mVhalConnected) {
            return;
        }
        int current = area == AREA_DRIVER ? mDriverSeatTemp : mPassengerSeatTemp;
        int next = Math.max(SEAT_TEMP_MIN, Math.min(SEAT_TEMP_MAX, current + delta));
        if (mVhalHelper.setIntProperty(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE, area, next)) {
            updateSeatDisplay(area, next);
        }
    }

    private void updateHvacDisplay(int area, float temp) {
        String format = resolveRroString("sysui_hvac_temp_format");
        if (format == null) {
            format = "%.0f°";
        }
        String text = String.format(Locale.getDefault(), format, temp);
        if (area == AREA_DRIVER) {
            mDriverHvacTemp = temp;
            if (mDriverHvacTempText != null) {
                mDriverHvacTempText.setText(text);
            }
        } else if (area == AREA_PASSENGER) {
            mPassengerHvacTemp = temp;
            if (mPassengerHvacTempText != null) {
                mPassengerHvacTempText.setText(text);
            }
        }
    }

    private void updateSeatDisplay(int area, int seatTemp) {
        String text = String.valueOf(seatTemp);
        if (area == AREA_DRIVER) {
            mDriverSeatTemp = seatTemp;
            if (mDriverSeatTempText != null) {
                mDriverSeatTempText.setText(text);
            }
        } else if (area == AREA_PASSENGER) {
            mPassengerSeatTemp = seatTemp;
            if (mPassengerSeatTempText != null) {
                mPassengerSeatTempText.setText(text);
            }
        }
    }

    private void refreshAllDisplays() {
        updateHvacDisplay(AREA_DRIVER, mDriverHvacTemp);
        updateHvacDisplay(AREA_PASSENGER, mPassengerHvacTemp);
        updateSeatDisplay(AREA_DRIVER, mDriverSeatTemp);
        updateSeatDisplay(AREA_PASSENGER, mPassengerSeatTemp);
    }

    @Override
    protected void applyUxRestrictions() {
        setScrimVisible(mHeaderUxScrim, mFullyRestricted);
        boolean enabled = !mFullyRestricted && mVhalConnected;
        setViewEnabled(mDriverHvacUp, enabled);
        setViewEnabled(mDriverHvacDown, enabled);
        setViewEnabled(mDriverSeatUp, enabled);
        setViewEnabled(mDriverSeatDown, enabled);
        setViewEnabled(mPassengerHvacUp, enabled);
        setViewEnabled(mPassengerHvacDown, enabled);
        setViewEnabled(mPassengerSeatUp, enabled);
        setViewEnabled(mPassengerSeatDown, enabled);
    }

    @Override
    protected void applyVhalDisconnectedState() {
        setScrimVisible(mHeaderVhalScrim, true);
        applyUxRestrictions();
    }

    @Override
    protected void applyVhalConnectedState() {
        setScrimVisible(mHeaderVhalScrim, false);
        readInitialValues();
        refreshAllDisplays();
        applyUxRestrictions();
    }

    @Override
    protected void dumpControllerState(PrintWriter pw) {
        pw.println("  driverHvacTemp=" + mDriverHvacTemp + "C");
        pw.println("  passengerHvacTemp=" + mPassengerHvacTemp + "C");
        pw.println("  driverSeatTemp=" + mDriverSeatTemp);
        pw.println("  passengerSeatTemp=" + mPassengerSeatTemp);
        pw.println("  panelLayout=" + getLayoutResourceName());
    }
}
