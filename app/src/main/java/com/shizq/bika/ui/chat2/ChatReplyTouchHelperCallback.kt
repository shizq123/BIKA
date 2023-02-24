package com.shizq.bika.ui.chat2

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.shizq.bika.adapter.ChatMessageMultiAdapter
import com.shizq.bika.adapter.holder.ChatMessageSystemHolder


class ChatReplyTouchHelperCallback(val adapter :ChatMessageMultiAdapter) : ItemTouchHelper.Callback() {
    var mCurrentScrollX=0
    var mFirstInactive=false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeMovementFlags(ItemTouchHelper.LEFT, ItemTouchHelper.LEFT)
    }

//    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
//        return 0.2f
//    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (viewHolder !is ChatMessageSystemHolder) {
//            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            //后面加功能
//            if (dX == 0f) {
//                mCurrentScrollX = viewHolder.itemView.getScrollX();
//                mFirstInactive = true;
//            }
//            viewHolder.itemView.scrollTo((mCurrentScrollX + -dX).toInt(), 0);
        }
    }
}