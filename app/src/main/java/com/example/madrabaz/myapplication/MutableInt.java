package com.example.madrabaz.myapplication;

import android.support.annotation.NonNull;

/**
 * Created by Madrabaz on 6.3.2018.
 */

public class MutableInt implements Comparable<MutableInt> {
    int value = 1; // note that we start at 1 since we're counting
    public void increment () { ++value;      }
    public int  getValue ()       { return value; }

    @Override
    public int compareTo(@NonNull MutableInt o) {
        return Integer.compare(this.getValue(), o.getValue());
    }
}
