package com.github.bea4dev.vanilla_source.hud

import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.scoreboard.Sidebar

class LinedSidebar(title: Component, private val line: Int) {
    private val sidebar = Sidebar(title)

    init {
        for (i in 0 until line) {
            sidebar.createLine(Sidebar.ScoreboardLine(i.toString(), Component.empty(), i))
        }
    }

    private fun getLineID(line: Int): String {
        return (this.line - line - 1).toString()
    }

    fun setLine(line: Int, text: Component) {
        sidebar.updateLineContent(getLineID(line), text)
    }

    fun addViewer(player: Player) {
        sidebar.addViewer(player)
    }

    fun removeViewer(player: Player) {
        sidebar.removeViewer(player)
    }

}