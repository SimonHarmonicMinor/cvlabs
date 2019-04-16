package com.kirekov.cvlabs.extension

import kotlinx.coroutines.newFixedThreadPoolContext

object ThreadPool {
    val pool = newFixedThreadPoolContext(10, "10 pool")
}