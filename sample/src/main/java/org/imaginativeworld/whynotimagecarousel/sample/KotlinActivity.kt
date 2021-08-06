package org.imaginativeworld.whynotimagecarousel.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.imaginativeworld.whynotimagecarousel.listener.CarouselListener
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import org.imaginativeworld.whynotimagecarousel.sample.databinding.ActivityKotlinBinding

class KotlinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKotlinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKotlinBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val data = mutableListOf(
            CarouselItem("https://minio.o.kg/media-service/ApplicationBanners/ru/cashing_money_1040x334_app_ru-copy.jpg"),
            CarouselItem("https://minio.o.kg/media-service/ApplicationBanners/ru/pno-otv-1040х334-ru.jpg"),
        )
        binding.carousel1.carouselListener = object : CarouselListener {
            override fun onClick(position: Int, carouselItem: CarouselItem) {
                super.onClick(position, carouselItem)
                Toast.makeText(applicationContext, "$position", Toast.LENGTH_SHORT).show()
            }
        }

        binding.carousel1.setData(data)
    }

}
