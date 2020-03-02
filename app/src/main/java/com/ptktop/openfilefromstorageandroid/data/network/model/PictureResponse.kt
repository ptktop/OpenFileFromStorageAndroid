package com.ptktop.openfilefromstorageandroid.data.network.model

import com.google.gson.annotations.SerializedName

class PictureResponse {
    @SerializedName("id")
    var id: String? = null
    @SerializedName("author")
    var author: String? = null
    @SerializedName("download_url")
    var downloadUrl: String? = null
}