package com.github.bea4dev.vanilla_source.util

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError

inline fun <reified T, R: Throwable> Result<T, R>.unwrap(): T {
    return when (this) {
        is Ok -> this.value
        is Err -> throw Exception("called Result.unwrap().", this.getError())
    }
}