package com.anifichadia.figstract.type.serializer

import com.anifichadia.figstract.figma.model.Variable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val figmaSerializersModule = SerializersModule {
    contextual(OffsetDateTimeSerializer())
    polymorphic(Variable::class) {
        subclass(Variable.BooleanVariable::class)
        subclass(Variable.NumberVariable::class)
        subclass(Variable.StringVariable::class)
        subclass(Variable.ColorVariable::class)
    }
}
