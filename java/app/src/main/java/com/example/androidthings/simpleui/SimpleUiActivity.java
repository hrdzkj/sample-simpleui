/*
 * Copyright 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.simpleui;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleUiActivity extends Activity {

    private static final String TAG = SimpleUiActivity.class.getSimpleName();

    private Map<String, Gpio> mGpioMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout gpioPinsView = findViewById(R.id.gpio_list);
        LayoutInflater inflater = getLayoutInflater();
        // PeripheralManager主要用于获取GPIO、SPI（串行外设接口）、Uart（串口）、I2C（串行总线）、PWM（脉冲宽度调节）列表，与打开对应的外设。
        PeripheralManager pioManager = PeripheralManager.getInstance();

        // getGpioList获取到所有可用的端口名称,进行遍历
        for (String name : pioManager.getGpioList()) {
            //学会一种动态加载View到ViewGroup到方法
            View child = inflater.inflate(R.layout.list_item_gpio, gpioPinsView, false);
            Switch button = child.findViewById(R.id.gpio_switch);
            button.setText(name);
            gpioPinsView.addView(button);
            Log.d(TAG, "Added button for GPIO: " + name);

            try {
                final Gpio ledPin = pioManager.openGpio(name); //根据端口名称打开端口，连接到该端口
                //设置触发类型：edge_none无触发(默认值),edge_rising上升沿触发,edge_falling下降沿触发，edge_both上升、下降沿两者都触发。
                ledPin.setEdgeTriggerType(Gpio.EDGE_NONE);
                ledPin.setActiveType(Gpio.ACTIVE_HIGH);//设置激活类型：高电平或低电平
                ledPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW); //设置方向是输出，初始化低电平
                /*
                 RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOB,ENABLE);//Arm使能时钟
                 ARM比Android Things的多了时钟频率设置与使能。ARM中的时钟分为内部低速、内部高速、外部低速与外部高速，
                 主要是为了节约功耗，可灵活根据需求选择时钟并且单独使能。我猜测，Android Things操作系统应该是默认了时钟频率，
                 才不需要单独配置的。
                */
                //设置开关状态改变事件
                button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        try {
                            // 使能该端口
                            ledPin.setValue(isChecked);
                        } catch (IOException e) {
                            Log.e(TAG, "error toggling gpio:", e);
                            buttonView.setOnCheckedChangeListener(null);
                            // reset button to previous state.
                            buttonView.setChecked(!isChecked);
                            buttonView.setOnCheckedChangeListener(this);
                        }
                    }
                });

                // 保存已经打开的GPIO资源，为了以后释放资源
                mGpioMap.put(name, ledPin);
            } catch (IOException e) { //打开端口失败
                Log.e(TAG, "Error initializing GPIO: " + name, e);
                // disable button
                button.setEnabled(false);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放已经打开的GPIO资源
        for (Map.Entry<String, Gpio> entry : mGpioMap.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing GPIO " + entry.getKey(), e);
            }
        }
        mGpioMap.clear();
    }
}
