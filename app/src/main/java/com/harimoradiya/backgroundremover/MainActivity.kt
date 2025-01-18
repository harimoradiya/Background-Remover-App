package com.harimoradiya.backgroundremover
// ApiService.kt

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part



import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.harimoradiya.backgroundremover.api.ApiService
import com.harimoradiya.backgroundremover.databinding.ActivityMainBinding
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Permission needed to access gallery", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            binding.imagePreview.setImageURI(selectedImageUri)
            binding.processButton.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
    }

    private fun setupUI() {
        binding.selectImageButton.setOnClickListener {
            checkGalleryPermission()
        }

        binding.processButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                processImage(uri)
            }
        }

        // Initialize progress bar
        binding.horizontalProgressBar.max = 100
        binding.horizontalProgressBar.progress = 0
    }

    private fun checkGalleryPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    openGallery()
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
            else -> {
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        openGallery()
                    }
                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun processImage(uri: Uri) {
        binding.horizontalProgressBar.visibility = View.VISIBLE
        binding.circularProgressBar.visibility = View.VISIBLE
        binding.processButton.isEnabled = false
        binding.horizontalProgressBar.progress = 0

        try {
            // Create API client with progress interceptor
            val progressInterceptor = ProgressInterceptor { progress ->
                runOnUiThread {
                    binding.horizontalProgressBar.progress = progress
                }
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(progressInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .client(client)
                .build()

            val service = retrofit.create(ApiService::class.java)

            // Convert Uri to File
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "image.jpg")
            FileOutputStream(file).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }

            // Create multipart request
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            // Make API call
            service.removeBackground(body).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { responseBody ->
                            try {
                                // Save the processed image
                                val processedFile = File(cacheDir, "processed_image.png")
                                FileOutputStream(processedFile).use { outputStream ->
                                    responseBody.byteStream().copyTo(outputStream)
                                }

                                // Display the processed image
                                binding.imagePreview.setImageURI(Uri.fromFile(processedFile))

                                // Save to gallery
                                saveImageToGallery(processedFile)

                                Toast.makeText(
                                    this@MainActivity,
                                    "Image processed and saved successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error saving processed image: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Server error: ${response.code()}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    binding.horizontalProgressBar.visibility = View.GONE
                    binding.circularProgressBar.visibility = View.GONE
                    binding.processButton.isEnabled = true
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(
                        this@MainActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.horizontalProgressBar.visibility = View.GONE
                    binding.circularProgressBar.visibility = View.GONE
                    binding.processButton.isEnabled = true
                }
            })

        } catch (e: Exception) {
            Toast.makeText(
                this@MainActivity,
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            binding.horizontalProgressBar.visibility = View.GONE
            binding.circularProgressBar.visibility = View.GONE
            binding.processButton.isEnabled = true
        }
    }

    private fun saveImageToGallery(file: File) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "BG_Removed_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        try {
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving to gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

// Custom progress interceptor for tracking upload progress
class ProgressInterceptor(
    private val onProgressUpdate: (Int) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()

        if (originalRequest.body == null) {
            return chain.proceed(originalRequest)
        }

        val progressRequest = originalRequest.newBuilder().build()

        val requestBody = originalRequest.body!!
        val contentLength = requestBody.contentLength()

        val progressBody = object : RequestBody() {
            override fun contentType() = requestBody.contentType()

            override fun contentLength() = requestBody.contentLength()

            override fun writeTo(sink: BufferedSink) {
                val progressSink = object : ForwardingSink(sink) {
                    private var bytesWritten = 0L

                    override fun write(source: Buffer, byteCount: Long) {
                        super.write(source, byteCount)
                        bytesWritten += byteCount
                        val progress = ((bytesWritten.toFloat() / contentLength.toFloat()) * 100).toInt()
                        onProgressUpdate(progress.coerceIn(0, 100))
                    }
                }

                val bufferedSink = progressSink.buffer()
                requestBody.writeTo(bufferedSink)
                bufferedSink.flush()
            }
        }

        val progressRequest2 = progressRequest.newBuilder().method(
            originalRequest.method,
            progressBody
        ).build()

        return chain.proceed(progressRequest2)
    }
}
