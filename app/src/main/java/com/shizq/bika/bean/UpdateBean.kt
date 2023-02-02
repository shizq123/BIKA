package com.shizq.bika.bean


data class UpdateBean(
    val android_min_api_level: String,
    val app_display_name: String,
    val app_icon_url: String,
    val app_name: String,
    val app_os: String,
    val bundle_identifier: String,
    val can_resign: Any,
    val destination_type: String,
    val device_family: Any,
    val distribution_group_id: String,
    val distribution_groups: List<DistributionGroup>,
    val download_url: String,//下载地址
    val enabled: Boolean,
    val fileExtension: String,
    val fingerprint: String,
    val id: Int,
    val install_url: String,
    val is_external_build: Boolean,
    val is_latest: Boolean,
    val is_udid_provisioned: Any,
    val mandatory_update: Boolean,
    val min_os: String,
    val origin: String,
    val owner: Owner,
    val package_hashes: List<String>,
    val release_notes: String,//描述
    val short_version: String,
    val size: Int,
    val status: String,
    val uploaded_at: String,
    val version: String//版本
){
    data class DistributionGroup(
        val display_name: String,
        val id: String,
        val is_public: Boolean,
        val name: String,
        val origin: String
    )

    data class Owner(
        val display_name: String,
        val name: String
    )
}