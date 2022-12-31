package com.shizq.bika.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shizq.bika.R;
import com.shizq.bika.bean.ChatMessageBean;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    //存放数据
    List<ChatMessageBean> chatModelList;


    //通过构造函数传入数据
    public ChatAdapter(List<ChatMessageBean> data) {
        this.chatModelList = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //布局加载器
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (chatModelList.get(position).getType().equals("send")) {
            holder.leftLayout.setVisibility(View.GONE);
            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.rightNameTextView.setText(chatModelList.get(position).getName());
            holder.rightContentTextView.setText(chatModelList.get(position).getMessage());
        } else {
            holder.rightLayout.setVisibility(View.GONE);
            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.leftNameTextView.setText(chatModelList.get(position).getName());
            holder.leftContentTextView.setText(chatModelList.get(position).getMessage());
        }

    }

    @Override
    public int getItemCount() {
        return chatModelList.size();
    }


    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView leftNameTextView;
        TextView leftContentTextView;
        ViewGroup leftLayout;

        TextView rightNameTextView;
        TextView rightContentTextView;
        ViewGroup rightLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            leftContentTextView = itemView.findViewById(R.id.chat_content_l);
            leftNameTextView =  itemView.findViewById(R.id.chat_name_l);
            leftLayout = itemView.findViewById(R.id.chat_layout_l);

            rightContentTextView = itemView.findViewById(R.id.chat_content_r);
            rightNameTextView = itemView.findViewById(R.id.chat_name_r);
            rightLayout = itemView.findViewById(R.id.chat_layout_r);

        }
    }
}
