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

import static com.app.transportation.main.core.selection.Shared.DEBUG;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener;

/**
 * An {@link ItemKeyProvider} that provides item keys by way of native
 * {@link RecyclerView.Adapter} stable ids.
 *
 * <p>The corresponding RecyclerView.Adapter instance must:
 * <ol>
 *     <li> Enable stable ids using {@link RecyclerView.Adapter#setHasStableIds(boolean)}
 *     <li> Override {@link RecyclerView.Adapter#getItemId(int)} with a real implementation.
 * </ol>
 *
 * <p>
 * There are trade-offs with this implementation:
 * <ul>
 *     <li>It necessarily auto-boxes {@code long} stable id values into {@code Long} values for
 *     use as selection keys.
 *     <li>It deprives Chromebook users (actually, any device with an attached pointer) of support
 *     for band-selection.
 * </ul>
 *
 * <p>See com.example.android.supportv7.widget.selection.fancy.DemoAdapter.KeyProvider in the
 * SupportV7 Demos package for an example of how to implement a better ItemKeyProvider.
 */
public final class StableIdKeyProvider extends ItemKeyProvider<Long> {

    private static final String TAG = "StableIdKeyProvider";

    private final SparseArray<Long> mPositionToKey = new SparseArray<>();
    private final LongSparseArray<Integer> mKeyToPosition = new LongSparseArray<>();
    private final RecyclerView mRecyclerView;

    /**
     * Creates a new key provider that uses cached {@code long} stable ids associated
     * with the RecyclerView items.
     *
     * @param recyclerView the owner RecyclerView
     */
    public StableIdKeyProvider(@NonNull RecyclerView recyclerView) {

        // Since this provide is based on stable ids based on whats laid out in the window
        // we can only satisfy "window" scope key access.
        super(SCOPE_CACHED);

        mRecyclerView = recyclerView;

        mRecyclerView.addOnChildAttachStateChangeListener(
                new OnChildAttachStateChangeListener() {
                    @Override
                    public void onChildViewAttachedToWindow(View view) {
                        onAttached(view);
                    }

                    @Override
                    public void onChildViewDetachedFromWindow(View view) {
                        onDetached(view);
                    }
                }
        );

    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onAttached(@NonNull View view) {
        RecyclerView.ViewHolder holder = mRecyclerView.findContainingViewHolder(view);
        if (holder == null) {
            if (DEBUG) {
                Log.w(TAG, "Unable to find ViewHolder for View. Ignoring onAttached event.");
            }
            return;
        }
        int position = holder.getBindingAdapterPosition();
        long id = holder.getItemId();
        if (position != RecyclerView.NO_POSITION && id != RecyclerView.NO_ID) {
            mPositionToKey.put(position, id);
            mKeyToPosition.put(id, position);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onDetached(@NonNull View view) {
        RecyclerView.ViewHolder holder = mRecyclerView.findContainingViewHolder(view);
        if (holder == null) {
            if (DEBUG) {
                Log.w(TAG, "Unable to find ViewHolder for View. Ignoring onDetached event.");
            }
            return;
        }
        int position = holder.getBindingAdapterPosition();
        long id = holder.getItemId();
        if (position != RecyclerView.NO_POSITION && id != RecyclerView.NO_ID) {
            mPositionToKey.delete(position);
            mKeyToPosition.remove(id);
        }
    }

    @Override
    public @Nullable Long getKey(int position) {
        return mPositionToKey.get(position, null);
    }

    @Override
    public int getPosition(@NonNull Long key) {
        return mKeyToPosition.get(key, RecyclerView.NO_POSITION);
    }
}
