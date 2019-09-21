package com.microtechmd.pda.entity;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/1/18.
 */

public class CalibrationHistory implements Serializable {
    private long time;
    private float value;

    public CalibrationHistory() {

    }

    public CalibrationHistory(long time, float value) {
        this.time = time;
        this.value = value;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
