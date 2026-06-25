package com.android.car.mockwidgets;

import android.app.Activity;
import android.car.Car;
import android.car.VehicleAreaSeat;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class ClimateActivity extends Activity {
    private static final String TAG = "ClimateActivity";
    private float mTemp = 16.0f; // Default if not read
    private boolean mAcOn = true;
    private boolean mAutoOn = false;

    private Car mCar;
    private CarPropertyManager mCarPropertyManager;
    private TextView mTxtTemp;
    private Button mBtnAc;
    private Button mBtnAuto;

    private static final int SEAT_AREA = 1; // ROW_1_LEFT

    private final CarPropertyManager.CarPropertyEventCallback mPropertyCallback =
            new CarPropertyManager.CarPropertyEventCallback() {
                @Override
                public void onChangeEvent(CarPropertyValue value) {
                    if (value == null) return;
                    int propId = value.getPropertyId();
                    if (propId == VehiclePropertyIds.HVAC_TEMPERATURE_SET) {
                        float temp = (Float) value.getValue();
                        mTemp = temp;
                        runOnUiThread(() -> updateTempDisplay());
                    } else if (propId == VehiclePropertyIds.HVAC_AC_ON) {
                        mAcOn = (Boolean) value.getValue();
                        runOnUiThread(() -> updateAcDisplay());
                    } else if (propId == VehiclePropertyIds.HVAC_AUTO_ON) {
                        mAutoOn = (Boolean) value.getValue();
                        runOnUiThread(() -> updateAutoDisplay());
                    }
                }

                @Override
                public void onErrorEvent(int propertyId, int zone) {
                    Log.e(TAG, "Error event for property " + propertyId);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.climate_widget);

        mTxtTemp = findViewById(R.id.txt_temperature);
        mBtnAc = findViewById(R.id.btn_ac_toggle);
        mBtnAuto = findViewById(R.id.btn_auto_toggle);
        ImageButton btnDown = findViewById(R.id.btn_temp_down);
        ImageButton btnUp = findViewById(R.id.btn_temp_up);

        btnDown.setOnClickListener(v -> {
            mTemp -= 1.0f;
            updateTempDisplay();
            setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, mTemp);
        });

        btnUp.setOnClickListener(v -> {
            mTemp += 1.0f;
            updateTempDisplay();
            setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, mTemp);
        });

        mBtnAc.setOnClickListener(v -> {
            mAcOn = !mAcOn;
            updateAcDisplay();
            setBooleanProperty(VehiclePropertyIds.HVAC_AC_ON, mAcOn);
        });

        mBtnAuto.setOnClickListener(v -> {
            mAutoOn = !mAutoOn;
            updateAutoDisplay();
            setBooleanProperty(VehiclePropertyIds.HVAC_AUTO_ON, mAutoOn);
        });

        mCar = Car.createCar(this, null, Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER, (car, ready) -> {
            if (ready) {
                mCarPropertyManager = (CarPropertyManager) car.getCarManager(Car.PROPERTY_SERVICE);
                if (mCarPropertyManager != null) {
                    mCarPropertyManager.registerCallback(mPropertyCallback, VehiclePropertyIds.HVAC_TEMPERATURE_SET, CarPropertyManager.SENSOR_RATE_ONCHANGE);
                    mCarPropertyManager.registerCallback(mPropertyCallback, VehiclePropertyIds.HVAC_AC_ON, CarPropertyManager.SENSOR_RATE_ONCHANGE);
                    mCarPropertyManager.registerCallback(mPropertyCallback, VehiclePropertyIds.HVAC_AUTO_ON, CarPropertyManager.SENSOR_RATE_ONCHANGE);
                }
            }
        });
    }

    private void setFloatProperty(int propId, float val) {
        if (mCarPropertyManager != null) {
            try {
                mCarPropertyManager.setFloatProperty(propId, SEAT_AREA, val);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set float property " + propId, e);
            }
        }
    }

    private void setBooleanProperty(int propId, boolean val) {
        if (mCarPropertyManager != null) {
            try {
                mCarPropertyManager.setBooleanProperty(propId, SEAT_AREA, val);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set bool property " + propId, e);
            }
        }
    }

    private void updateTempDisplay() {
        // If VHAL expects celsius, value is around 16-32.
        // For the sake of the mock widget UI matching SystemUI directly,
        // SystemUI converts Celsius to Fahrenheit if the locale uses F, 
        // or uses raw values if the HVAC properties are already F.
        // We'll just display the integer value and add a degree symbol.
        mTxtTemp.setText(Math.round(mTemp) + "°");
    }

    private void updateAcDisplay() {
        mBtnAc.setText(mAcOn ? "A/C ON" : "A/C OFF");
        mBtnAc.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                mAcOn ? 0x4DFFFFFF : 0x1AFFFFFF));
    }

    private void updateAutoDisplay() {
        mBtnAuto.setText(mAutoOn ? "AUTO: ON" : "AUTO");
        mBtnAuto.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                mAutoOn ? 0x4DFFFFFF : 0x1AFFFFFF));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCar != null && mCar.isConnected()) {
            if (mCarPropertyManager != null) {
                mCarPropertyManager.unregisterCallback(mPropertyCallback);
            }
            mCar.disconnect();
        }
    }
}
