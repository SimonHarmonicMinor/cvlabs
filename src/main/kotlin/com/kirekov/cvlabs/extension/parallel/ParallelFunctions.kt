package com.kirekov.cvlabs.extension.parallel

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.map { it.await() }
}


suspend fun <A, B> Iterable<A>.pmapIndexed(f: suspend (Int, A) -> B): List<B> = coroutineScope {
    mapIndexed { index, item ->
        async {
            f(
                index,
                item
            )
        }
    }.map { it.await() }
}