package org.llschall.action.light.ribbon

import org.llschall.rgbribbon.RgbRibbon
import java.awt.Color

/// https://github.com/llschall/rgb-ribbon/blob/main/ino/rgb-ribbon/rgb-ribbon.ino
class Ribbon {

    fun start() {
        println("Ribbon started")
        val ribbon = RgbRibbon(0)
        ribbon.start()
        ribbon.setBrightness(3)
        ribbon.getLed(0).setColor(Color.yellow)
        ribbon.getLed(1).setColor(Color.green)
        ribbon.getLed(2).setColor(Color.yellow)
        ribbon.getLed(3).setColor(Color.yellow)
        ribbon.publish()
    }

}