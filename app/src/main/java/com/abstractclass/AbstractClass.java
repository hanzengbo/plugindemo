package com.abstractclass;

import android.util.Log;

/**
 * Created by zengbo1 on 2018/3/26.
 */

public abstract class AbstractClass {

    public void doHandler() {
        Log.e("AbstractClass", "do Handler");
    }

    public abstract void doAbstractHandler();
}
