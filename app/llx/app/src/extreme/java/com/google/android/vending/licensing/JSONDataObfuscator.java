/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.google.android.vending.licensing;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import net.pierrox.lightning_launcher.data.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;


public class JSONDataObfuscator {

    private static final byte[] SALT = new byte[] {
            42, 13, 27, -7, 1, 63, -51, 94, 107, 120, 4,
            -54, -77, 123, -12, -72, -99, 10, 0, 73
    };

    private File mFile;
    private String mPackageName;
    private JSONObject mData;
    private final Obfuscator mObfuscator;


    public JSONDataObfuscator(Context context) {
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        mPackageName = context.getPackageName();
        mFile = new File(context.getCacheDir(), "icon");
        mObfuscator = new AESObfuscator(SALT, mPackageName, deviceId);
        String obfuscated_data = FileUtils.readFileContent(mFile);
        if(obfuscated_data != null) {
            try {
                String data = mObfuscator.unobfuscate(obfuscated_data, context.getPackageName());
                if(data != null) {
                    mData = new JSONObject(data);
                }
            } catch (Exception e) {
                // pass
            }

        }
        if(mData == null) {
            mData = new JSONObject();
        }
    }

    public void putString(String key, String value) {
        try {
            mData.put(key, value);
        } catch (JSONException e) {
            // pass
        }
    }

    public String getString(String key, String defValue) {
        return mData.optString(key, defValue);
    }

    public void commit() {
        String data = mData.toString();
        String obfuscated_data = mObfuscator.obfuscate(data, mPackageName);
        try {
            FileUtils.saveStringToFile(obfuscated_data, mFile);
        } catch (IOException e) {
            // pass
        }
    }
}
