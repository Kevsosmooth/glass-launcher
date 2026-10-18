package com.kevin.glasslauncher.update

/** The [FailReason] [block] throws; fails the test when it throws nothing. */
fun failureOf(block: () -> Unit): FailReason {
    try {
        block()
    } catch (e: UpdateException) {
        return e.reason
    }
    throw AssertionError("expected an UpdateException")
}
