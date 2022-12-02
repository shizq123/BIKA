package com.shizq.bika.bean

data class KnightBean(
    val users: List<Users>
) {
    data class Users(
        /**
         * _id : 593019d53f532059f297efa7
         * gender : m
         * name : 黎欧
         * slogan : emmm。。。二八七六八七八三九二（QQ代传邮箱，请标注来意不然我只能无视了）
         * title : 萌新
         * verified : false
         * exp : 1742339
         * level : 132
         * characters : ["knight"]
         * role : knight
         * avatar : {"fileServer":"https://storage1.picacomic.com","path":"6b6b7a4a-4485-4e6b-aca8-2796ad3d06dd.jpg","originalName":"avatar.jpg"}
         * comicsUploaded : 12051
         * character : https://pica-web.wakamoment.tk/special/frame-456.png
         */
        val _id: String,
        val gender: String,
        val name: String,
        val slogan: String,
        val title: String,
        val isVerified: Boolean,
        val exp: Int,
        val level: Int,
        val role: String,
        val avatar: Avatar,
        val comicsUploaded: Int,
        val character: String,
        val characters: List<String>
    ) {
        data class Avatar(
            /**
             * fileServer : https://storage1.picacomic.com
             * path : 6b6b7a4a-4485-4e6b-aca8-2796ad3d06dd.jpg
             * originalName : avatar.jpg
             */
            val fileServer: String,
            val path: String,
            val originalName: String
        )
    }

}
