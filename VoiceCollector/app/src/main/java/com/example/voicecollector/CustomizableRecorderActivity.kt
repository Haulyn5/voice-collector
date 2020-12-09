/*
* Author: Haulyn5
* Reference: https://developer.android.com/guide/topics/media/mediarecorder
* */

package com.example.voicecollector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import java.io.File


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
        val backRecordButton:Button = findViewById(R.id.backRecordButton)
        val nextRecordButton:Button = findViewById(R.id.nextRecordButton)
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


        backRecordButton.setOnClickListener {
            updateFileName()
            if(!inputComplete){
                return@setOnClickListener
            }
            var data_id = dataIdInput.text.toString().toInt()
            if(data_id > 1 ) {
                data_id -= 1  // 不做借位运算了，单纯减一
            }
            dataIdInput.setText(data_id.toString())
        }

        nextRecordButton.setOnClickListener {
            updateFileName()
            if (!inputComplete) {
                return@setOnClickListener
            }
            // 进位操作参考 stopRecording
            // 先对 Data Id 递增
            val currentDataId: Int = dataIdInput.text.toString().toInt() + 1
            // 不进位了
            dataIdInput.setText(currentDataId.toString())
        }

    }

    override fun onStop() {
        super.onStop()
        recorder?.release()
        recorder = null
        player?.release()
        player = null
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
        // 为了加速录制，在录制完一个音频后，自动更改 Data ID，目前录制计划为
        // 在真实数据集上：每个用户选取 50 条命令（参考 CaField，命令选自 ok-google.io），在 4 个不同距离
        // 进行录制。分别是：25cm（面前，正常使用手机的距离）收集 20 条，50cm 收集 10条， 100 cm 收集 10
        // 条，3m 收集 10条，在重放数据集上，每条真实语音都在 25cm ， 100cm 和 3m 距离进行重放。这样每个
        // 用户都会生成 200 条音频数据。将用户在 25 cm距离的真实语音中选取 10条用于录入，剩余用于测试。
        val spoofSwitch:Switch = findViewById(R.id.spoofSwitch)
        val dataIdInput: EditText = findViewById(R.id.dataIdInput)
        val userId: Int = findViewById<EditText>(R.id.userIdInput).text.toString().toInt()
        val recordDistanceSpinner: Spinner = findViewById(R.id.recordDistanceSpinner)
        // 先对 Data Id 递增
        var currentDataId:Int = dataIdInput.text.toString().toInt() + 1

        // 再 “进位”
        // 101 以后的userId 不进位
        if (userId < 101){
            if(spoofSwitch.isChecked){
                // spoof
                if(currentDataId > 50){
                    when(recordDistanceSpinner.selectedItemId) {
                        0.toLong() -> {
                            currentDataId = 1
                            recordDistanceSpinner.setSelection(2)  // 将 Spinner 设置选中第三项
                        }
                        2.toLong() -> {
                            currentDataId = 1
                            recordDistanceSpinner.setSelection(3)
                        }
                        3.toLong() -> {
                            currentDataId = 1
                            recordDistanceSpinner.setSelection(0)
                            spoofSwitch.isChecked = false  // 切换至录制 genuine
                        }
                    }

                }
            }
            else{
                // genuine
                when(recordDistanceSpinner.selectedItemId) {
                    0.toLong() -> {
                        if (currentDataId > 20) {
                            currentDataId = 1
                            recordDistanceSpinner.setSelection(1)
                        }
                    }
                    1.toLong() -> {
                        if (currentDataId > 10) {
                            currentDataId = 1
                            recordDistanceSpinner.setSelection(2)
                        }
                    }
                    2.toLong() -> {
                        if (currentDataId > 10) {
                            currentDataId = 1
                            recordDistanceSpinner.setSelection(3)
                        }
                    }
                    3.toLong() -> {
                        if (currentDataId > 10) {
                            currentDataId = 1
                            recordDistanceSpinner.setSelection(0)
                            spoofSwitch.isChecked = true  // 切换至录制 spoof
                        }
                    }
                }
            }
        }
        dataIdInput.setText(currentDataId.toString())
    }

}