package com.nuvio.app.features.hub

data class SlotPos(val index: Int, val row: Int, val col: Int, val rowSpan: Int, val colSpan: Int)

enum class MultiWindowLayout(val label: String) {
    V2_SPLIT("1\u00D72"),
    V2_STACK("2\u00D71"),
    V3_TOP1_BOT2("1+2"),
    V3_LEFT2_RIGHT1("2+1"),
    V3_STACK("3 vert"),
    V4_GRID("2\u00D72"),
    V4_1_2_1("1-2-1"),
    V5_TOP1_BOT4("1+4"),
    V5_LEFT3_RIGHT2("3+2"),
    V5_GRID4_BOT1("4+1"),
    V6_3x2("3\u00D72"),
    V6_2x3("2\u00D73"),
    V6_1_2_2_1("1-2-2-1"),
    V7_1_3_3("1-3-3"),
    V7_TOP1_BOT6("1+6"),
    V8_GRID4x2("4\u00D72"),
    V8_2x4("2\u00D74"),
    V8_1_3_3_1("1-3-3-1"),
    V9_GRID3x3("3\u00D73"),
}

fun getValidLayouts(count: Int, isPortrait: Boolean): List<MultiWindowLayout> {
    return when (count) {
        2 -> listOf(MultiWindowLayout.V2_SPLIT, MultiWindowLayout.V2_STACK)
        3 -> if (isPortrait) listOf(MultiWindowLayout.V3_TOP1_BOT2, MultiWindowLayout.V3_STACK)
             else listOf(MultiWindowLayout.V3_LEFT2_RIGHT1, MultiWindowLayout.V3_STACK)
        4 -> listOf(MultiWindowLayout.V4_GRID, MultiWindowLayout.V4_1_2_1)
        5 -> if (isPortrait) listOf(MultiWindowLayout.V5_TOP1_BOT4, MultiWindowLayout.V5_GRID4_BOT1)
             else listOf(MultiWindowLayout.V5_LEFT3_RIGHT2, MultiWindowLayout.V5_GRID4_BOT1)
        6 -> if (isPortrait) listOf(MultiWindowLayout.V6_3x2, MultiWindowLayout.V6_1_2_2_1)
             else listOf(MultiWindowLayout.V6_2x3, MultiWindowLayout.V6_1_2_2_1)
        7 -> if (isPortrait) listOf(MultiWindowLayout.V7_1_3_3, MultiWindowLayout.V7_TOP1_BOT6)
             else listOf(MultiWindowLayout.V7_1_3_3)
        8 -> if (isPortrait) listOf(MultiWindowLayout.V8_GRID4x2, MultiWindowLayout.V8_1_3_3_1)
             else listOf(MultiWindowLayout.V8_2x4, MultiWindowLayout.V8_GRID4x2)
        9 -> listOf(MultiWindowLayout.V9_GRID3x3)
        else -> emptyList()
    }
}

fun defaultLayout(count: Int, isPortrait: Boolean): MultiWindowLayout = getValidLayouts(count, isPortrait).first()

