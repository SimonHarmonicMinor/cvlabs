package com.kirekov.cvlabs.extension

import kotlinx.coroutines.newFixedThreadPoolContext

object ThreadPool {
    val pool = newFixedThreadPoolContext(15, "10 pool")
}