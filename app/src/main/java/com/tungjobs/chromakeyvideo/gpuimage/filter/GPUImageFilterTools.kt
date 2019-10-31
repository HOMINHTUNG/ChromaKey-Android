/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tungjobs.chromakeyvideo.gpuimage.filter

import android.app.AlertDialog
import android.content.Context
import android.graphics.BitmapFactory
import com.tungjobs.chromakeyvideo.R
import jp.co.cyberagent.android.gpuimage.filter.*
import java.util.*

object GPUImageFilterTools {
    fun showDialog(
        context: Context,
        listener: (filter: GPUImageFilter) -> Unit
    ) {
        val filters = FilterList().apply {
            addFilter("GPUImageChromaKeyBlendFilter",
                FilterType.BLEN_GPU_CHROMA_KEY
            )
            addFilter("ChromaKeyFilter",
                FilterType.BLEND_CHROMA_KEY
            )
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Choose a filter")
        builder.setItems(filters.names.toTypedArray()) { _, item ->
            listener(
                createFilterForType(
                    context,
                    filters.filters[item]
                )
            )
        }
        builder.create().show()
    }

    private fun createFilterForType(context: Context, type: FilterType): GPUImageFilter {
        return when (type) {
            FilterType.BLEND_CHROMA_KEY -> ChromaKeyFilter()
            FilterType.FILTER_GROUP -> GPUImageFilterGroup(
                listOf(
                    GPUImageContrastFilter(),
                    GPUImageDirectionalSobelEdgeDetectionFilter(),
                    GPUImageGrayscaleFilter()
                )
            )
            FilterType.BLEN_GPU_CHROMA_KEY -> createBlendFilter(
                context,
                GPUImageChromaKeyBlendFilter::class.java
            )
        }
    }

    private fun createBlendFilter(
        context: Context,
        filterClass: Class<out GPUImageTwoInputFilter>
    ): GPUImageFilter {
        return try {
            filterClass.newInstance().apply {
                bitmap = BitmapFactory.decodeResource(context.resources,
                    R.drawable.ic_launcher_background
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            GPUImageFilter()
        }
    }

    private enum class FilterType {
        BLEND_CHROMA_KEY,FILTER_GROUP,BLEN_GPU_CHROMA_KEY
    }

    private class FilterList {
        val names: MutableList<String> = LinkedList()
        val filters: MutableList<FilterType> = LinkedList()

        fun addFilter(name: String, filter: FilterType) {
            names.add(name)
            filters.add(filter)
        }
    }

    class FilterAdjuster(filter: GPUImageFilter) {
        private val adjuster: Adjuster<out GPUImageFilter>?

        init {
            adjuster = when (filter) {
                is GPUImageChromaKeyBlendFilter -> ChromaKeyBlur(filter)
                is ChromaKeyFilter -> ChromaKeyBlurCustom(filter)
                else -> null
            }
        }

        fun canAdjust(): Boolean {
            return adjuster != null
        }

        fun adjust(percentage: Int) {
            adjuster?.adjust(percentage)
        }

        private abstract inner class Adjuster<T : GPUImageFilter>(protected val filter: T) {

            abstract fun adjust(percentage: Int)

            protected fun range(percentage: Int, start: Float, end: Float): Float {
                return (end - start) * percentage / 100.0f + start
            }

            protected fun range(percentage: Int, start: Int, end: Int): Int {
                return (end - start) * percentage / 100 + start
            }
        }

        private inner class ChromaKeyBlurCustom(filter: ChromaKeyFilter) :
            Adjuster<ChromaKeyFilter>(filter) {
            override fun adjust(percentage: Int) {
                filter.setThresholdSensitivity(range(percentage, 0.0f, 3.0f))
            }
        }

        private inner class ChromaKeyBlur(filter: GPUImageChromaKeyBlendFilter) :
            Adjuster<GPUImageChromaKeyBlendFilter>(filter) {
            override fun adjust(percentage: Int) {
//                filter.setSmoothing(range(percentage, 0.0f, 1.0f))
            }
        }
    }
}
