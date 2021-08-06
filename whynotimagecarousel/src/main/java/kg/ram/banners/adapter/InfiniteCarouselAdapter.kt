package kg.ram.banners.adapter

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kg.ram.banners.model.CarouselGravity
import kg.ram.banners.model.CarouselItem
import kg.ram.banners.model.CarouselType

class InfiniteCarouselAdapter(
    recyclerView: RecyclerView,
    carouselType: CarouselType,
    carouselGravity: CarouselGravity,
    autoWidthFixing: Boolean,
    imageScaleType: ImageView.ScaleType,
    imagePlaceholder: Drawable?,
    cardCornerRadius: Int
) : FiniteCarouselAdapter(
    recyclerView,
    carouselType,
    carouselGravity,
    autoWidthFixing,
    imageScaleType,
    imagePlaceholder,
    cardCornerRadius
) {
    override fun getItemCount(): Int {
        return if (dataList.isEmpty()) 0 else Integer.MAX_VALUE
    }

    override fun getItem(position: Int): CarouselItem? {
        return if (position < itemCount) {
            dataList[position % dataList.size]
        } else {
            null
        }
    }

    override fun getRealDataPosition(position: Int): Int {
        return position % dataList.size
    }
}
