package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.minhaprimeiraapi.service.RetrofitClient
import com.example.minhaprimeiraapi.service.RetrofitClient.safeApiCall
import com.example.myapitest.databinding.ActivityCarDetailsBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.CarDetails
import com.example.myapitest.model.Place
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom

class CarDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCarDetailsBinding
    private lateinit var car: Car


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
                            imageUrl = " ",
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
        //binding.image.loadUrl)
    }

    companion object {
        private const val ID = "ID"
        fun newIntent(
            context: Context,
            itemId: String
        ) = Intent(context, CarDetailsActivity::class.java).apply {
            putExtra(ID, itemId)
        }
    }
}