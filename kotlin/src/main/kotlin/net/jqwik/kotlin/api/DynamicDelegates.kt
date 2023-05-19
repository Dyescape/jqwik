package net.jqwik.kotlin.api

import net.jqwik.api.Arbitrary
import net.jqwik.api.dynamic.Dynamic
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DynamicValueProvider<T>(
    private val provider: () -> Arbitrary<T>
) : PropertyDelegateProvider<Nothing?, DynamicParameter<T>> {
    override fun provideDelegate(thisRef: Nothing?, property: KProperty<*>): DynamicParameter<T> {
        return DynamicParameter(Dynamic.parameter(property.name, provider))
    }
}

class DynamicParameter<T>(private val value: T) : ReadOnlyProperty<Nothing?, T> {
    override fun getValue(thisRef: Nothing?, property: KProperty<*>): T {
        return value
    }
}

fun <T> arbitrary(provider: () -> Arbitrary<T>): DynamicValueProvider<T> {
    return DynamicValueProvider(provider)
}
