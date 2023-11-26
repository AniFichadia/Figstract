package com.anifichadia.figmaimporter.model

interface Describeable {
    fun describe(): String

    companion object {
        fun Any.describeOrToString(): String {
            return if (this is Describeable) {
                this.describe()
            } else {
                this.toString()
            }
        }
    }
}
