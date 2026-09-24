package com.self.kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform