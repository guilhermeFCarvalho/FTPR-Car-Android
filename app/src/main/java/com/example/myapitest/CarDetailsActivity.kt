package com.example.myapitest

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.transition.Visibility
import com.example.minhaprimeiraapi.service.RetrofitClient
import com.example.minhaprimeiraapi.service.RetrofitClient.safeApiCall
import com.example.myapitest.databinding.ActivityCarDetailsBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.CarDetails
import com.example.myapitest.model.Place
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class CarDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarDetailsBinding
    private lateinit var car: Car

    private lateinit var imageUri: Uri
    private var imageFile: File? = null

    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding.tvImageUrl.text = imageUri.path
            binding.btnClearImage.visibility = View.VISIBLE
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        setupView()
        loadCar()
    }


    private fun setupView() {
        binding.btnDelete.setOnClickListener {
            deleteCar()
        }
        binding.btnSave.setOnClickListener {
            saveCar()
        }
        binding.btnCamera.setOnClickListener {
            takePicture()
        }
        binding.btnClearImage.setOnClickListener {
            binding.tvImageUrl.text = ""
            binding.btnClearImage.visibility = View.INVISIBLE
        }
        if (!intent.getStringExtra(ID).isNullOrBlank()) {
            binding.btnDelete.visibility = View.VISIBLE
        }

    }

    private fun loadCar() {
        val itemId = intent.getStringExtra(ID)

        itemId?.let {
            CoroutineScope(Dispatchers.IO).launch {
                val result = safeApiCall { RetrofitClient.apiService.getCar(itemId) }

                withContext(Dispatchers.Main) {
                    when (result) {
                        is RetrofitClient.Result.Error -> {}
                        is RetrofitClient.Result.Success<*> -> {
                            car = result.data as Car
                            handleSuccess()
                        }
                    }
                }
            }

        }


    }

    private fun saveCar() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                if (!intent.getStringExtra(ID).isNullOrBlank()) {
                    RetrofitClient.apiService.updateCar(
                        car.id,
                        car.value.copy(
                            name = binding.etModel.text.toString(),
                            licence = binding.etLicence.text.toString(),
                            year = binding.etYear.text.toString(),
                            imageUrl = binding.tvImageUrl.text.toString(),
                        )
                    )

                } else {
                    val id = SecureRandom().nextInt().toString()
                    RetrofitClient.apiService.addCar(
                        CarDetails(
                            id = id,
                            name = binding.etModel.text.toString(),
                            year = binding.etYear.text.toString(),
                            licence = binding.etLicence.text.toString(),
                            imageUrl = binding.tvImageUrl.text.toString(),
                            place = Place(0.0, 0.0)

                        )
                    )

                }


            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is RetrofitClient.Result.Error -> {
                        Toast.makeText(
                            this@CarDetailsActivity,
                            "erro",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is RetrofitClient.Result.Success<*> -> {
                        Toast.makeText(
                            this@CarDetailsActivity,
                            "sucesso",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun deleteCar() {
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteCar(car.id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is RetrofitClient.Result.Error -> {
                        Toast.makeText(
                            this@CarDetailsActivity,
                            "erro",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    is RetrofitClient.Result.Success -> {
                        Toast.makeText(
                            this@CarDetailsActivity,
                            "sucesso",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            }
        }
    }


    private fun handleSuccess() {
        binding.etModel.setText(car.value.name)
        binding.etYear.setText(car.value.year)
        binding.etLicence.setText(car.value.licence)
        binding.tvImageUrl.text = car.value.imageUrl
        if (binding.tvImageUrl.text.isNotBlank()) {
            binding.btnClearImage.visibility = View.VISIBLE
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        val uri = FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
        return uri
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(
                    this@CarDetailsActivity,
                    "Permissão de camera negada",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //o upload para o firebase esta desativado devido à cobrança do plano
    private fun uploadImageToFirebase() {
        val storageRef = FirebaseStorage.getInstance().reference

        val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

        val baos = ByteArrayOutputStream()
        val imageBitmap = BitmapFactory.decodeFile(imageFile!!.path)
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        binding.progressLoadImage.visibility = View.VISIBLE
        binding.btnCamera.isEnabled = false
        binding.btnSave.isEnabled = false

        imagesRef.putBytes(data)
            .addOnFailureListener {
                binding.progressLoadImage.visibility = View.GONE
                binding.btnCamera.isEnabled = true
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Upload Error", Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {
                binding.progressLoadImage.visibility = View.GONE
                binding.btnCamera.isEnabled = true
                binding.btnSave.isEnabled = true
                imagesRef.downloadUrl.addOnSuccessListener { uri ->
                    binding.tvImageUrl.text = uri.path
                }
            }
    }


    companion object {
        private const val ID = "ID"
        private const val CAMERA_REQUEST_CODE = 101
        fun newIntent(
            context: Context,
            itemId: String
        ) = Intent(context, CarDetailsActivity::class.java).apply {
            putExtra(ID, itemId)
        }
    }
}