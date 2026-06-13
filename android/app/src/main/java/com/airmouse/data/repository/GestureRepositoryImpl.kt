// app/src/main/java/com/airmouse/data/repository/GestureRepositoryImpl.kt
package com.airmouse.data.repository

import android.content.Context
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.repository.IGestureRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class GestureRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IGestureRepository {

    private val gesturesDir = File(context.filesDir, "custom_gestures")
    private val _gesturesFlow = MutableStateFlow<List<CustomGestureTemplate>>(emptyList())
    private var trainedClassifier: Map<String, FloatArray> = emptyMap() // gestureId -> mean feature vector

    init {
        gesturesDir.mkdirs()
        loadAllGestures()
        trainAllGestures()
    }

    private fun loadAllGestures() {
        val files = gesturesDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        val gestures = files.mapNotNull { file ->
            try {
                parseGesture(file.readText())
            } catch (e: Exception) { null }
        }
        _gesturesFlow.value = gestures
    }

    private fun parseGesture(jsonStr: String): CustomGestureTemplate {
        val json = JSONObject(jsonStr)
        val samplesArray = json.getJSONArray("samples")
        val samples = mutableListOf<FloatArray>()
        for (i in 0 until samplesArray.length()) {
            val arr = samplesArray.getJSONArray(i)
            samples.add(FloatArray(arr.length()) { j -> arr.getDouble(j).toFloat() })
        }
        val featuresArray = json.optJSONArray("features")
        val features = if (featuresArray != null) {
            FloatArray(featuresArray.length()) { featuresArray.getDouble(it).toFloat() }
        } else floatArrayOf()

        return CustomGestureTemplate(
            id = json.getString("id"),
            name = json.getString("name"),
            threshold = json.optDouble("threshold", 10.0).toFloat(),
            samples = samples,
            features = features,
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            modifiedAt = json.optLong("modifiedAt", System.currentTimeMillis()),
            isTrained = json.optBoolean("isTrained", false),
            confidence = json.optDouble("confidence", 0.0).toFloat()
        )
    }

    private fun gestureToJson(gesture: CustomGestureTemplate): String {
        val samplesArray = JSONArray().apply {
            gesture.samples.forEach { sample ->
                put(JSONArray(sample.toList()))
            }
        }
        val featuresArray = JSONArray().apply {
            gesture.features.forEach { put(it) }
        }
        return JSONObject().apply {
            put("id", gesture.id)
            put("name", gesture.name)
            put("threshold", gesture.threshold)
            put("samples", samplesArray)
            put("features", featuresArray)
            put("createdAt", gesture.createdAt)
            put("modifiedAt", gesture.modifiedAt)
            put("isTrained", gesture.isTrained)
            put("confidence", gesture.confidence)
        }.toString()
    }

    private fun saveGestureToFile(gesture: CustomGestureTemplate) {
        val file = File(gesturesDir, "${gesture.id}.json")
        file.writeText(gestureToJson(gesture))
    }

    override suspend fun getAllCustomGestures(): List<CustomGestureTemplate> = _gesturesFlow.value

    override suspend fun getCustomGesture(id: String): CustomGestureTemplate? = _gesturesFlow.value.find { it.id == id }

    override suspend fun saveCustomGesture(gesture: CustomGestureTemplate) {
        val updatedGesture = gesture.copy(modifiedAt = System.currentTimeMillis())
        saveGestureToFile(updatedGesture)
        val newList = _gesturesFlow.value.toMutableList().apply {
            val idx = indexOfFirst { it.id == gesture.id }
            if (idx >= 0) set(idx, updatedGesture) else add(updatedGesture)
        }
        _gesturesFlow.value = newList
        // Automatically retrain all gestures (or we could only train this one)
        trainAllGestures()
    }

    override suspend fun deleteCustomGesture(id: String) {
        File(gesturesDir, "$id.json").delete()
        _gesturesFlow.value = _gesturesFlow.value.filter { it.id != id }
        trainAllGestures()
    }

    /**
     * Extracts feature vector from raw sensor samples.
     * Features: mean, std, max, min, zero-crossing rate for each of the 6 axes (gyroX,Y,Z, accelX,Y,Z)
     * → 6 axes * 4 = 24 features + total magnitude mean/std = 26 features.
     */
    private fun extractFeatures(samples: List<FloatArray>): FloatArray {
        if (samples.isEmpty()) return FloatArray(26)
        val numAxes = 6
        val features = mutableListOf<Float>()
        for (axis in 0 until numAxes) {
            val values = samples.map { it[axis] }
            val mean = values.average().toFloat()
            val std = sqrt(values.map { (it - mean) * (it - mean) }.average()).toFloat()
            val maxVal = values.maxOrNull() ?: 0f
            val minVal = values.minOrNull() ?: 0f
            features.add(mean)
            features.add(std)
            features.add(maxVal)
            features.add(minVal)
        }
        // Magnitude mean and std
        val magnitudes = samples.map { sample ->
            sqrt(sample[0]*sample[0] + sample[1]*sample[1] + sample[2]*sample[2])
        }
        val magMean = magnitudes.average().toFloat()
        val magStd = sqrt(magnitudes.map { (it - magMean) * (it - magMean) }.average()).toFloat()
        features.add(magMean)
        features.add(magStd)
        return features.toFloatArray()
    }

    private suspend fun trainAllGestures() = withContext(Dispatchers.IO) {
        val gestures = _gesturesFlow.value
        val updatedGestures = mutableListOf<CustomGestureTemplate>()
        for (g in gestures) {
            val features = if (g.samples.isNotEmpty()) extractFeatures(g.samples) else g.features
            val updated = g.copy(features = features, isTrained = true, confidence = 1f)
            updatedGestures.add(updated)
            saveGestureToFile(updated)
        }
        // Build classifier map: gestureId -> feature vector
        trainedClassifier = updatedGestures.associate { it.id to it.features }
        _gesturesFlow.value = updatedGestures
    }

    override suspend fun trainGesture(gestureId: String, newSamples: List<FloatArray>?): Boolean = withContext(Dispatchers.IO) {
        val gesture = getCustomGesture(gestureId) ?: return@withContext false
        val samplesToUse = newSamples ?: gesture.samples
        if (samplesToUse.isEmpty()) return@withContext false
        val features = extractFeatures(samplesToUse)
        val updated = gesture.copy(
            features = features,
            samples = if (newSamples != null) samplesToUse else gesture.samples,
            isTrained = true,
            confidence = 1f,
            modifiedAt = System.currentTimeMillis()
        )
        saveCustomGesture(updated)
        // Update classifier
        trainedClassifier = trainedClassifier.toMutableMap().apply { this[gestureId] = features }
        true
    }

    override suspend fun recognizeGesture(samples: List<FloatArray>): Pair<String?, Float> = withContext(Dispatchers.Default) {
        if (samples.isEmpty() || trainedClassifier.isEmpty()) return@withContext null to 0f
        val features = extractFeatures(samples)
        var bestId: String? = null
        var bestDistance = Float.MAX_VALUE
        for ((id, refFeatures) in trainedClassifier) {
            val distance = euclideanDistance(features, refFeatures)
            if (distance < bestDistance) {
                bestDistance = distance
                bestId = id
            }
        }
        val gesture = bestId?.let { getCustomGesture(it) }
        val confidence = if (gesture != null) (1f / (1f + bestDistance)).coerceIn(0f, 1f) else 0f
        gesture?.name to confidence
    }

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        val minLen = min(a.size, b.size)
        var sum = 0f
        for (i in 0 until minLen) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    override suspend fun exportGesturesToCSV(): String = withContext(Dispatchers.IO) {
        val exportFile = File(context.getExternalFilesDir(null), "gesture_dataset_${System.currentTimeMillis()}.csv")
        FileWriter(exportFile).use { writer ->
            writer.write("gesture_id,gesture_name,timestamp_ms,gyro_x,gyro_y,gyro_z,accel_x,accel_y,accel_z\n")
            val gestures = _gesturesFlow.value
            for (gesture in gestures) {
                for (sample in gesture.samples) {
                    writer.write("${gesture.id},${gesture.name},${System.currentTimeMillis()},${sample[0]},${sample[1]},${sample[2]},${sample[3]},${sample[4]},${sample[5]}\n")
                }
            }
        }
        exportFile.absolutePath
    }

    override suspend fun importGesturesFromCSV(filePath: String): Boolean = withContext(Dispatchers.IO) {
        // Implementation would parse CSV and create gestures; omitted for brevity but can be added.
        false
    }

    override fun observeGestures(): Flow<List<CustomGestureTemplate>> = _gesturesFlow.asStateFlow()
}