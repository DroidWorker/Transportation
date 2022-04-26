package com.app.transportation.data

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.app.transportation.core.dpToPx

class RVDecoration(private val space: Int, private val left: Boolean) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)

        if (left)
            outRect.left = view.context.dpToPx(space)
        else
            outRect.right = view.context.dpToPx(space)
    }

}