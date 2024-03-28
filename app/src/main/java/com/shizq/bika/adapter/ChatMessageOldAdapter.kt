package com.shizq.bika.adapter

import android.graphics.Bitmap
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.shizq.bika.BIKAApplication
import com.shizq.bika.R
import com.shizq.bika.base.BaseBindingAdapter
import com.shizq.bika.base.BaseBindingHolder
import com.shizq.bika.bean.ChatMessageOldBean
import com.shizq.bika.databinding.ItemChatMessageOldBinding
import com.shizq.bika.utils.Base64Util
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.SPUtil
import com.shizq.bika.utils.dp

//旧聊天页面 乱
class ChatMessageOldAdapter :
    BaseBindingAdapter<ChatMessageOldBean, ItemChatMessageOldBinding>(R.layout.item_chat_message_old) {
    override fun bindView(
        holder: BaseBindingHolder<*, *>,
        bean: ChatMessageOldBean,
        binding: ItemChatMessageOldBinding,
        position: Int
    ) {
        //设置一个回复类消息的最小宽度 类似qq

        if (bean.name == null && bean.user_id == null && bean.message != null) {
            //通知（加入，踢人，悄悄话）
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.VISIBLE
            binding.chatNotification.text = bean.message
        } else if (bean.name != null && bean.name == SPUtil.get(
                BIKAApplication.contextBase,
                "user_name",
                ""
            ) as String
        ) {
            //我发送的消息
            binding.chatLayoutL.visibility = View.GONE
            binding.chatLayoutR.visibility = View.VISIBLE
            binding.chatNotification.visibility = View.GONE
            binding.chatNameR.text = bean.name

            //头像框
            GlideApp.with(holder.itemView)
                .load(if (bean.character != null && bean.character != "") bean.character else "")
                .into(binding.chatCharacterR)

            //头像
            GlideApp.with(holder.itemView)
                .load(
                    if (bean.avatar != null && bean.avatar != "") {
                        val i: Int = bean.avatar.indexOf("/static/")
                        if (i > 0)
                            GlideUrlNewKey(
                                bean.avatar.substring(0, i),
                                bean.avatar.substring(i + 8)
                            )
                        else bean.avatar
                    } else R.drawable.placeholder_avatar_2

                )
                .placeholder(R.drawable.placeholder_avatar_2)
                .into(binding.chatAvatarR)
            if (bean.reply_name != null && bean.reply_name != "") {
                binding.chatReplyLayoutR.visibility = ViewGroup.VISIBLE
                binding.chatReplyNameR.text = bean.reply_name
                if (bean.reply.length > 50) {
                    //要改 TODO  显示两行 尾部显示...
                    binding.chatReplyR.text = bean.reply.substring(0, 50) + "..."
                } else {
                    binding.chatReplyR.text = bean.reply
                }

            } else {
                binding.chatReplyLayoutR.visibility = ViewGroup.GONE
            }

            if (bean.level >= 0) {
                //等级
                binding.chatLevelR.text = "Lv." + bean.level
            }

            binding.chatVerifiedR.visibility = if (bean.verified) View.VISIBLE else View.GONE

            if (bean.at != null && bean.at != "") {
                //艾特某人
                binding.chatAtR.visibility = View.VISIBLE
                binding.chatAtR.text = bean.at.replace("嗶咔_", "@")
            } else {
                binding.chatAtR.visibility = View.GONE
            }

            //显示时间 后面加设置显示隐藏
//            String time = "";
//            if (chatModelList.get(position).getPlatform() != null && !chatModelList.get(position).getPlatform().equals("")) {
//                time = chatModelList.get(position).getPlatform();
//            }
//            time += " " + TimeUtil.getTime();
//            holder.chat_time_r.setText(time);

            if (bean.image != null && bean.image != "") {
                binding.chatContentImageR.visibility = View.VISIBLE
                //图片要处理宽高问题
                setImageView(binding.chatContentImageR, Base64Util().base64ToBitmap(bean.image))
            } else {
                binding.chatContentImageR.visibility = View.GONE
            }
            if (bean.audio != null && bean.audio != "") {
                //这里要处理语音
                binding.chatVoiceR.visibility = View.VISIBLE
            } else {
                binding.chatVoiceR.visibility = View.GONE
            }

            if (bean.message != null && bean.message != "") {
                binding.chatContentR.visibility = View.VISIBLE
                if (bean.event_colors != null && bean.event_colors.isNotEmpty()) {
                    setTextViewStyles(binding.chatContentR, bean.event_colors, bean.message)

                } else {
                    binding.chatContentR.text = bean.message

                }
            } else {
                binding.chatContentR.visibility = View.GONE
            }

        } else {
            //接收消息
            binding.chatLayoutL.visibility = View.VISIBLE
            binding.chatLayoutR.visibility = View.GONE
            binding.chatNotification.visibility = View.GONE
            binding.chatNameL.text = bean.name

            //头像框
            GlideApp.with(holder.itemView)
                .load(if (bean.character != null && bean.character != "") bean.character else "")
                .into(binding.chatCharacterL)

            //头像 //拆分 利于缓存 省流量 加载更快
            GlideApp.with(holder.itemView)
                .load(
                    if (bean.avatar != null && bean.avatar != "") {
                        val i: Int = bean.avatar.indexOf("/static/")
                        if (i > 0) {
                            GlideUrlNewKey(
                                bean.avatar.substring(0, i),
                                bean.avatar.substring(i + 8)
                            )
                        } else bean.avatar
                    } else R.drawable.placeholder_avatar_2

                )
                .placeholder(R.drawable.placeholder_avatar_2)
                .into(binding.chatAvatarL)

            if (bean.reply_name != null && bean.reply_name != "") {
                binding.chatReplyLayout.visibility = ViewGroup.VISIBLE
                binding.chatReplyName.text = bean.reply_name
                if (bean.reply.length > 50) {
                    //要改 TODO  显示两行 尾部显示...
                    binding.chatReply.text = bean.reply.substring(0, 50) + "..."
                } else {
                    binding.chatReply.text = bean.reply
                }

            } else {
                binding.chatReplyLayout.visibility = ViewGroup.GONE
            }

            if (bean.level >= 0) {
                //等级
                binding.chatLevelL.text = "Lv." + bean.level
            }

            binding.chatVerified.visibility = if (bean.verified) View.VISIBLE else View.GONE

            if (bean.at != null && bean.at != "") {
                //艾特某人
                binding.chatAtL.visibility = View.VISIBLE
                binding.chatAtL.text = bean.at.replace("嗶咔_", "@")
            } else {
                binding.chatAtL.visibility = View.GONE
            }

            //显示时间 后面加设置显示隐藏
//            String time = "";
//            if (chatModelList.get(position).getPlatform() != null && !chatModelList.get(position).getPlatform().equals("")) {
//                time = chatModelList.get(position).getPlatform();
//            }
//            time += " " + TimeUtil.getTime();
//            holder.chat_time_l.setText(time);

            if (bean.image != null && bean.image != "") {
                binding.chatContentImageL.visibility = View.VISIBLE
                //图片要处理宽高问题
                setImageView(binding.chatContentImageL, Base64Util().base64ToBitmap(bean.image))
            } else {
                binding.chatContentImageL.visibility = View.GONE
            }
            if (bean.audio != null && bean.audio != "") {
                //这里要处理语音
                binding.chatVoiceL.visibility = View.VISIBLE
            } else {
                binding.chatVoiceL.visibility = View.GONE
            }

            if (bean.message != null && bean.message != "") {
                binding.chatContentL.visibility = View.VISIBLE
                if (bean.event_colors != null && bean.event_colors.isNotEmpty()) {
                    setTextViewStyles(binding.chatContentL, bean.event_colors, bean.message)

                } else {
                    binding.chatContentL.text = bean.message

                }
            } else {
                binding.chatContentL.visibility = View.GONE
            }
        }

        holder.addOnClickListener(R.id.chat_avatar_layout_l)
        holder.addOnClickListener(R.id.chat_name_l)
        holder.addOnLongClickListener(R.id.chat_avatar_layout_l)
        holder.addOnClickListener(R.id.chat_message_layout_l)
        holder.addOnClickListener(R.id.chat_message_layout_r)
    }

    private fun setImageView(imageView: ImageView, bitmap: Bitmap) {
        //手机截图比例的图片防止占满屏
        val bitmapH = bitmap.height
        val bitmapW = bitmap.width
        val imageMinW = 150.dp
        val layoutParams = imageView.layoutParams
        if (bitmapH > 1.7 * bitmapW) {
            layoutParams.width = imageMinW
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else if (bitmapW < 1080) {
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        imageView.layoutParams = layoutParams
        imageView.setImageBitmap(bitmap)
    }

    //反编译源码 聊天室的彩色字体
    private fun setTextViewStyles(textView: TextView, strArr: List<String>, str: String) {
        textView.text = ""
        var i = 0
        while (i < str.length) {
            val i2 = i + 1
            if (i2 >= str.length || !(str[i].code == 55356 || str[i].code == 55357)) {
                val spannableString = SpannableString(str[i].toString() + "")
                str[i]
                spannableString.setSpan(
                    ForegroundColorSpan(
                        Color.parseColor(
                            strArr[i % strArr.size]
                        )
                    ), 0, spannableString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textView.append(spannableString)
            } else {
                val substring = str.substring(i, i + 2)
                val spannableString2 = SpannableString(substring + "")
                spannableString2.setSpan(
                    ForegroundColorSpan(
                        Color.parseColor(
                            strArr[i % strArr.size]
                        )
                    ), 0, spannableString2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                textView.append(substring)
                i = i2
            }
            i++
        }
    }
}