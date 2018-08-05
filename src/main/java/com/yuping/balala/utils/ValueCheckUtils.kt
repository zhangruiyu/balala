package com.yuping.balala.utils

class ValueCheckUtils {
    companion object {
        fun isTel(tel: String): Boolean {
            return tel.length == 11
        }

        fun isPassword(password: String): Boolean {
            return password.isNotEmpty()
        }
    }
}
