package com.anifichadia.figstract.cli.core.assets.model

import kotlinx.serialization.Serializable

@Serializable
data class BatchFormat(val batches: List<AssetConfig>)
