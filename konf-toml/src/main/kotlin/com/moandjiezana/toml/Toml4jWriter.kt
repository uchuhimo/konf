/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.moandjiezana.toml

import com.moandjiezana.toml.BooleanValueReaderWriter.BOOLEAN_VALUE_READER_WRITER
import com.moandjiezana.toml.DateValueReaderWriter.DATE_PARSER_JDK_6
import com.moandjiezana.toml.DateValueReaderWriter.DATE_VALUE_READER_WRITER
import com.moandjiezana.toml.NumberValueReaderWriter.NUMBER_VALUE_READER_WRITER
import com.moandjiezana.toml.StringValueReaderWriter.STRING_VALUE_READER_WRITER
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Arrays
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.TimeZone
import java.util.regex.Pattern

/**
 * <p>Converts Objects to TOML</p>
 *
 * <p>An input Object can comprise arbitrarily nested combinations of Java primitive types,
 * other {@link Object}s, {@link Map}s, {@link List}s, and Arrays. {@link Object}s and {@link Map}s
 * are output to TOML tables, and {@link List}s and Array to TOML arrays.</p>
 *
 * <p>Example usage:</p>
 * <pre><code>
 * class AClass {
 *   int anInt = 1;
 *   int[] anArray = { 2, 3 };
 * }
 *
 * String tomlString = new TomlWriter().write(new AClass());
 * </code></pre>
 */
