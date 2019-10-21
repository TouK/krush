package pl.touk.exposed

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Convert(val value: KClass<out Converter<*, *>>)
