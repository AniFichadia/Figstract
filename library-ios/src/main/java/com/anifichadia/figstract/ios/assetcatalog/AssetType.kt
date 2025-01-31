package com.anifichadia.figstract.ios.assetcatalog

/**
 * https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/AssetTypes.html#//apple_ref/doc/uid/TP40015170-CH30-SW1
 */
sealed class AssetType(val directorySuffix: String) {
    sealed class Image(directorySuffix: String) : AssetType(directorySuffix) {
        data object ImageSet : Image("imageset")
        data object IconSet : Image("iconset")
    }

    sealed class Theme(directorySuffix: String) : AssetType(directorySuffix) {
        data object ColorSet : Theme("colorset")
        data object StringSet : Theme("stringset")
    }
}

fun AssetType.directoryName(name: String): String {
    return "${name}.${directorySuffix}"
}
