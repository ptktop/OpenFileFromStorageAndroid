package com.ptktop.openfilefromstorageandroid.module.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.Glide
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.PermissionListener
import com.ptktop.openfilefromstorageandroid.R
import com.ptktop.openfilefromstorageandroid.data.network.ServiceCreateCall
import com.ptktop.openfilefromstorageandroid.data.network.model.PictureResponse
import com.ptktop.openfilefromstorageandroid.manager.DownloadFileAll
import com.ptktop.openfilefromstorageandroid.manager.PermissionValidate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import kotlin.math.pow
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), View.OnClickListener, PermissionValidate.EventListener
    , DownloadFileAll.DownloadListener {

    private lateinit var progressLoading: ProgressBar
    private lateinit var linearDownload: LinearLayoutCompat
    private lateinit var tvPercent: AppCompatTextView
    private lateinit var tvSizeData: AppCompatTextView
    private lateinit var tvTotalData: AppCompatTextView
    private lateinit var tvAuthor: AppCompatTextView
    private lateinit var progressDownload: ContentLoadingProgressBar
    private lateinit var imgView: AppCompatImageView
    private lateinit var btnPrevious: AppCompatButton
    private lateinit var btnNext: AppCompatButton

    private var onLoading = MutableLiveData<Boolean>()
    private var picResponse = MutableLiveData<ArrayList<PictureResponse>>()
    private var picCloneResponse = ArrayList<PictureResponse>()

    private var subscription: Disposable? = null

    private var permissionStorage: PermissionListener? = null
    private val requestSetting: Int = 1
    private var positionPic: Int = 0
    private val folderDownload: String = "/OpenFileFromStorageAndroid/Downloads/"
    private var strAuthor: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        createPermissionWriteStorage()
    }

    private fun initView() {
        progressLoading = findViewById(R.id.progressLoading)
        linearDownload = findViewById(R.id.linearDownload)
        tvPercent = findViewById(R.id.tvPercent)
        tvSizeData = findViewById(R.id.tvSizeData)
        tvTotalData = findViewById(R.id.tvTotalData)
        tvAuthor = findViewById(R.id.tvAuthor)
        progressDownload = findViewById(R.id.progressDownload)
        imgView = findViewById(R.id.imgView)
        btnPrevious = findViewById(R.id.btnPrevious)
        btnNext = findViewById(R.id.btnNext)
        setupView()
        setupObserve()
        callApi()
    }

    private fun setupView() {
        btnPrevious.setOnClickListener(this)
        btnNext.setOnClickListener(this)
    }

    private fun setupObserve() {
        onLoading.observe(this, androidx.lifecycle.Observer { isLoad -> loading(isLoad) })
        picResponse.observe(
            this,
            androidx.lifecycle.Observer { listData -> (validateDirectoryHaveData(listData)) })
    }

    @SuppressLint("SetTextI18n")
    private fun validateDirectoryHaveData(list: ArrayList<PictureResponse>) {
        picCloneResponse.clear()
        picCloneResponse.addAll(list)
        val listData = ArrayList<PictureResponse>(list)
        var fullHaveData = true
        var firstNotFound = true
        val pathFolder = Environment.getExternalStorageDirectory().toString() + folderDownload
//        val pathFolder = applicationContext.filesDir.absolutePath.toString() + folderDownload
        for (i in 0 until listData.size) {
            val dao = listData[i]
            val pathFile = File(pathFolder + dao.id + "_" + dao.author + ".jpg")
            if (!pathFile.exists()) { // Check File & No File
                if (firstNotFound) {
                    picResponse.value!!.clear()
                    firstNotFound = false
                }
                picResponse.value!!.add(listData[i])
                fullHaveData = false
            }
        }

        if (fullHaveData) {
            tvSizeData.text = "Have data not download again."
            setUpImage()
        } else {
            tvSizeData.text = "Download Author => 0 kb downloading"
            checkPermissionStorage() // download storage file
        }
    }

    private fun setUpImage() {
        val pathFolder = Environment.getExternalStorageDirectory().toString() + folderDownload
//        val pathFolder = applicationContext.filesDir.absolutePath.toString() + folderDownload
        val dao = picCloneResponse[positionPic]
        val pathFile = File(pathFolder + dao.id + "_" + dao.author + ".jpg")
        val imgUri = Uri.fromFile(pathFile)
        Glide.with(this)
            .load(imgUri)
            .into(imgView)
        tvAuthor.text = dao.author
    }

    /****************** Permission ******************/
    private fun createPermissionWriteStorage() {
        val permissionListener: PermissionListener = PermissionValidate(this)
        permissionStorage = CompositePermissionListener(permissionListener)
    }

    private fun checkPermissionStorage() {
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(permissionStorage)
            .check()
    }

    override fun showPermissionGranted(permission: String?) {
        if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (picResponse.value != null) {
                if (picResponse.value!!.size > 0) {
                    positionPic = 0
                    startDownloadFile()
                }
            } else {
                callApi()
            }
        }
    }

    override fun showPermissionDenied(permission: String?, isPermanentlyDenied: Boolean) {
        if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (isPermanentlyDenied) { // go to setting
                showDialogPermission()
            } else { // not check box ask me later
                showDialogPermission()
            }
        }
    }

    override fun showPermissionRationale(token: PermissionToken?) {
        token!!.continuePermissionRequest()
    }

    private fun showDialogPermission() {
        val builder =
            AlertDialog.Builder(this, R.style.Theme_AppCompat_Dialog_Alert)
                .setCancelable(false)
                .setIcon(ContextCompat.getDrawable(this, R.mipmap.ic_launcher))
                .setTitle("Write storage permission request")
                .setMessage("File will not be available until you accept the permission request.")
                .setPositiveButton(
                    "OK"
                ) { _: DialogInterface?, _: Int ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", packageName, null)
                    startActivityForResult(intent, requestSetting)
                }
        val dialog: AppCompatDialog = builder.create()
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestSetting) {
            if (resultCode == Activity.RESULT_CANCELED) {
                checkPermissionStorage()
            }
        }
    }

    /****************** Download ******************/
    @SuppressLint("SetTextI18n")
    private fun startDownloadFile() {
        checkFolderInsideStorage()
        val list = picResponse.value!!
        tvTotalData.text = "0/${list.size}"
        strAuthor = list[0].author!!
        for (i in 0 until list.size) {
            DownloadFileAll(
                this,
                this,
                list[i].downloadUrl!!,
                folderDownload,
                list[i].id + "_" + list[i].author!! + ".jpg"
            ).run()
        }
    }

    private fun checkFolderInsideStorage() {
        val folderApp = "/OpenFileFromStorageAndroid/"
        val pathApp = File(Environment.getExternalStorageDirectory().toString() + folderApp)
//        val pathApp = File(applicationContext.filesDir.absolutePath.toString() + folderApp)
        val pathDownload = File(Environment.getExternalStorageDirectory().toString() + folderDownload)
//        val pathDownload = File(applicationContext.filesDir.absolutePath.toString() + folderDownload)
        if (!pathApp.exists()) { // not found folder app
            pathApp.mkdir()
            if (!pathDownload.exists()) { // not found folder download
                pathDownload.mkdir()
            }
        } else { // have folder main
            if (!pathDownload.exists()) { // have folder app , but not found folder download
                pathDownload.mkdir()
            }
        }
    }

    //-------------------------------------- Call Api ----------------------------------------------
    private fun callApi() {
        subscription = ServiceCreateCall.getInstance().getPictureApiService().photoList(1, 3)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                loading(true)
            }
            .doAfterTerminate {
                loading(false)
            }
            .subscribe(
                {
                    when (it.size) {
                        0 -> showToast("Not found list photo.")
                        else -> picResponse.value = it
                    }
                },
                { showToast(it.message!!) }
            )
    }

    override fun onClick(v: View?) {
        if (v == btnPrevious) {
            if (positionPic > 0) positionPic -= 1
        } else if (v == btnNext) {
            if (positionPic < (picCloneResponse.size - 1)) positionPic += 1
        }
        setUpImage()
    }

    private var startTime = System.currentTimeMillis()
    private var timeCount = 1

    @SuppressLint("SetTextI18n")
    override fun downloadProcess(bytesRead: Long, contentLength: Long, done: Boolean) {
        runOnUiThread {
            val totalFileSize = (contentLength / 1024.0.pow(1.0)).roundToInt()
            val current = (bytesRead / 1024.0.pow(1.0)).roundToInt()
            val progress = (bytesRead * 100 / contentLength).toInt()
            val currentTime: Long = System.currentTimeMillis() - startTime
            tvPercent.text = "$progress%"
            progressDownload.progress = progress
            tvSizeData.text =
                "Download Author $strAuthor => $current / $totalFileSize kb downloading"
            if (currentTime > 1000 * timeCount) {
                timeCount++
            }
            if (done) onDownloadComplete()
        }
    }

    private var currentPicDownload: Int = 0
    @SuppressLint("SetTextI18n")
    override fun downloadFileComplete(complete: Boolean, message: String?) {
        if (complete) {
            currentPicDownload += 1
            val list = picResponse.value!!
            if (currentPicDownload < list.size) strAuthor = list[currentPicDownload].author!!
            tvTotalData.text = "$currentPicDownload/${list.size}"
            if (currentPicDownload == list.size) setUpImage()
        } else {
            showDialogAlert(message)
        }
    }

    override fun storageFileComplete(complete: Boolean, message: String?) {
        if (complete) {
            showToast(message)
        } else {
            showDialogAlert(message)
        }
    }

    private fun showDialogAlert(message: String?) {
        val builder =
            AlertDialog.Builder(
                this,
                R.style.Theme_AppCompat_Dialog_Alert
            )
                .setCancelable(false)
                .setIcon(ContextCompat.getDrawable(this, R.mipmap.ic_launcher))
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton(
                    "OK"
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        val dialog: AppCompatDialog = builder.create()
        dialog.show()
    }

    private fun onDownloadComplete() {
        // do something
    }

    private fun loading(isLoad: Boolean) {
        if (isLoad) {
            progressLoading.visibility = View.VISIBLE
        } else {
            progressLoading.visibility = View.GONE
        }
    }

    private fun showToast(str: String?) {
        Toast.makeText(this@MainActivity, str, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.dispose()
    }
}
