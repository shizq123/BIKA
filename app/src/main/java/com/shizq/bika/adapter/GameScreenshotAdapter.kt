package com.shizq.bika.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.shizq.bika.R
import com.shizq.bika.bean.GameInfoBean
import com.shizq.bika.utils.GlideApp
import com.shizq.bika.utils.GlideUrlNewKey
import com.shizq.bika.utils.dp

class GameScreenshotAdapter(val context: Context) :
    RecyclerView.Adapter<GameScreenshotAdapter.ViewHolder>() {
    private lateinit var bean: GameInfoBean.Game
    private var datas = ArrayList<GameInfoBean.Game.Screenshot>()

    fun addData(bean: GameInfoBean.Game) {
        this.bean = bean
        datas.clear()
        datas.addAll(bean.screenshots)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, parent: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_game_screenshot, viewGroup, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(bean, position)
        holder.itemView.setOnClickListener {
            onItemClick.invoke(it, datas[position])
        }
    }

    override fun getItemCount(): Int {
        return datas.size
    }


    lateinit var onItemClick: (v: View, data: GameInfoBean.Game.Screenshot) -> Unit

    fun setOnItemClickListener(onItemClick: (v: View, data: GameInfoBean.Game.Screenshot) -> Unit) {
        this.onItemClick = onItemClick
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var mImageView: ImageView

        init {
            mImageView = view.findViewById(R.id.game_screenshot)
        }

        fun setData(bean: GameInfoBean.Game, position: Int) {
            val item = bean.screenshots[position]
            GlideApp.with(itemView)
                .load(GlideUrlNewKey(item.fileServer, item.path))
                .centerCrop()
                .placeholder(R.drawable.placeholder_transparent)
                .dontTransform()
                .into(object : CustomTarget<Drawable>(1, 240.dp) {
                    override fun onLoadStarted(placeholder: Drawable?) {
                        super.onLoadStarted(placeholder)
                        mImageView.setImageDrawable(placeholder)
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        mImageView.setImageDrawable(null)
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        mImageView.setImageDrawable(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        mImageView.setImageDrawable(placeholder)
                    }

                })
        }

    }

}