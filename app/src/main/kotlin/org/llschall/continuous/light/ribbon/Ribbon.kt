package org.llschall.continuous.light.ribbon

import org.llschall.continuous.light.request.Status
import org.llschall.rgbribbon.RgbRibbon
import java.awt.Color

/// https://github.com/llschall/rgb-ribbon/blob/main/ino/rgb-ribbon/rgb-ribbon.ino
class Ribbon {

    private val ribbon: RgbRibbon = RgbRibbon(0)

    fun start() {
        println("Ribbon started")
        ribbon.start()
        ribbon.setBrightness(3)
        for (i in 0 until 9) {
            ribbon.getLed(i).setColor(Color.blue)
        }
        ribbon.publish()
    }

    fun update(list: List<Status>) {
        if (list.isEmpty()) {
            ribbon.setBrightness(2)
            for (i in 0 until 9) {
                ribbon.getLed(i).setColor(Color.blue)
            }
            ribbon.publish()
            return
        }

        ribbon.setBrightness(20)
        val count = list.size
        for (i in 0 until count) {
            list.get(i).let { status ->
                ribbon.getLed(i).setColor(status.color)
            }
        }
        for (i in count until 9) {
            ribbon.getLed(i).setColor(Color.black)
        }
        ribbon.publish()
    }

}