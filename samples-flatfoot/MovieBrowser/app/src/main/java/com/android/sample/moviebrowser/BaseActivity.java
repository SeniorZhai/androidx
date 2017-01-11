/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.sample.moviebrowser;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.android.support.lifecycle.ActivityLifecycleDispatcher;
import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleProvider;

/**
 * Temporary base activity that acts as lifecycle provider.
 */
public abstract class BaseActivity extends AppCompatActivity implements LifecycleProvider {
    private final ActivityLifecycleDispatcher mDispatcher = new ActivityLifecycleDispatcher(this,
            this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDispatcher.onCreate();
    }

    @Override
    protected void onResume() {
        mDispatcher.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mDispatcher.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mDispatcher.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mDispatcher.onDestroy();
        super.onDestroy();
    }

    @Override
    public Lifecycle getLifecycle() {
        return mDispatcher.getLifecycle();
    }
}
