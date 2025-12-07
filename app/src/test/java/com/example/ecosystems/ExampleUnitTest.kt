package com.example.ecosystems

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
/*
class VectorDrawableUtilsTest {

    private lateinit var mockContext: Context
    private lateinit var mockDrawable: Drawable
    private val testDrawableId = R.drawable.map_marker

    @Before
    fun setUp() {
        // Мокаем контекст и Drawable
        mockContext = mockk(relaxed = true)
        mockDrawable = mockk(relaxed = true)

        // Мокаем статические методы ContextCompat
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getBitmapFromVectorDrawable should return bitmap with correct dimensions`() {
        // Arrange
        val expectedWidth = 32
        val expectedHeight = 32

        every { ContextCompat.getDrawable(mockContext, testDrawableId) } returns mockDrawable
        every { mockDrawable.intrinsicWidth } returns expectedWidth
        every { mockDrawable.intrinsicHeight } returns expectedHeight

        // Act
        val result = getBitmapFromVectorDrawable(mockContext, testDrawableId)

        // Assert
        assertNotNull(result)
        assertEquals(expectedWidth, result.width)
        assertEquals(expectedHeight, result.height)
        assertEquals(Bitmap.Config.ARGB_8888, result.config)
    }

    @Test
    fun `getBitmapFromVectorDrawable should set bounds and draw on canvas`() {
        // Arrange
        every { ContextCompat.getDrawable(mockContext, testDrawableId) } returns mockDrawable
        every { mockDrawable.intrinsicWidth } returns 50
        every { mockDrawable.intrinsicHeight } returns 50

        // Act
        getBitmapFromVectorDrawable(mockContext, testDrawableId)

        // Assert - проверяем, что были вызваны нужные методы
        verify {
            mockDrawable.setBounds(0, 0, 50, 50)
            mockDrawable.draw(any<Canvas>())
        }
    }

    @Test
    fun `getBitmapFromVectorDrawable should call ContextCompat getDrawable with correct parameters`() {
        // Arrange
        every { ContextCompat.getDrawable(mockContext, testDrawableId) } returns mockDrawable
        every { mockDrawable.intrinsicWidth } returns 10
        every { mockDrawable.intrinsicHeight } returns 10

        // Act
        getBitmapFromVectorDrawable(mockContext, testDrawableId)

        // Assert
        verify { ContextCompat.getDrawable(mockContext, testDrawableId) }
    }

    @Test
    fun `getBitmapFromVectorDrawable should create non-null bitmap`() {
        // Arrange
        every { ContextCompat.getDrawable(mockContext, testDrawableId) } returns mockDrawable
        every { mockDrawable.intrinsicWidth } returns 200
        every { mockDrawable.intrinsicHeight } returns 200

        // Act
        val result = getBitmapFromVectorDrawable(mockContext, testDrawableId)

        // Assert
        assertNotNull(result)
        assertTrue(result.width > 0)
        assertTrue(result.height > 0)
        assertTrue(result.byteCount > 0)
    }

    @Test(expected = NullPointerException::class)
    fun `getBitmapFromVectorDrawable should throw exception when drawable is null`() {
        // Arrange
        every { ContextCompat.getDrawable(mockContext, testDrawableId) } returns null

        // Act & Assert
        getBitmapFromVectorDrawable(mockContext, testDrawableId)
    }
}
*/
