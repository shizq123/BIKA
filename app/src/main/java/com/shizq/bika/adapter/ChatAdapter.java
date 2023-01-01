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
import com.shizq.bika.utils.TimeUtil;

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

        //设置一个回复消息的最小宽度 类似qq

        if (chatModelList.get(position).getName()==null
                &&chatModelList.get(position).getUser_id()==null
                &&chatModelList.get(position).getMessage()!=null) {
            //通知 悄悄话
            holder.leftLayout.setVisibility(View.GONE);
            holder.rightLayout.setVisibility(View.GONE);
            holder.chat_notification.setVisibility(View.VISIBLE);
            holder.chat_notification.setText(chatModelList.get(position).getMessage());
        } else if (String.valueOf(chatModelList.get(position).getType()).equals("100")) {
            //我发送的消息
            holder.leftLayout.setVisibility(View.GONE);
            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.chat_notification.setVisibility(View.GONE);
            holder.rightNameTextView.setText(chatModelList.get(position).getName());
            holder.rightContentTextView.setText(chatModelList.get(position).getMessage());
        } else {
            //接收消息
            holder.rightLayout.setVisibility(View.GONE);
            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.chat_notification.setVisibility(View.GONE);
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
                                i > 0 ?
                                        new GlideUrlNewKey(chatModelList.get(position).getAvatar().substring(0, i), chatModelList.get(position).getAvatar().substring(i + 8))
                                        : chatModelList.get(position).getAvatar()
                                : R.drawable.placeholder_avatar_2)
                        .placeholder(R.drawable.placeholder_avatar_2)
                        .into(holder.leftAvatarImage);
            }
            if (chatModelList.get(position).getReply_name() != null && !chatModelList.get(position).getReply_name().equals("")){
                holder.chat_reply_layout.setVisibility(ViewGroup.VISIBLE);
                holder.chat_reply_name.setText(chatModelList.get(position).getReply_name());
                if (chatModelList.get(position).getReply().length() > 50) {
                    //要改 显示两行 尾部显示...
                    holder.chat_reply.setText(chatModelList.get(position).getReply().substring(0,50)+"...");
                } else {
                    holder.chat_reply.setText(chatModelList.get(position).getReply());
                }
            }else {
                holder.chat_reply_layout.setVisibility(ViewGroup.GONE);
            }

            if (chatModelList.get(position).getLevel() >= 0) {
                //等级
                holder.chat_level_l.setText("Lv."+chatModelList.get(position).getLevel());
            }

            if (chatModelList.get(position).getAt() != null && !chatModelList.get(position).getAt().equals("")) {
                //艾特某人
                holder.chat_at_l.setVisibility(View.VISIBLE);
                holder.chat_at_l.setText("@"+chatModelList.get(position).getAt().replace("嗶咔_",""));
            }else {
                holder.chat_at_l.setVisibility(View.GONE);
            }

            //显示时间 后面加设置显示隐藏
//            String time = "";
//            if (chatModelList.get(position).getPlatform() != null && !chatModelList.get(position).getPlatform().equals("")) {
//                time = chatModelList.get(position).getPlatform();
//            }
//            time += " " + TimeUtil.getTime();
//            holder.chat_time_l.setText(time);


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
        TextView chat_level_l;
        TextView leftContentTextView;
        ViewGroup leftLayout;
        ImageView leftCharacterImage;
        ImageView leftAvatarImage;
        ViewGroup chat_reply_layout;
        TextView chat_reply_name;
        TextView chat_reply;
        TextView chat_at_l;
        TextView chat_time_l;

        TextView rightNameTextView;
        TextView rightContentTextView;
        ViewGroup rightLayout;

        TextView chat_notification;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            leftContentTextView = itemView.findViewById(R.id.chat_content_l);
            leftNameTextView = itemView.findViewById(R.id.chat_name_l);
            chat_level_l = itemView.findViewById(R.id.chat_level_l);
            chat_at_l = itemView.findViewById(R.id.chat_at_l);
            leftLayout = itemView.findViewById(R.id.chat_layout_l);
            leftCharacterImage = itemView.findViewById(R.id.chat_character_l);
            leftAvatarImage = itemView.findViewById(R.id.chat_avatar_l);
            chat_reply_layout = itemView.findViewById(R.id.chat_reply_layout);
            chat_reply_name = itemView.findViewById(R.id.chat_reply_name);
            chat_reply = itemView.findViewById(R.id.chat_reply);
            chat_time_l = itemView.findViewById(R.id.chat_time_l);

            rightContentTextView = itemView.findViewById(R.id.chat_content_r);
            rightNameTextView = itemView.findViewById(R.id.chat_name_r);
            rightLayout = itemView.findViewById(R.id.chat_layout_r);

            chat_notification = itemView.findViewById(R.id.chat_notification);

        }
    }
}
