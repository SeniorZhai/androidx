/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XEquality
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import com.google.devtools.ksp.symbol.ClassKind
import androidx.room.compiler.processing.tryBox
import androidx.room.compiler.processing.tryUnbox
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Nullability
import kotlin.reflect.KClass

/**
 * XType implementation for KSP type.
 *
 * It might be initialized with a [KSTypeReference] or [KSType] depending on the call point.
 *
 * We don't necessarily have a [KSTypeReference] (e.g. if we are getting it from an element).
 * Similarly, we may not be able to get a [KSType] (e.g. if it resolves to error).
 */
internal abstract class KspType(
    val env: KspProcessingEnv,
    val ksType: KSType
) : XType, XEquality {
    override val rawType by lazy {
        KspRawType(this)
    }

    override val nullability by lazy {
        when (ksType.nullability) {
            Nullability.NULLABLE -> XNullability.NULLABLE
            Nullability.NOT_NULL -> XNullability.NONNULL
            else -> XNullability.UNKNOWN
        }
    }

    private val _typeElement by lazy {
        check(ksType.declaration is KSClassDeclaration) {
            """
            Unexpected case where ksType's declaration is not a KSClassDeclaration.
            Please file a bug.
            """.trimIndent()
        }
        env.wrapClassDeclaration(ksType.declaration as KSClassDeclaration)
    }

    override fun asTypeElement(): XTypeElement {
        return _typeElement
    }

    override fun isAssignableFrom(other: XType): Boolean {
        check(other is KspType)
        return ksType.isAssignableFrom(other.ksType)
    }

    override fun isError(): Boolean {
        return ksType.isError
    }

    override fun defaultValue(): String {
        // NOTE: this does not match the java implementation though it is probably more correct for
        // kotlin.
        if (ksType.nullability == Nullability.NULLABLE) {
            return "null"
        }
        val builtIns = env.resolver.builtIns
        return when (ksType) {
            builtIns.booleanType -> "false"
            builtIns.byteType, builtIns.shortType, builtIns.intType, builtIns.longType, builtIns
                .charType -> "0"
            builtIns.floatType -> "0f"
            builtIns.doubleType -> "0.0"
            else -> "null"
        }
    }

    override fun isInt(): Boolean {
        return env.commonTypes.nullableInt.isAssignableFrom(ksType)
    }

    override fun isLong(): Boolean {
        return env.commonTypes.nullableLong.isAssignableFrom(ksType)
    }

    override fun isByte(): Boolean {
        return env.commonTypes.nullableByte.isAssignableFrom(ksType)
    }

    override fun isNone(): Boolean {
        // even void is converted to Unit so we don't have none type in KSP
        // see: KspTypeTest.noneType
        return false
    }

    override fun isType(): Boolean {
        return ksType.declaration is KSClassDeclaration
    }

    override fun isTypeOf(other: KClass<*>): Boolean {
        // closest to what MoreTypes#isTypeOf does.
        // accept both boxed and unboxed because KClass.java for primitives wrappers will always
        // give the primitive (e.g. kotlin.Int::class.java is int)
        return rawType.typeName.tryBox().toString() == other.java.canonicalName ||
            rawType.typeName.tryUnbox().toString() == other.java.canonicalName
    }

    override fun isSameType(other: XType): Boolean {
        check(other is KspType)
        // NOTE: this is inconsistent with java where nullability is ignored.
        // it is intentional but might be reversed if it happens to break use cases.
        return ksType == other.ksType
    }

    override fun extendsBound(): XType? {
        // NOTE: wildcard does not fully exist in kotlin and when we resolve, it always seems to
        // be mapped to the upper bound. Might still need more investigation here
        // https://kotlinlang.org/docs/reference/generics.html#star-projections
        return null
    }

    override val equalityItems: Array<out Any?> by lazy {
        arrayOf(ksType)
    }

    override fun equals(other: Any?): Boolean {
        return XEquality.equals(this, other)
    }

    override fun hashCode(): Int {
        return XEquality.hashCode(equalityItems)
    }

    override fun toString(): String {
        return ksType.toString()
    }

    override fun isEnum(): Boolean {
        return (ksType.declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS
    }
}
