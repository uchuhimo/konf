package com.uchuhimo.konf.annotation

@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class JavaApi
