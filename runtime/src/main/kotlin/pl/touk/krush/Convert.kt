package pl.touk.krush

import pl.touk.krush.Converter
import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Convert(val value: KClass<out Converter<*, *>>)
