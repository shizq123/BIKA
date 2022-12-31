package com.shizq.bika.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shizq.bika.R;
import com.shizq.bika.bean.ChatMessageBean;
import com.shizq.bika.utils.GlideApp;
import com.shizq.bika.utils.GlideUrlNewKey;

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

        if (String.valueOf(chatModelList.get(position).getType()).equals("100")) {
            holder.leftLayout.setVisibility(View.GONE);
            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.rightNameTextView.setText(chatModelList.get(position).getName());
            holder.rightContentTextView.setText(chatModelList.get(position).getMessage());
        } else {
            holder.rightLayout.setVisibility(View.GONE);
            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.leftNameTextView.setText(chatModelList.get(position).getName());
            if (chatModelList.get(position).getCharacter() != null && !chatModelList.get(position).getCharacter().equals("")) {
                GlideApp.with(holder.itemView)
                        .load(chatModelList.get(position).getCharacter())
                        .into(holder.leftCharacterImage);
            }
            if (chatModelList.get(position).getAvatar() != null && !chatModelList.get(position).getAvatar().equals("")) {
                //拆分 利于缓存 省流量 加载更快
                int i = chatModelList.get(position).getAvatar().indexOf("/static/");
                GlideApp.with(holder.itemView)
                        .load(chatModelList.get(position).getAvatar() != null && !chatModelList.get(position).getAvatar().equals("") ?
                                new GlideUrlNewKey(chatModelList.get(position).getAvatar().substring(0, i), chatModelList.get(position).getAvatar().substring(i + 8))
                                : R.drawable.placeholder_avatar_2)
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .into(holder.leftAvatarImage);
            }

            if (chatModelList.get(position).getImage() != null && !chatModelList.get(position).getImage().equals("")) {
                //这里要处理图片
                holder.leftContentTextView.setText("[有图片]");
            } else if (chatModelList.get(position).getAudio() != null && !chatModelList.get(position).getAudio().equals("")) {
                //这里要处理语音
                holder.leftContentTextView.setText("[有语音]");
            } else {
                holder.leftContentTextView.setText(chatModelList.get(position).getMessage());
            }
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
        ImageView leftCharacterImage;
        ImageView leftAvatarImage;

        TextView rightNameTextView;
        TextView rightContentTextView;
        ViewGroup rightLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            leftContentTextView = itemView.findViewById(R.id.chat_content_l);
            leftNameTextView = itemView.findViewById(R.id.chat_name_l);
            leftLayout = itemView.findViewById(R.id.chat_layout_l);
            leftCharacterImage = itemView.findViewById(R.id.chat_character_l);
            leftAvatarImage = itemView.findViewById(R.id.chat_avatar_l);

            rightContentTextView = itemView.findViewById(R.id.chat_content_r);
            rightNameTextView = itemView.findViewById(R.id.chat_name_r);
            rightLayout = itemView.findViewById(R.id.chat_layout_r);

        }
    }
}
