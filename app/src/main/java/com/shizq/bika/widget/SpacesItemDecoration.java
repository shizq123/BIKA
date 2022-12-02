package com.shizq.bika.widget;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

//给RecyclerView设置左右边距
public class SpacesItemDecoration extends RecyclerView.ItemDecoration {
    private final int space;
    private final List<?> list;


    public SpacesItemDecoration(int space, List<?> list) {
        this.space = space;
        this.list = list;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
        if (parent.getChildLayoutPosition(view) == 0) outRect.left = space;//设置第一个item左边距
        if (parent.getChildAdapterPosition(view) == list.size() - 1) outRect.right = space;//设置最后一个item右边距
    }

    //将像素转换成dp
    public static int px2dp(float dpValue) {
        return (int) (0.5f + dpValue * Resources.getSystem().getDisplayMetrics().density);
    }
}
