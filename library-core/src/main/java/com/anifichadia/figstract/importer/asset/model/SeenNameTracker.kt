package com.anifichadia.figstract.importer.asset.model


/**
 * Reports on the canvas and node names actually encountered while building an [AssetFileHandler]'s instructions.
 */
fun interface SeenNameTracker {
    /**
     * Called once, after all canvases and nodes have been processed, with every canvas/node name that was actually seen.
     */
    fun report(seenCanvasNames: Set<String>, seenNodeNames: Set<String>)

    companion object {
        val NoOp = SeenNameTracker { _, _ -> }
    }
}
