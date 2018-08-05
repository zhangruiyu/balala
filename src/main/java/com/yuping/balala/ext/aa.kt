package com.yuping.balala.ext


fun main(args: Array<String>) {
    val listOf = arrayOf(5, 6, 6, 7, 5, 6, 8, 15, 2, 4, 4, 14)
    f(listOf)
}


fun f(args: Array<Int>) {
    try {

        var min = args[0]
        var delta = Int.MIN_VALUE
        var max = 0
        args.forEachIndexed { index, i ->
            if (i < min) {
                min = i
            } else {
                delta = i - min
                max = index
            }
        }
        println(delta)
        println(max)
    } catch (e: Exception) {
        println("list error")
    }
}
