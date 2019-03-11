package com.kirekov.cvlabs.extension.parallel

import java.util.concurrent.Executors

object ThreadPool {
    val pool = Executors.newFixedThreadPool(10)
}