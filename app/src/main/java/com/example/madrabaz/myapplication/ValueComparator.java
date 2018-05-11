package com.example.madrabaz.myapplication;

import java.util.Comparator;
import java.util.Map;

/**
 * Created by Madrabaz on 19.3.2018.
 */

public class ValueComparator implements Comparator<Float> {

    private Map<Float, Weight> map;

    public ValueComparator(Map<Float, Weight> base) {
        this.map = base;
    }

    @Override
    public int compare(Float a, Float b) {
        if (map.get(a).getAverage() >= map.get(b).getAverage()) {
            return -1;
        } else {
            return 1;
        }
    }
}
