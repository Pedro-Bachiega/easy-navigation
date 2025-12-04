package com.pedrobneto.easy.navigation.core.annotation

import com.pedrobneto.easy.navigation.core.model.NavigationRoute
import kotlin.reflect.KClass

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Deeplink(val value: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Route(val value: KClass<out NavigationRoute>)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ParentRoute(val value: KClass<out NavigationRoute>)

@Repeatable
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class Scope(val value: String)