fun MultiWindowLayout.calculateSlots(count: Int): List<SlotPos> {
    return when (this) {
        MultiWindowLayout.V2_SPLIT -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1),
        )
        MultiWindowLayout.V2_STACK -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 1, 0, 1, 1),
        )
        MultiWindowLayout.V3_TOP1_BOT2 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
        )
        MultiWindowLayout.V3_LEFT2_RIGHT1 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 0, 1, 2, 1),
        )
        MultiWindowLayout.V3_STACK -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 2, 0, 1, 1),
        )
        MultiWindowLayout.V4_GRID -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1),
            SlotPos(2, 1, 0, 1, 1), SlotPos(3, 1, 1, 1, 1),
        )
        MultiWindowLayout.V4_1_2_1 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1),
            SlotPos(2, 1, 1, 1, 1), SlotPos(3, 2, 0, 1, 2),
        )
        MultiWindowLayout.V5_TOP1_BOT4 -> listOf(
            SlotPos(0, 0, 0, 1, 2), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
            SlotPos(3, 2, 0, 1, 1), SlotPos(4, 2, 1, 1, 1),
        )
        MultiWindowLayout.V5_LEFT3_RIGHT2 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 1, 0, 1, 1), SlotPos(2, 2, 0, 1, 1),
            SlotPos(3, 0, 1, 1, 1), SlotPos(4, 1, 1, 1, 1),
        )
        MultiWindowLayout.V5_GRID4_BOT1 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1),
            SlotPos(2, 1, 0, 1, 1), SlotPos(3, 1, 1, 1, 1),
            SlotPos(4, 2, 0, 1, 2),
        )
        MultiWindowLayout.V6_3x2 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1),
            SlotPos(2, 1, 0, 1, 1), SlotPos(3, 1, 1, 1, 1),
            SlotPos(4, 2, 0, 1, 1), SlotPos(5, 2, 1, 1, 1),
        )
        MultiWindowLayout.V6_2x3 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1), SlotPos(2, 0, 2, 1, 1),
            SlotPos(3, 1, 0, 1, 1), SlotPos(4, 1, 1, 1, 1), SlotPos(5, 1, 2, 1, 1),
        )
        MultiWindowLayout.V6_1_2_2_1 -> listOf(
            SlotPos(0, 0, 0, 1, 2),
            SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
            SlotPos(3, 2, 0, 1, 1), SlotPos(4, 2, 1, 1, 1),
            SlotPos(5, 3, 0, 1, 2),
        )
        MultiWindowLayout.V7_1_3_3 -> listOf(
            SlotPos(0, 0, 0, 1, 3),
            SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1), SlotPos(3, 1, 2, 1, 1),
            SlotPos(4, 2, 0, 1, 1), SlotPos(5, 2, 1, 1, 1), SlotPos(6, 2, 2, 1, 1),
        )
        MultiWindowLayout.V7_TOP1_BOT6 -> listOf(
            SlotPos(0, 0, 0, 1, 2),
            SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1),
            SlotPos(3, 2, 0, 1, 1), SlotPos(4, 2, 1, 1, 1),
            SlotPos(5, 3, 0, 1, 1), SlotPos(6, 3, 1, 1, 1),
        )
        MultiWindowLayout.V8_GRID4x2 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1),
            SlotPos(2, 1, 0, 1, 1), SlotPos(3, 1, 1, 1, 1),
            SlotPos(4, 2, 0, 1, 1), SlotPos(5, 2, 1, 1, 1),
            SlotPos(6, 3, 0, 1, 1), SlotPos(7, 3, 1, 1, 1),
        )
        MultiWindowLayout.V8_2x4 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1), SlotPos(2, 0, 2, 1, 1), SlotPos(3, 0, 3, 1, 1),
            SlotPos(4, 1, 0, 1, 1), SlotPos(5, 1, 1, 1, 1), SlotPos(6, 1, 2, 1, 1), SlotPos(7, 1, 3, 1, 1),
        )
        MultiWindowLayout.V8_1_3_3_1 -> listOf(
            SlotPos(0, 0, 0, 1, 3),
            SlotPos(1, 1, 0, 1, 1), SlotPos(2, 1, 1, 1, 1), SlotPos(3, 1, 2, 1, 1),
            SlotPos(4, 2, 0, 1, 1), SlotPos(5, 2, 1, 1, 1), SlotPos(6, 2, 2, 1, 1),
            SlotPos(7, 3, 0, 1, 3),
        )
        MultiWindowLayout.V9_GRID3x3 -> listOf(
            SlotPos(0, 0, 0, 1, 1), SlotPos(1, 0, 1, 1, 1), SlotPos(2, 0, 2, 1, 1),
            SlotPos(3, 1, 0, 1, 1), SlotPos(4, 1, 1, 1, 1), SlotPos(5, 1, 2, 1, 1),
            SlotPos(6, 2, 0, 1, 1), SlotPos(7, 2, 1, 1, 1), SlotPos(8, 2, 2, 1, 1),
        )
    }.take(count)
}
