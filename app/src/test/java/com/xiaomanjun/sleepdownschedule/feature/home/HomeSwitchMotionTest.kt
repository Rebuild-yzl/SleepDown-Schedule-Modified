package com.xiaomanjun.sleepdownschedule.feature.home

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.yield
import org.junit.Assert.*
import org.junit.Test

class HomeSwitchMotionTest {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun nativeMaterialKeepsDensePageFollowersAndRebound() = runTest {
        val frames = Frames()
        val target = mutableIntStateOf(1)
        val motion = HomeSwitchMotion(0, target, fullMotion = true, renderedCardCount = { 30 })
        assertFalse(motion.plainSlide)
        val job = launch(frames.clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(1) }
        var sawStagger = false
        var sawRebound = false
        repeat(120) {
            // Group starts use coroutine delay; advance it together with animation frames.
            advanceTimeBy(17)
            runCurrent()
            frames.next()
            runCurrent()
            val groups = (0 until 6).map(motion::groupProgress)
            sawStagger = sawStagger || groups.any { it != motion.progress.value }
            sawRebound = sawRebound || groups.any { it > 1f }
        }
        assertTrue(sawStagger)
        assertTrue(sawRebound)
        assertTrue(job.isCompleted)
        assertFalse(motion.moving)
        assertFalse(motion.plainSlide)
        job.cancelAndJoin()
    }

    @Test fun densePagesSelectPlainSlideBeforeTheFirstTransitionFrame() {
        val count = mutableIntStateOf(0)
        val motion = HomeSwitchMotion(0, mutableIntStateOf(0), renderedCardCount = { count.intValue })
        assertFalse(motion.plainSlide)
        count.intValue = 20
        assertTrue(motion.plainSlide)
        assertFalse(motion.moving)
    }

    private class Frames {
        val clock = BroadcastFrameClock()
        private var time = 0L

        suspend fun next() {
            clock.sendFrame(time)
            time += 16_666_667L
            repeat(3) { yield() }
        }
    }

    @Test fun tenCardsKeepStaggerWhileElevenMoveTogetherWithoutOvershoot() = runBlocking {
        for (count in listOf(10, 11)) {
            val frames = Frames()
            val target = mutableIntStateOf(1)
            val motion = HomeSwitchMotion(0, target, renderedCardCount = { count })
            val job = launch(frames.clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(1) }
            var sawStagger = false
            var previous = 0f
            repeat(90) {
                frames.next()
                val page = motion.progress.value
                sawStagger = sawStagger || (0 until 6).any { motion.groupProgress(it) != page }
                if (count == 11) {
                    assertTrue(page >= previous && page <= 1f)
                    for (group in 0 until 6) assertEquals(page, motion.groupProgress(group), 0f)
                }
                previous = page
            }
            assertEquals(count == 10, sawStagger)
            if (count == 11) {
                assertTrue(job.isCompleted)
                assertFalse(motion.moving)
                assertFalse(motion.retains(false))
            }
            job.cancelAndJoin()
        }
    }

    @Test fun reversalKeepsItsStyleAndPositionThenNextSwitchRechecksCount() = runBlocking {
        val frames = Frames()
        val target = mutableIntStateOf(1)
        var count = 11
        val motion = HomeSwitchMotion(0, target, renderedCardCount = { count })
        val outgoing = launch(frames.clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(1) }
        repeat(7) { frames.next() }
        assertTrue(motion.progress.value in 0.01f..0.99f)
        val interruptedPosition = motion.progress.value
        outgoing.cancelAndJoin()
        count = 10
        target.value = 0
        val returning = launch(frames.clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(0) }
        assertEquals(interruptedPosition, motion.progress.value, 0f)
        assertTrue(motion.plainSlide)
        repeat(90) {
            frames.next()
            for (group in 0 until 6) assertEquals(motion.progress.value, motion.groupProgress(group), 0f)
        }
        assertTrue(returning.isCompleted)
        assertFalse(motion.moving)
        assertFalse(motion.retains(true))

        target.value = 1
        val next = launch(frames.clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(1) }
        assertFalse(motion.plainSlide)
        next.cancelAndJoin()
        motion.settleAt(1)
        assertFalse(motion.moving)
        for (group in 0 until 6) assertEquals(1f, motion.groupProgress(group), 0f)
    }

    @Test fun denseLandscapeAlsoUsesPlainSlideAndSettleClearsInterruptedMotion() = runBlocking {
        val frames = Frames()
        val target = mutableIntStateOf(1)
        val motion = HomeSwitchMotion(0, target, landscape = true, renderedCardCount = { 11 })
        val job = launch(frames.clock, start = CoroutineStart.UNDISPATCHED) { motion.animateTo(1) }
        repeat(7) { frames.next() }
        assertTrue(motion.plainSlide)
        assertTrue(motion.moving)
        job.cancelAndJoin()
        target.value = 0
        motion.settleAt(0)
        assertFalse(motion.moving)
        assertFalse(motion.retains(true))
        assertEquals(0f, motion.progress.value, 0f)
        for (group in 0 until 6) assertEquals(0f, motion.groupProgress(group), 0f)
    }
}