class Toml4jWriter {
    /**
     * Write an Object into TOML String.
     *
     * @param from the object to be written
     * @return a string containing the TOML representation of the given Object
     */
    fun write(from: Any): String {
        try {
            val output = StringWriter()
            write(from, output)
            return output.toString()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    /**
     * Write an Object in TOML to a [Writer]. You MUST ensure that the [Writer]s's encoding is set to UTF-8 for the TOML to be valid.
     *
     * @param from the object to be written. Can be a Map or a custom type. Must not be null.
     * @param target the Writer to which TOML will be written. The Writer is not closed.
     * @throws IOException if target.write() fails
     * @throws IllegalArgumentException if from is of an invalid type
     */
    @Throws(IOException::class)
    fun write(from: Any, target: Writer) {
        val valueWriter = Toml4jValueWriters.findWriterFor(from)
        if (valueWriter === NewMapValueWriter || valueWriter === NewObjectValueWriter) {
            val context = WriterContext(
                IndentationPolicy(0, 0, 0),
                DatePolicy(TimeZone.getTimeZone("UTC"), false),
                target
            )
            valueWriter.write(from, context)
        } else {
            throw IllegalArgumentException("An object of class " + from.javaClass.simpleName + " cannot produce valid TOML. Please pass in a Map or a custom type.")
        }
    }
}

internal object Toml4jValueWriters {

    fun findWriterFor(value: Any): ValueWriter {
        for (valueWriter in VALUE_WRITERS) {
            if (valueWriter.canWrite(value)) {
                return valueWriter
            }
        }
        return NewObjectValueWriter
    }

    private val platformSpecificDateConverter: DateValueReaderWriter
        get() {
            val specificationVersion = Runtime::class.java.getPackage().specificationVersion
            return if (specificationVersion != null && specificationVersion.startsWith("1.6"))
                DATE_PARSER_JDK_6
            else DATE_VALUE_READER_WRITER
        }

    private val VALUE_WRITERS = arrayOf<ValueWriter>(
        STRING_VALUE_READER_WRITER,
        NUMBER_VALUE_READER_WRITER,
        BOOLEAN_VALUE_READER_WRITER,
        platformSpecificDateConverter,
        NewMapValueWriter,
        NewArrayValueWriter
    )
}

internal object NewArrayValueWriter : ArrayValueWriter() {
    override fun canWrite(value: Any?): Boolean = isArrayish(value)

    override fun write(o: Any, context: WriterContext) {
        val values = normalize(o)

        context.write('[')
        context.writeArrayDelimiterPadding()

        var first = true
        var firstWriter: ValueWriter? = null

        for (value in values) {
            if (first) {
                firstWriter = Toml4jValueWriters.findWriterFor(value!!)
                first = false
            } else {
                val writer = Toml4jValueWriters.findWriterFor(value!!)
                if (writer !== firstWriter) {
                    throw IllegalStateException(
                        context.contextPath +
                            ": cannot write a heterogeneous array; first element was of type " + firstWriter +
                            " but found " + writer
                    )
                }
                context.write(", ")
            }

            val writer = Toml4jValueWriters.findWriterFor(value)
            val isNestedOldValue = NewMapValueWriter.isNested
            if (writer == NewMapValueWriter || writer == NewObjectValueWriter) {
                NewMapValueWriter.isNested = true
            }
            writer.write(value, context)
            if (writer == NewMapValueWriter || writer == NewObjectValueWriter) {
                NewMapValueWriter.isNested = isNestedOldValue
            }
        }

        context.writeArrayDelimiterPadding()
        context.write(']')
    }
}

internal object NewMapValueWriter : ValueWriter {

    override fun canWrite(value: Any): Boolean {
        return value is Map<*, *>
    }

    var isNested: Boolean = false

    override fun write(value: Any, context: WriterContext) {
        val from = value as Map<*, *>

        if (hasPrimitiveValues(from)) {
            if (isNested) {
                context.indent()
                context.write("{\n")
            } else {
                context.writeKey()
            }
        }

        // Render primitive types and arrays of primitive first so they are
        // grouped under the same table (if there is one)
        for ((key, value1) in from) {
            val fromValue = value1 ?: continue

            val valueWriter = Toml4jValueWriters.findWriterFor(fromValue)
            if (valueWriter.isPrimitiveType()) {
                context.indent()
                context.write(quoteKey(key!!)).write(" = ")
                valueWriter.write(fromValue, context)
                if (isNested) {
                    context.write(',')
                }
                context.write('\n')
            } else if (valueWriter === NewArrayValueWriter) {
                context.setArrayKey(key.toString())
                context.write(quoteKey(key!!)).write(" = ")
                valueWriter.write(fromValue, context)
                if (isNested) {
                    context.write(',')
                }
                context.write('\n')
            }
        }

        // Now render (sub)tables and arrays of tables
        for (key in from.keys) {
            val fromValue = from[key] ?: continue

            val valueWriter = Toml4jValueWriters.findWriterFor(fromValue)
            if (valueWriter === this || valueWriter === NewObjectValueWriter) {
                valueWriter.write(fromValue, context.pushTable(quoteKey(key!!)))
            }
        }
        if (isNested) {
            context.indent()
            context.write("}\n")
        }
    }

    override fun isPrimitiveType(): Boolean {
        return false
    }

    private val REQUIRED_QUOTING_PATTERN = Pattern.compile("^.*[^A-Za-z\\d_-].*$")

    private fun quoteKey(key: Any): String {
        var stringKey = key.toString()
        val matcher = REQUIRED_QUOTING_PATTERN.matcher(stringKey)
        if (matcher.matches()) {
            stringKey = "\"" + stringKey + "\""
        }

        return stringKey
    }

    private fun hasPrimitiveValues(values: Map<*, *>): Boolean {
        for (key in values.keys) {
            val fromValue = values[key] ?: continue

            val valueWriter = Toml4jValueWriters.findWriterFor(fromValue)
            if (valueWriter.isPrimitiveType() || valueWriter === NewArrayValueWriter) {
                return true
            }
        }

        return false
    }
}

internal object NewObjectValueWriter : ValueWriter {

    override fun canWrite(value: Any): Boolean {
        return true
    }

    override fun write(value: Any, context: WriterContext) {
        val to = LinkedHashMap<String, Any>()
        val fields = getFields(value.javaClass)
        for (field in fields) {
            to[field.name] = getFieldValue(field, value)!!
        }

        NewMapValueWriter.write(to, context)
    }

    override fun isPrimitiveType(): Boolean {
        return false
    }

    private fun getFields(cls: Class<*>): Set<Field> {
        var clazz = cls
        val fields = LinkedHashSet(Arrays.asList(*clazz.declaredFields))
        while (clazz != Any::class.java) {
            fields.addAll(Arrays.asList(*clazz.declaredFields))
            clazz = clazz.superclass
        }
        removeConstantsAndSyntheticFields(fields)

        return fields
    }

    private fun removeConstantsAndSyntheticFields(fields: MutableSet<Field>) {
        val iterator = fields.iterator()
        while (iterator.hasNext()) {
            val field = iterator.next()
            if (Modifier.isFinal(field.modifiers) && Modifier.isStatic(field.modifiers) || field.isSynthetic || Modifier.isTransient(field.modifiers)) {
                iterator.remove()
            }
        }
    }

    private fun getFieldValue(field: Field, o: Any): Any? {
        val isAccessible = field.isAccessible
        field.isAccessible = true
        var value: Any? = null
        try {
            value = field.get(o)
        } catch (ignored: IllegalAccessException) {
        }

        field.isAccessible = isAccessible

        return value
    }
}
