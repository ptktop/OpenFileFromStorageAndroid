package com.ptktop.openfilefromstorageandroid.manager

import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class PermissionValidate(private val listener : EventListener) : PermissionListener {

    interface EventListener {
        @Throws(InterruptedException::class)
        fun showPermissionGranted(permission: String?)

        fun showPermissionDenied(
            permission: String?,
            isPermanentlyDenied: Boolean
        )

        fun showPermissionRationale(token: PermissionToken?)
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse) {
        try {
            listener.showPermissionGranted(response.permissionName)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse) {
        listener.showPermissionDenied(response.permissionName, response.isPermanentlyDenied)
    }

    override fun onPermissionRationaleShouldBeShown(
        permission: PermissionRequest?, token: PermissionToken?
    ) {
        listener.showPermissionRationale(token)
    }
}