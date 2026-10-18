package com.kevin.glasslauncher.update

import java.io.File

/** Records what the controller asks Android to do; [problem] is the identity check's answer. */
class FakeInstaller(var problem: FailReason? = null) : ApkInstaller {
    val installed = mutableListOf<ByteArray>()
    var abandoned = 0
        private set

    override fun problem(apk: File): FailReason? = problem

    override fun install(apk: File) {
        installed += apk.readBytes()
    }

    override fun abandon() {
        abandoned++
    }
}
