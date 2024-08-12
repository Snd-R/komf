package snd.komf.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(PatchValueSerializer::class)
sealed class PatchValue<out T> {
    data object Unset : PatchValue<Nothing>()
    data object None : PatchValue<Nothing>()
    class Some<T>(val value: T) : PatchValue<T>()

    fun <T> patch(original: T?, patch: T?): PatchValue<T> {
        return when {
            original == patch -> Unset
            patch == null -> None
            else -> Some(patch)
        }
    }

    fun getOrNull(): T? = when (this) {
        None, Unset -> null
        is Some -> value
    }
}

class PatchValueSerializer<T : Any>(
    private val valueSerializer: KSerializer<T>
) : KSerializer<PatchValue<T>> {
    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): PatchValue<T> {
        return when (val value = decoder.decodeNullableSerializableValue(valueSerializer)) {
            null -> PatchValue.None
            else -> PatchValue.Some(value)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: PatchValue<T>) {
        when (value) {
            PatchValue.None -> encoder.encodeNull()
            is PatchValue.Some -> valueSerializer.serialize(encoder, value.value)
            PatchValue.Unset -> throw SerializationException("Value is unset. Make sure that property has default unset value")
        }
    }
}
