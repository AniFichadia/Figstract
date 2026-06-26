package com.anifichadia.figstract.web

import com.anifichadia.figstract.Conventions
import com.anifichadia.figstract.model.TokenStringGenerator

val Conventions.Casing.web: TokenStringGenerator.Casing
    get() = TokenStringGenerator.Casing.SnakeCase
