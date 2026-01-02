package org.llschall.continuous.light.ribbon

import org.llschall.rgbribbon.RgbRibbon
import java.awt.Color
import java.util.concurrent.atomic.AtomicInteger

/// https://github.com/llschall/rgb-ribbon/blob/main/ino/rgb-ribbon/rgb-ribbon.ino
class Ribbon {

    private val ribbon: RgbRibbon = RgbRibbon(0)

    fun start() {
        println("Ribbon started")
        ribbon.start()
        ribbon.setBrightness(3)
        for (i in 0 until 9) {
            ribbon.getLed(i).setColor(Color.green)
        }
        ribbon.publish()
    }

    fun update(count: Int) {
        if (count == 0) {
            ribbon.setBrightness(2)
            for (i in 0 until 9) {
                ribbon.getLed(i).setColor(Color.green)
            }
            ribbon.publish()
            return
        }

        ribbon.setBrightness(20)
        for (i in 0 until count) {
            ribbon.getLed(i).setColor(Color.yellow)

        }
        for (i in count until 9) {
            ribbon.getLed(i).setColor(Color.blue)
        }
        ribbon.publish()
    }

}