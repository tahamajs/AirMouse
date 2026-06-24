
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus

interface ICalibrationDataSource {

    
    
    

    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun getCalibrationData(): CalibrationData

    
    
    

    suspend fun saveGyroBias(x: Float, y: Float, z: Float)
    suspend fun getGyroBias(): Triple<Float, Float, Float>
    suspend fun saveGyroVariance(x: Float, y: Float, z: Float)
    suspend fun getGyroVariance(): Triple<Float, Float, Float>
    suspend fun saveGyroSampleCount(count: Int)
    suspend fun getGyroSampleCount(): Int

    
    
    

    suspend fun saveAccelOffset(x: Float, y: Float, z: Float)
    suspend fun getAccelOffset(): Triple<Float, Float, Float>
    suspend fun saveAccelScale(x: Float, y: Float, z: Float)
    suspend fun getAccelScale(): Triple<Float, Float, Float>
    suspend fun saveAccelPosition(position: Int, values: Triple<Float, Float, Float>)
    suspend fun getAccelPosition(position: Int): Triple<Float, Float, Float>?
    suspend fun getAllAccelPositions(): Map<Int, Triple<Float, Float, Float>>
    suspend fun saveAccelPositionsCompleted(count: Int)
    suspend fun getAccelPositionsCompleted(): Int

    
    
    

    suspend fun saveMagOffset(x: Float, y: Float, z: Float)
    suspend fun getMagOffset(): Triple<Float, Float, Float>
    suspend fun saveMagScale(x: Float, y: Float, z: Float)
    suspend fun getMagScale(): Triple<Float, Float, Float>
    suspend fun saveMagSampleCount(count: Int)
    suspend fun getMagSampleCount(): Int

    
    
    

    suspend fun setCalibrationStatus(status: CalibrationStatus)
    suspend fun getCalibrationStatus(): CalibrationStatus
    suspend fun setCalibrationQuality(quality: CalibrationQuality)
    suspend fun getCalibrationQuality(): CalibrationQuality
    suspend fun setCalibrationProgress(progress: Int)
    suspend fun getCalibrationProgress(): Int
    suspend fun setCurrentStep(step: Int)
    suspend fun getCurrentStep(): Int
    suspend fun setCalibrationComplete(complete: Boolean)
    suspend fun isCalibrationComplete(): Boolean
    suspend fun setCalibrationTimestamp(timestamp: Long)
    suspend fun getCalibrationTimestamp(): Long

    
    
    

    suspend fun hasCalibrationData(): Boolean
    suspend fun resetAll()
    suspend fun resetGyro()
    suspend fun resetAccel()
    suspend fun resetMag()
    suspend fun getCalibrationSummary(): Map<String, Any>
}

package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData

interface ICalibrationDataSource {
    suspend fun getCalibrationData(): CalibrationData?
    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun clearCalibrationData()
}