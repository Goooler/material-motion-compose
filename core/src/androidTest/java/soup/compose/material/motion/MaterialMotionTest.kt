/*
 * Copyright 2021 SOUP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package soup.compose.material.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

abstract class MaterialMotionTest {

    protected abstract fun transitionSpec(
        forward: Boolean,
        durationMillis: Int? = null,
    ): ContentTransform

    @get:Rule
    val rule: ComposeContentTestRule = createComposeRule()

    @Test
    fun showsContent() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            val showFirst by remember { mutableStateOf(true) }
            MaterialMotion(
                showFirst,
                transitionSpec = { transitionSpec(forward = showFirst) },
                pop = showFirst.not()
            ) {
                BasicText(if (it) First else Second)
            }
        }
        rule.mainClock.advanceTimeBy(defaultDurationMillis.toLong())

        rule.onNodeWithText(First).assertExists()
    }

    @Test
    fun disposesContentOnChange() {
        rule.mainClock.autoAdvance = false

        var showFirst by mutableStateOf(true)
        var disposed = false
        rule.setContent {
            MaterialMotion(
                showFirst,
                transitionSpec = { transitionSpec(forward = showFirst) },
                pop = showFirst.not()
            ) {
                BasicText(if (it) First else Second)
                DisposableEffect(Unit) {
                    onDispose {
                        disposed = true
                    }
                }
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(defaultDurationMillis.toLong())

        rule.runOnUiThread {
            showFirst = false
        }

        // Wait for content to be disposed
        rule.mainClock.advanceTimeUntil { disposed }

        rule.onNodeWithText(First).assertDoesNotExist()
        rule.onNodeWithText(Second).assertExists()
    }

    @Test
    fun durationCanBeModifier() {
        rule.mainClock.autoAdvance = false

        val duration = 100 // smaller than default 300
        var showFirst by mutableStateOf(true)
        var disposed = false
        rule.setContent {
            MaterialMotion(
                showFirst,
                transitionSpec = { transitionSpec(forward = showFirst, durationMillis = duration) },
                pop = showFirst.not()
            ) {
                BasicText(if (it) First else Second)
                DisposableEffect(Unit) {
                    onDispose {
                        disposed = true
                    }
                }
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(duration.toLong())

        rule.runOnUiThread {
            showFirst = false
        }

        rule.mainClock.advanceTimeBy(duration.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        assertTrue(disposed)
    }

    @Test
    fun nullInitialValue() {
        rule.mainClock.autoAdvance = false
        var current by mutableStateOf<String?>(null)

        rule.setContent {
            MaterialMotion(
                current,
                transitionSpec = { transitionSpec(forward = current != null) },
                pop = current == null
            ) { value ->
                BasicText(if (value == null) First else Second)
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(defaultDurationMillis.toLong())

        rule.onNodeWithText(First).assertExists()
        rule.onNodeWithText(Second).assertDoesNotExist()

        rule.runOnUiThread {
            current = "other"
        }

        rule.mainClock.advanceTimeBy(defaultDurationMillis.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        rule.onNodeWithText(First).assertDoesNotExist()
        rule.onNodeWithText(Second).assertExists()
    }

    @Test
    fun rememberSaveableIsNotRecreatedForScreens() {
        rule.mainClock.autoAdvance = false

        val duration = 100
        var showFirst by mutableStateOf(true)
        var counter = 1
        var counter1 = 0
        var counter2 = 0
        rule.setContent {
            val saveableStateHolder = rememberSaveableStateHolder()
            MaterialMotion(
                showFirst,
                transitionSpec = { transitionSpec(forward = showFirst, durationMillis = duration) },
                pop = showFirst.not()
            ) {
                saveableStateHolder.SaveableStateProvider(it) {
                    if (it) {
                        counter1 = rememberSaveable { counter++ }
                    } else {
                        counter2 = rememberSaveable { counter++ }
                    }
                }
            }
        }

        rule.mainClock.advanceTimeByFrame() // Kick off the animation
        rule.mainClock.advanceTimeBy(duration.toLong())

        rule.runOnUiThread {
            showFirst = false
        }

        rule.mainClock.advanceTimeBy(duration.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        // and go back to the second screen

        rule.runOnUiThread {
            showFirst = true
        }

        rule.mainClock.advanceTimeBy(duration.toLong())
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame() // Wait for changes to propagate

        assertEquals(1, counter1)
        assertEquals(2, counter2)
    }

    companion object {
        private const val First = "first"
        private const val Second = "second"
        private const val defaultDurationMillis: Int = MotionConstants.DefaultMotionDuration
    }
}
