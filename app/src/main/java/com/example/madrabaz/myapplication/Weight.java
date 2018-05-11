package com.example.madrabaz.myapplication;

/**
 * Created by Madrabaz on 10.4.2018.
 */

public class Weight {

    private float magnitude;
    private int times;

    Weight(float magnitude){
        this.magnitude = magnitude;
        times = 1;
    }

    Weight addMagnitude(float magnitude){
        this.magnitude += magnitude;
        times++;
        return this;
    }

    float getAverage(){
        //return magnitude*times;
		//return times;
        return magnitude;
    }

    public float getMagnitude(){
        return magnitude;
    }

    public int getTimes(){
        return times;
    }
}
