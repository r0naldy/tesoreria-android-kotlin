package pe.edu.hzg.tesoreria.data.local

import app.cash.sqldelight.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

fun <T : Any> Query<T>.asListFlow(dispatcher: CoroutineDispatcher): Flow<List<T>> =
    callbackFlow {
        val listener = Query.Listener { trySend(executeAsList()) }
        addListener(listener)
        trySend(executeAsList())
        awaitClose { removeListener(listener) }
    }.flowOn(dispatcher)

fun <T : Any> Query<T>.asOneFlow(dispatcher: CoroutineDispatcher): Flow<T> =
    callbackFlow {
        val listener = Query.Listener { trySend(executeAsOne()) }
        addListener(listener)
        trySend(executeAsOne())
        awaitClose { removeListener(listener) }
    }.flowOn(dispatcher)
