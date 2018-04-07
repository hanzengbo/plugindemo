package com.example.zengbo1.plugintest;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.abstractclass.ITestInterface;
import com.abstractclass.RealClass;
import com.test.lib.MyTestLib;
import com.test.lib.MyTestLibUtil;

public class MainActivity extends Activity implements ITestInterface{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RealClass test1 = new RealClass();
        test1.doAbstractHandler();
        test1.doHandler();
        test1.setTestInterface(this);
        test1.doSelfHandler();
        doHandleProjectOne();
        callTestLibFunction();
    }

    @Override
    public void onTestInterface() {
        Log.e("MainActivity", "onTestInterface");
    }

    private void doHandleProjectOne() {
        findViewById(R.id.btProjectOne).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri=Uri.parse("plugintest://projectone");
                Intent intent=new Intent(Intent.ACTION_VIEW,uri);
                try {
                    MainActivity.this.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Intent intent = new Intent(MainActivity.this, ProjectOneActivity.class);
                //MainActivity.this.startActivity(intent);
            }
        });
    }

    private void callTestLibFunction() {
        MyTestLib myTestLib = new MyTestLib();
        myTestLib.doTestLib();
        MyTestLibUtil myTestLibUtil = new MyTestLibUtil();
        myTestLibUtil.doTestLibUtil();
    }
}
