package com.ptktop.openfilefromstorageandroid.data.network.api

import com.ptktop.openfilefromstorageandroid.data.network.model.PictureResponse
import io.reactivex.Observable

import retrofit2.http.GET
import retrofit2.http.Query
import java.util.ArrayList

interface PictureApi {

    @GET("/v2/list")
    fun photoList(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Observable<ArrayList<PictureResponse>>

}