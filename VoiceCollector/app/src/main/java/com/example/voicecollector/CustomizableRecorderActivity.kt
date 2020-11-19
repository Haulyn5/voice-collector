/*
* Author: Haulyn5
* Reference: https://developer.android.com/guide/topics/media/mediarecorder
* */

package com.example.voicecollector

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.widget.addTextChangedListener

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class CustomizableRecorderActivity : AppCompatActivity() {
    var isRecording: Boolean = false
    var isPlaying: Boolean = false
    var inputComplete: Boolean = false

    private var fileName: String = ""

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customizable_recorder)
        val startRecordingButton: Button = findViewById(R.id.startRecordingButton)
        val startPlayingButton: Button = findViewById(R.id.startPlayingButton)
        val userIdInput: EditText = findViewById(R.id.userIdInput)
        val dataIdInput: EditText = findViewById(R.id.dataIdInput)
        val spoofSwitch: Switch = findViewById(R.id.spoofSwitch)
        val recordDistanceSpinner: Spinner = findViewById(R.id.recordDistanceSpinner)

        // 请求录音权限
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        // 设定监听使得任何修改都会使得下方的输出随之修改
        userIdInput.addTextChangedListener{
            updateFileName()
        }
        dataIdInput.addTextChangedListener{
            updateFileName()
        }
        spoofSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            updateFileName()
        }
        // spinner 的选中监听，实在不能简约一点
        recordDistanceSpinner.onItemSelectedListener = object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateFileName()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateFileName()
            }
        }

        userIdInput.keyListener = DigitsKeyListener.getInstance("1234567890") // 确保 id 只能输入正整数
        dataIdInput.keyListener = DigitsKeyListener.getInstance("1234567890")
        // 设定两个按钮的点击监听
        startRecordingButton.setOnClickListener {
            updateFileName()
            if(!inputComplete){
                return@setOnClickListener  // 及时 return 避免崩溃
            }
            if (isRecording) {
                // stop
                startRecordingButton.text = "Start Recording"
                stopRecording()
            } else {
                startRecordingButton.text = "Stop Recording"
                startRecording()
            }
            isRecording = !isRecording
        }

        startPlayingButton.setOnClickListener {
            updateFileName()
            if(!inputComplete){
                return@setOnClickListener
            }
            if (isPlaying) {
                // stop
                startPlayingButton.text = "Start Playing"
                stopPlaying()
            } else {
                startPlayingButton.text = "Stop Playing"
                startPlaying()
            }
            isPlaying = !isPlaying
        }


    }

    fun inputValidator(): Boolean {
        val userIdInput: EditText = findViewById(R.id.userIdInput)
        val dataIdInput: EditText = findViewById(R.id.dataIdInput)
        if (userIdInput.text.toString() == "" || userIdInput.text.toString().toInt() <= 0)
            return false
        if (dataIdInput.text.toString() == "" || dataIdInput.text.toString().toInt() <= 0)
            return false
        return true
    }

    fun updateFileName(){
        // println(inputValidator())
        if(!inputValidator()){
            inputComplete = false
            Toast.makeText(applicationContext, "请检查您的输入是否正确", Toast.LENGTH_LONG).show()
            return
        }
        inputComplete = true
        var tmpFileName = getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString() + "/";
        val spoofSwitch:Switch = findViewById(R.id.spoofSwitch)
        val userIdInput: EditText = findViewById(R.id.userIdInput)
        val dataIdInput: EditText = findViewById(R.id.dataIdInput)
        val recordDistanceSpinner: Spinner = findViewById(R.id.recordDistanceSpinner)
        val expectFileNameTextView: TextView = findViewById(R.id.expectFileNameTextView)
        if(spoofSwitch.isChecked){
            tmpFileName += "spoof-u"
        }
        else{
            tmpFileName += "genuine-u"
        }
        tmpFileName += userIdInput.text.toString().toInt().toString()
        tmpFileName += "-"
        tmpFileName += recordDistanceSpinner.getSelectedItem().toString().dropLast(2)  // 去掉距离的单位厘米
        tmpFileName += "-d"
        tmpFileName += dataIdInput.text.toString().toInt().toString()
        tmpFileName += ".wav" // 文件格式

        this.fileName = tmpFileName
        expectFileNameTextView.text = fileName
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun startPlaying() {
        player = MediaPlayer().apply {
            setDataSource(fileName)
            prepare()
            start()
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun startRecording() {
        recorder = MediaRecorder().apply {
            // 录制格式、编码
            // 用虚拟机似乎会崩溃，使用真机测试
            // reference: https://stackoverflow.com/questions/57486603/how-to-configure-mediarecorder-to-record-audio-in-wav-format-at-48khz-in-android
            setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
            setOutputFormat(AudioFormat.ENCODING_PCM_16BIT)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(2)
            setAudioEncodingBitRate(128000);
            setAudioSamplingRate(48000);
            setOutputFile(fileName)
            // setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)  // https://developer.android.com/guide/topics/media/media-formats
            prepare()
            start()
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            stop()
            reset()
            release()
        }
        recorder = null
    }
}