package kg.ram.banners

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.Dimension
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.*
import me.relex.circleindicator.CircleIndicator2
import org.imaginativeworld.banners.R
import kg.ram.banners.adapter.FiniteCarouselAdapter
import kg.ram.banners.adapter.InfiniteCarouselAdapter
import kg.ram.banners.listener.CarouselListener
import kg.ram.banners.listener.CarouselOnScrollListener
import kg.ram.banners.model.CarouselGravity
import kg.ram.banners.model.CarouselItem
import kg.ram.banners.model.CarouselType
import kg.ram.banners.utils.CarouselLinearLayoutManager
import kg.ram.banners.utils.LinearStartSnapHelper
import kg.ram.banners.utils.dpToPx
import kg.ram.banners.utils.getSnapPosition
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class ImageCarousel(
    @NotNull context: Context,
    @Nullable private var attributeSet: AttributeSet?
) : ConstraintLayout(context, attributeSet), LifecycleObserver {

    companion object {
        const val TAG = "ImageCarousel"
    }

    private var adapter: FiniteCarouselAdapter? = null

    private var cardCornerRadius: Int = 4

    private val scaleTypeArray = arrayOf(
        ImageView.ScaleType.MATRIX,
        ImageView.ScaleType.FIT_XY,
        ImageView.ScaleType.FIT_START,
        ImageView.ScaleType.FIT_CENTER,
        ImageView.ScaleType.FIT_END,
        ImageView.ScaleType.CENTER,
        ImageView.ScaleType.CENTER_CROP,
        ImageView.ScaleType.CENTER_INSIDE
    )

    private val carouselTypeArray = arrayOf(
        CarouselType.BLOCK,
        CarouselType.SHOWCASE
    )

    private val carouselGravityArray = arrayOf(
        CarouselGravity.START,
        CarouselGravity.CENTER
    )

    private lateinit var carouselView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewTopShadow: View
    private lateinit var viewBottomShadow: View
    private var snapHelper: SnapHelper? = null

    private var indicator: CircleIndicator2? = null

    private var isBuiltInIndicator = false
    private var autoPlayHandler: Handler = Handler(Looper.getMainLooper())
    private var data: List<CarouselItem>? = null
    private var dataSize = 0

    private var isCarouselCentered = false

    var carouselListener: CarouselListener? = null
        set(value) {
            field = value

            adapter?.listener = carouselListener
        }

    var onScrollListener: CarouselOnScrollListener? = null
        set(value) {
            field = value

            initOnScrollStateChange()
        }

    /**
     * Get or set current item position
     */
    var currentPosition = -1
        get() {
            return snapHelper?.getSnapPosition(recyclerView.layoutManager) ?: -1
        }
        set(value) {
            val position = when {
                value >= Int.MAX_VALUE -> {
                    -1
                }
                value < 0 -> {
                    -1
                }
                else -> {
                    value
                }
            }

            field = position

            if (position != -1) {
                recyclerView.smoothScrollToPosition(position)
            }
        }

    /**
     * ****************************************************************
     * Attributes
     * ****************************************************************
     */

    var showTopShadow = false
        set(value) {
            field = value

            viewTopShadow.visibility = if (showTopShadow) View.VISIBLE else View.GONE
        }

    var topShadowAlpha: Float = 0f
        set(value) {
            field = getValueFromZeroToOne(value)

            viewTopShadow.alpha = topShadowAlpha
        }

    @Dimension(unit = Dimension.PX)
    var topShadowHeight: Int = 0
        set(value) {
            field = value

            val topShadowParams = viewTopShadow.layoutParams as LayoutParams
            topShadowParams.height = topShadowHeight
            viewTopShadow.layoutParams = topShadowParams
        }

    var showBottomShadow = false
        set(value) {
            field = value

            viewBottomShadow.visibility = if (showBottomShadow) View.VISIBLE else View.GONE
        }

    var bottomShadowAlpha: Float = 0f
        set(value) {
            field = getValueFromZeroToOne(value)

            viewBottomShadow.alpha = bottomShadowAlpha
        }

    @Dimension(unit = Dimension.PX)
    var bottomShadowHeight: Int = 0
        set(value) {
            field = value

            val bottomShadowParams = viewBottomShadow.layoutParams as LayoutParams
            bottomShadowParams.height = bottomShadowHeight
            viewBottomShadow.layoutParams = bottomShadowParams
        }

    var showIndicator = false
        set(value) {
            field = value

            initIndicator()
        }

    @Dimension(unit = Dimension.PX)
    var indicatorMargin: Int = 0
        set(value) {
            field = value

            if (isBuiltInIndicator) {
                indicator?.apply {
                    val indicatorMarginParams = this.layoutParams as LayoutParams
                    indicatorMarginParams.setMargins(
                        0,
                        0,
                        0,
                        indicatorMargin
                    )
                    this.layoutParams = indicatorMarginParams
                }
            }
        }

    var imageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
        set(value) {
            field = value

            initAdapter()
        }

    var carouselBackground: Drawable? = null
        set(value) {
            field = value

            recyclerView.background = carouselBackground
        }

    var imagePlaceholder: Drawable? = null
        set(value) {
            field = value

            initAdapter()
        }

    @Dimension(unit = Dimension.PX)
    var carouselPadding: Int = 0
        set(value) {
            field = value

            carouselPaddingStart = value
            carouselPaddingTop = value
            carouselPaddingEnd = value
            carouselPaddingBottom = value
        }

    @Dimension(unit = Dimension.PX)
    var carouselPaddingStart: Int = 0
        set(value) {
            field = value

            updateRecyclerViewPadding()
        }

    @Dimension(unit = Dimension.PX)
    var carouselPaddingTop: Int = 0
        set(value) {
            field = value

            updateRecyclerViewPadding()
        }

    @Dimension(unit = Dimension.PX)
    var carouselPaddingEnd: Int = 0
        set(value) {
            field = value

            updateRecyclerViewPadding()
        }

    @Dimension(unit = Dimension.PX)
    var carouselPaddingBottom: Int = 0
        set(value) {
            field = value

            updateRecyclerViewPadding()
        }

    private fun updateRecyclerViewPadding() {
        recyclerView.setPaddingRelative(
            carouselPaddingStart,
            carouselPaddingTop,
            carouselPaddingEnd,
            carouselPaddingBottom
        )
    }

    var carouselType: CarouselType = CarouselType.BLOCK
        set(value) {
            field = value

            updateSnapHelper()
        }

    var carouselGravity: CarouselGravity = CarouselGravity.CENTER
        set(value) {
            field = value

            recyclerView.layoutManager?.apply {
                if (this is CarouselLinearLayoutManager) {
                    this.isOffsetStart = carouselGravity == CarouselGravity.START
                }
            }

            updateSnapHelper()
        }

    var scaleOnScroll: Boolean = false
        set(value) {
            field = value

            recyclerView.layoutManager?.apply {
                if (this is CarouselLinearLayoutManager) {
                    this.scaleOnScroll = this@ImageCarousel.scaleOnScroll
                }
            }
        }

    var scalingFactor: Float = 0f
        set(value) {
            field = getValueFromZeroToOne(value)

            recyclerView.layoutManager?.apply {
                if (this is CarouselLinearLayoutManager) {
                    this.scalingFactor = this@ImageCarousel.scalingFactor
                }
            }
        }

    /**
     * Auto width fixing.
     *
     * If the sum of consecutive two items of a [RecyclerView] is not greater than
     * the [ImageCarousel] view width, then the [scrollToPosition] method will not work
     * as expected. So we will check the width of the element and increase the minimum width
     * for fixing the problem if [autoWidthFixing] is true.
     */
    var autoWidthFixing: Boolean = false
        set(value) {
            field = value

            initAdapter()
        }

    var autoPlay: Boolean = false
        set(value) {
            field = value

            initAutoPlay()
        }

    var autoPlayDelay: Int = 0

    var infiniteCarousel: Boolean = false

    var touchToPause: Boolean = false

    @ColorRes
    var activeIndicatorColor: Int = R.color.white

    @ColorRes
    var inactiveIndicatorColor: Int = R.color.white

    var slowingScrollSpeed: Int = 1

    init {
        initViews()
        initAttributes()
        initLayoutManager()
        initAdapter()
        initListeners()
        initAutoPlay()
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (touchToPause) {
            when (event?.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resumeAutoPlay()
                }
                MotionEvent.ACTION_DOWN -> {
                    pauseAutoPlay()
                }
            }
        }

        return super.onInterceptTouchEvent(event)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun pauseAutoPlay() {
        if (autoPlay) {
            autoPlayHandler.removeCallbacksAndMessages(null)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun resumeAutoPlay() {
        if (autoPlay) {
            initAutoPlay()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResumeCheckForStartPositionForInfiniteCarousel() {
        if (infiniteCarousel && !isCarouselCentered && dataSize != 0) {
            initStartPositionForInfiniteCarousel()
        }
    }

    private fun initViews() {
        carouselView = LayoutInflater.from(context).inflate(R.layout.image_carousel, this)

        recyclerView = carouselView.findViewById(R.id.recyclerView)
        viewTopShadow = carouselView.findViewById(R.id.view_top_shadow)
        viewBottomShadow = carouselView.findViewById(R.id.view_bottom_shadow)
    }

    private fun initAttributes() {
        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.ImageCarousel,
            0,
            0
        ).apply {

            try {

                slowingScrollSpeed =
                    getInt(R.styleable.ImageCarousel_slowingScrollSpeed, 1)

                activeIndicatorColor =
                    getResourceId(R.styleable.ImageCarousel_indicatorColor, R.color.white)
                inactiveIndicatorColor =  activeIndicatorColor

                if(hasValue(R.styleable.ImageCarousel_indicatorInactiveColor)) {
                    inactiveIndicatorColor =
                        getResourceId(
                            R.styleable.ImageCarousel_indicatorInactiveColor,
                            R.color.white
                        )
                }

                cardCornerRadius =
                    getDimension(R.styleable.ImageCarousel_carouselCardCornerRadius, 4f).toInt()

                showTopShadow = getBoolean(
                    R.styleable.ImageCarousel_showTopShadow,
                    true
                )

                topShadowAlpha = getFloat(
                    R.styleable.ImageCarousel_topShadowAlpha,
                    0.6f
                )

                topShadowHeight = getDimension(
                    R.styleable.ImageCarousel_topShadowHeight,
                    32.dpToPx(context).toFloat()
                ).toInt()

                showBottomShadow = getBoolean(
                    R.styleable.ImageCarousel_showBottomShadow,
                    true
                )

                bottomShadowAlpha = getFloat(
                    R.styleable.ImageCarousel_bottomShadowAlpha,
                    0.6f
                )

                bottomShadowHeight = getDimension(
                    R.styleable.ImageCarousel_bottomShadowHeight,
                    64.dpToPx(context).toFloat()
                ).toInt()

                carouselType = carouselTypeArray[
                        getInteger(
                            R.styleable.ImageCarousel_carouselType,
                            CarouselType.BLOCK.ordinal
                        )
                ]

                carouselGravity = carouselGravityArray[
                        getInteger(
                            R.styleable.ImageCarousel_carouselGravity,
                            CarouselGravity.CENTER.ordinal
                        )
                ]

                showIndicator = getBoolean(
                    R.styleable.ImageCarousel_showIndicator,
                    true
                )

                indicatorMargin = getDimension(
                    R.styleable.ImageCarousel_indicatorMargin,
                    0F
                ).toInt()

                imageScaleType = scaleTypeArray[
                        getInteger(
                            R.styleable.ImageCarousel_imageScaleType,
                            ImageView.ScaleType.CENTER_CROP.ordinal
                        )
                ]

                carouselBackground = getDrawable(
                    R.styleable.ImageCarousel_carouselBackground
                ) ?: ColorDrawable(Color.parseColor("#00000000"))

                imagePlaceholder = getDrawable(R.styleable.ImageCarousel_imagePlaceholder)

                carouselPadding = getDimension(
                    R.styleable.ImageCarousel_carouselPadding,
                    0F
                ).toInt()

                carouselPaddingStart = getDimension(
                    R.styleable.ImageCarousel_carouselPaddingStart,
                    0F
                ).toInt()

                carouselPaddingTop = getDimension(
                    R.styleable.ImageCarousel_carouselPaddingTop,
                    0F
                ).toInt()

                carouselPaddingEnd = getDimension(
                    R.styleable.ImageCarousel_carouselPaddingEnd,
                    0F
                ).toInt()

                carouselPaddingBottom = getDimension(
                    R.styleable.ImageCarousel_carouselPaddingBottom,
                    0F
                ).toInt()

                scaleOnScroll = getBoolean(
                    R.styleable.ImageCarousel_scaleOnScroll,
                    false
                )

                scalingFactor = getFloat(
                    R.styleable.ImageCarousel_scalingFactor,
                    .15F
                )

                autoWidthFixing = getBoolean(
                    R.styleable.ImageCarousel_autoWidthFixing,
                    true
                )

                autoPlay = getBoolean(
                    R.styleable.ImageCarousel_autoPlay,
                    false
                )

                autoPlayDelay = getInt(
                    R.styleable.ImageCarousel_autoPlayDelay,
                    3000
                )

                infiniteCarousel = getBoolean(
                    R.styleable.ImageCarousel_infiniteCarousel,
                    true
                )

                touchToPause = getBoolean(
                    R.styleable.ImageCarousel_touchToPause,
                    false
                )
            } finally {
                recycle()
            }
        }
    }

    private fun initLayoutManager() {
        recyclerView.layoutManager =
            CarouselLinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false,
                slowingScrollSpeed
            )
                .apply {
                    scaleOnScroll = this@ImageCarousel.scaleOnScroll
                    scalingFactor = this@ImageCarousel.scalingFactor
                }
        recyclerView.setHasFixedSize(true)
    }

    private fun initAdapter() {
        if (infiniteCarousel) {
            adapter = InfiniteCarouselAdapter(
                recyclerView = recyclerView,
                carouselType = carouselType,
                carouselGravity = carouselGravity,
                autoWidthFixing = autoWidthFixing,
                imageScaleType = imageScaleType,
                imagePlaceholder = imagePlaceholder,
                cardCornerRadius = cardCornerRadius
            ).apply {
                listener = carouselListener
            }
        } else {
            adapter = FiniteCarouselAdapter(
                recyclerView = recyclerView,
                carouselType = carouselType,
                carouselGravity = carouselGravity,
                autoWidthFixing = autoWidthFixing,
                imageScaleType = imageScaleType,
                imagePlaceholder = imagePlaceholder,
                cardCornerRadius = cardCornerRadius
            ).apply {
                listener = carouselListener
            }
        }

        recyclerView.adapter = adapter

        data?.apply {
            adapter?.replaceData(this)

            recyclerView.scrollToPosition(this.size / 2)

            indicator?.apply {
                this.createIndicators(dataSize, 0)
            }
        }
    }

    private fun initListeners() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val position = snapHelper?.getSnapPosition(recyclerView.layoutManager)
                    ?: RecyclerView.NO_POSITION

                val currentRealPosition =
                    adapter?.getRealDataPosition(position) ?: RecyclerView.NO_POSITION

                var dataItem: CarouselItem? = null

                // Change Indicator position
                indicator?.apply {
                    animatePageSelected(currentRealPosition)
                }

                // Invoke the listener
                onScrollListener?.onScrolled(recyclerView, dx, dy, currentRealPosition, dataItem)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                // Invoke the listener
                onScrollListener?.apply {
                    val position = snapHelper?.getSnapPosition(recyclerView.layoutManager)
                        ?: RecyclerView.NO_POSITION

                    val currentRealPosition =
                        adapter?.getRealDataPosition(position) ?: RecyclerView.NO_POSITION

                    if (currentRealPosition >= 0) {
                        val carouselItem: CarouselItem? = adapter?.getItem(currentRealPosition)

                        onScrollStateChanged(
                            recyclerView,
                            newState,
                            currentRealPosition,
                            carouselItem
                        )
                    }
                }
            }
        })
    }

    /**
     * Initialize and start/stop auto play based on [autoPlay] value.
     */
    private fun initAutoPlay() {
        autoPlayHandler.removeCallbacksAndMessages(null)

        if (autoPlay) {
            autoPlayHandler.postDelayed(
                object : Runnable {
                    override fun run() {
                        if (currentPosition == Int.MAX_VALUE - 1) {
                            currentPosition = 0
                        } else {
                            currentPosition++
                        }

                        autoPlayHandler.postDelayed(this, autoPlayDelay.toLong())
                    }
                },
                autoPlayDelay.toLong()
            )
        }
    }

    private fun initIndicator() {
        // If no custom indicator added, then default indicator will be shown.
        if (indicator == null) {
            indicator = carouselView.findViewById(R.id.indicator)
            isBuiltInIndicator = true
        }

        indicator?.apply {
            tintIndicator(
                ContextCompat.getColor(context, activeIndicatorColor),
                ContextCompat.getColor(context, inactiveIndicatorColor)
            )

            if (isBuiltInIndicator) {
                // Indicator margin re-initialize
                val indicatorMarginParams = this.layoutParams as LayoutParams
                indicatorMarginParams.setMargins(
                    indicatorMargin,
                    indicatorMargin,
                    indicatorMargin,
                    indicatorMargin
                )
                this.layoutParams = indicatorMarginParams

                // Indicator visibility
                this.visibility = if (showIndicator) View.VISIBLE else View.GONE
            }

            this.createIndicators(dataSize, currentPosition)
        }
    }

    private fun getValueFromZeroToOne(value: Float): Float {
        return when {
            value < 0 -> {
                0f
            }
            value > 1 -> {
                1f
            }
            else -> {
                value
            }
        }
    }

    /**
     * This is called because when data added/updated the ScrollListener is not invoked as expected.
     */
    private fun initOnScrollStateChange() {
        data?.apply {
            if (isNotEmpty()) {
                onScrollListener?.onScrollStateChanged(
                    recyclerView,
                    RecyclerView.SCROLL_STATE_IDLE,
                    0,
                    this[0]
                )
            }
        }
    }

    private fun updateSnapHelper() {
        snapHelper?.attachToRecyclerView(null)

        snapHelper = if (carouselType == CarouselType.BLOCK) {
            PagerSnapHelper()
        } else { // CarouselType.SHOWCASE
            if (carouselGravity == CarouselGravity.START) {
                LinearStartSnapHelper()
            } else { // CarouselGravity.CENTER
                LinearSnapHelper()
            }
        }

        snapHelper?.also {
            try {
                it.attachToRecyclerView(recyclerView)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        stop()
    }

    // ----------------------------------------------------------------

    private fun createIndicator() {
        indicator?.apply {
            createIndicators(dataSize, currentPosition)
        }
    }

    // ----------------------------------------------------------------

    /**
     * Get carousel [CarouselItem]s.
     *
     * @return Return [CarouselItem]s or null if no item added yet.
     */
    fun getData(): List<CarouselItem>? {
        return data
    }

    /**
     * Set new data to the carousel. It removes all previous data.
     *
     * @property data List of [CarouselItem].
     */
    fun setData(data: List<CarouselItem>) {
        adapter?.apply {
            replaceData(data)

            this@ImageCarousel.data = data
            this@ImageCarousel.dataSize = data.size

            createIndicator()

            initOnScrollStateChange()

            isCarouselCentered = false

            initStartPositionForInfiniteCarousel()
        }
    }

    /**
     * Add multiple data to the carousel. It is the same as [setData], but it will not remove
     * previously added data. It just appends the items to the list.
     *
     * @property data List of [CarouselItem].
     */
    fun addData(data: List<CarouselItem>) {
        adapter?.apply {
            val isFirstTime = this@ImageCarousel.data.isNullOrEmpty()
            appendData(data)

            this@ImageCarousel.data = data
            this@ImageCarousel.dataSize = data.size

            createIndicator()

            initOnScrollStateChange()

            if (isFirstTime) {
                isCarouselCentered = false

                initStartPositionForInfiniteCarousel()
            }
        }
    }

    /**
     * Add single data to the carousel. It will not remove previously added data. It just appends
     * the item to the list.
     *
     * @property item Single [CarouselItem].
     */
    fun addData(item: CarouselItem) {
        adapter?.apply {
            val data = appendData(item)

            this@ImageCarousel.data = data
            this@ImageCarousel.dataSize = data.size

            createIndicator()

            initOnScrollStateChange()
        }
    }

    // ----------------------------------------------------------------

    /**
     * @return Current indicator or null.
     */
    fun getIndicator(): CircleIndicator2? {
        return indicator
    }

    /**
     * Set custom indicator.
     */
    fun setIndicator(newIndicator: CircleIndicator2) {
        indicator?.apply {
            // if we remove it form the view, then the caption textView constraint won't work.
            this.visibility = View.GONE

            isBuiltInIndicator = false
        }

        this.indicator = newIndicator
        initIndicator()
    }

    // ----------------------------------------------------------------

    /**
     * Initialize the start position for infinite carousel.
     *
     * The carousel will auto call this method on [setData] and [addData] (list version).
     */
    fun initStartPositionForInfiniteCarousel() {
        if (!infiniteCarousel)
            return

        recyclerView.post {
            // There is no data! So nothing to do.
            if (dataSize == 0)
                return@post

            // To making the view infinite, we create a virtual [Integer.MAX_VALUE] number of items.
            // We will find the first item from the middle of the whole virtual list.
            // 1073741823 is half of the [Integer.MAX_VALUE].
            val offset = 1073741823 % dataSize
            val finalPosition = 1073741823 - offset

            val layoutManager = recyclerView.layoutManager

            if (layoutManager is CarouselLinearLayoutManager) {
                // We assuming that every item will have the same size.
                // As we cannot get the view from the middle (because the view will not be created
                // until it gets visible), so we use the first item for getting the view width.
                val view = layoutManager.findViewByPosition(0)

                if (view == null) {
                    Log.e(
                        TAG,
                        "The first view is not found! Maybe the app is in the background."
                    )
                    return@post
                }

                if (carouselGravity == CarouselGravity.CENTER) {
                    layoutManager.scrollToPositionWithOffset(
                        finalPosition,
                        recyclerView.width / 2 - view.width / 2
                    )
                } else { // CarouselGravity.START
                    layoutManager.scrollToPositionWithOffset(
                        finalPosition,
                        0
                    )
                }

                isCarouselCentered = true
            }
        }
    }

    // ----------------------------------------------------------------

    /**
     * Register ImageCarousel to a lifecycle, so that the auto-play/scroll will pause when the
     * app is in the background. It is also used to correctly initialize the infinite carousel
     * when the app is in the background. It is recommended if you enabled auto-play & infinite
     * carousel.
     *
     * @param lifecycle A [androidx.lifecycle.LifecycleOwner]'s lifecycle.
     */
    fun registerLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    /**
     * It receives lifecycle owner as a parameter, especially for fragments.
     *
     * @see [registerLifecycle] for details.
     *
     * @param lifecycleOwner A [androidx.lifecycle.LifecycleOwner]
     */
    fun registerLifecycle(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    // ----------------------------------------------------------------

    /**
     * Goto previous item.
     *
     * Note: Sometimes it will not work. See [autoWidthFixing] for details.
     */
    fun previous() {
        currentPosition--
    }

    // ----------------------------------------------------------------

    /**
     * Goto next item.
     *
     * Note: Sometimes it will not work. See [autoWidthFixing] for details.
     */
    fun next() {
        currentPosition++
    }

    // ----------------------------------------------------------------

    /**
     * Start auto play.
     */
    fun start() {
        autoPlay = true
    }

    // ----------------------------------------------------------------

    /**
     * Stop auto play.
     */
    fun stop() {
        autoPlay = false
    }


    override fun setEnabled(enabled: Boolean) {
        recyclerView.isFocusable = enabled
        recyclerView.isClickable = enabled
        super.setEnabled(enabled)
    }
}
