/*
 * Copyright 2017 The Android Open Source Project
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

package com.app.transportation.main.core.selection;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

/**
 * Register an OnItemActivatedListener to be notified when an item is activated
 * (tapped or double clicked).
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
public interface OnItemActivatedListener<K> {

    /**
     * Called when an item is "activated". An item is activated, for example, when no selection
     * exists and the user taps an item with her finger, or double clicks an item with a
     * pointing device like a Mouse.
     *
     * @param item details of the item.
     * @param e the event associated with item.
     *
     * @return true if the event was handled.
     */
    boolean onItemActivated(@NonNull ItemDetailsLookup.ItemDetails<K> item, @NonNull MotionEvent e);
}
