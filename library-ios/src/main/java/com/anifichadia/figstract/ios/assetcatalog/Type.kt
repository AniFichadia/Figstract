package com.anifichadia.figstract.ios.assetcatalog

sealed class Type(val directorySuffix: String) {
    sealed class Image(directorySuffix: String) : Type(directorySuffix) {
        data object ImageSet : Image("imageset")
        data object IconSet : Image("iconset")
    }
}
