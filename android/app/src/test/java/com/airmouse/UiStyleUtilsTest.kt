package com.airmouse

import android.content.Context
import androidx.cardview.widget.CardView
import com.airmouse.ui.UiStyleUtils
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UiStyleUtilsTest {

    @Test
    fun `styleCard enables compat padding for material cards`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val card = MaterialCardView(context)

        UiStyleUtils.styleCard(card)

        assertTrue(card.isUseCompatPadding)
    }

    @Test
    fun `styleCard enables compat padding for card views`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val card = CardView(context)

        UiStyleUtils.styleCard(card)

        assertTrue(card.useCompatPadding)
    }
}
