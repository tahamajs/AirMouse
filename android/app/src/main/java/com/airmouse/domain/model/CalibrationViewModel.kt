@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationRepo: ICalibrationRepository,
    private val connectionRepo: IConnectionRepository
) : ViewModel() {

    fun saveGyroCalibration(offsets: Triple<Float, Float, Float>) {
        viewModelScope.launch {
            calibrationRepo.saveGyroBias(GyroBias(offsets.first, offsets.second, offsets.third))
            calibrationRepo.markCalibrationComplete()
        }
    }

    fun connectToServer(ip: String, port: Int) {
        viewModelScope.launch {
            connectionRepo.connect(ConnectionConfig(ip, port))
        }
    }

    fun sendClick() {
        viewModelScope.launch {
            connectionRepo.sendEvent(MouseEvent.Click("left"))
        }
    }
}