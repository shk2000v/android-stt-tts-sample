package com.example.stttts
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import java.util.*
import com.example.stttts.R
import com.example.stttts.data.api.AnthropicService
import com.example.stttts.data.model.Message
import com.example.stttts.data.model.MessageRequest
import com.example.stttts.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var resultTextView: TextView
    private lateinit var sttButton: Button
    private lateinit var ttsButton: Button
    private lateinit var anthropicService: AnthropicService

    private var isListening = false
    private var isSpeaking = false

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 서비스 초기화
        anthropicService = NetworkModule.createAnthropicService(this)

        // 뷰 초기화
        resultTextView = findViewById(R.id.resultTextView)
        sttButton = findViewById(R.id.sttButton)
        ttsButton = findViewById(R.id.ttsButton)

        // 권한 확인
        checkPermission()

        // TTS 초기화
        textToSpeech = TextToSpeech(this, this)

        // 버튼 설정
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            initSTT()
            setupButtons()
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun initSTT() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                sttButton.text = "말씀해주세요..."
                isListening = true
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    resultTextView.text = "인식된 텍스트: $text\n\n응답 대기 중..."

                    lifecycleScope.launch {
                        sendToAnthropic(text)
                    }
                }
                sttButton.text = "음성 인식 시작"
                isListening = false
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "오디오 에러"
                    SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 없음"
                    SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃"
                    SpeechRecognizer.ERROR_NO_MATCH -> "음성 인식 실패"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "음성 인식기 사용 중"
                    SpeechRecognizer.ERROR_SERVER -> "서버 에러"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "음성 입력 없음"
                    else -> "알 수 없는 에러"
                }
                resultTextView.text = "에러: $errorMessage"
                sttButton.text = "음성 인식 시작"
                isListening = false
            }

            // 필수 구현 메서드들
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })
    }

    private fun setupButtons() {
        sttButton.setOnClickListener {
            if (!isListening) {
                startSTT()
            } else {
                stopSTT()
            }
        }

        ttsButton.setOnClickListener {
            if (!isSpeaking) {
                val text = resultTextView.text.toString()
                if (text.isNotEmpty()) {
                    startTTS(text)
                }
            } else {
                stopTTS()
            }
        }
    }

    private fun startSTT() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        }
        speechRecognizer.startListening(intent)
    }

    private fun stopSTT() {
        speechRecognizer.stopListening()
        isListening = false
        sttButton.text = "음성 인식 시작"
    }

    private fun startTTS(text: String) {
        isSpeaking = true
        ttsButton.text = "읽기 중지"
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun stopTTS() {
        isSpeaking = false
        ttsButton.text = "텍스트 읽기"
        textToSpeech.stop()
    }

    private suspend fun sendToAnthropic(userInput: String) {
        try {
            val request = MessageRequest(
                messages = listOf(
                    Message(
                        role = "user",
                        content = userInput
                    )
                )
            )

            val response = anthropicService.sendMessage(requestBody = request)

            withContext(Dispatchers.Main) {
                response.content.firstOrNull()?.text?.let { responseText ->
                    resultTextView.text = "인식된 텍스트: $userInput\n\n응답: $responseText"
                    startTTS(responseText)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                e.printStackTrace()
                resultTextView.text = "오류가 발생했습니다: ${e.message}"
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.KOREAN)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                resultTextView.text = "TTS가 한국어를 지원하지 않습니다."
            }
        } else {
            resultTextView.text = "TTS 초기화 실패"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    initSTT()
                    setupButtons()
                }
                return
            }
        }
    }
}