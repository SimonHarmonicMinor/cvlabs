package com.kirekov.cvlabs.image.filter

interface Filter {
    /**
     * Размер фильтра
     */
    val size: Int

    /**
     * Получить значение фильтра (0, 0) - центр фильтра
     */
    fun getValue(x: Int, y: Int): Double
}