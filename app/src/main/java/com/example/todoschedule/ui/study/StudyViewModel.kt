package com.example.todoschedule.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

class StudyViewModel : ViewModel() {
    private val _studyStats = MutableStateFlow<List<StudyStat>>(emptyList())
    val studyStat: StateFlow<List<StudyStat>> = _studyStats.asStateFlow()

    private val _studyPlans = MutableStateFlow<List<StudyPlan>>(emptyList())
    val studyPlans: StateFlow<List<StudyPlan>> = _studyPlans.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _showStudyMenu = MutableStateFlow(false)
    val showStudyMenu: StateFlow<Boolean> = _showStudyMenu.asStateFlow()

    private val _selectedPlan = MutableStateFlow<StudyPlan?>(null)
    val selectedPlan: StateFlow<StudyPlan?> = _selectedPlan.asStateFlow()

    private val _selectedStat = MutableStateFlow<StudyStat?>(null)
    val selectedStat: StateFlow<StudyStat?> = _selectedStat.asStateFlow()

    private val _selectedStatRecords = MutableStateFlow<List<FocusSessionRecord>>(emptyList())
    val selectedStatRecords: StateFlow<List<FocusSessionRecord>> =
        _selectedStatRecords.asStateFlow()

    private val _showTimerSettingsModal = MutableStateFlow(false)
    val showTimerSettingsModal: StateFlow<Boolean> = _showTimerSettingsModal.asStateFlow()

    private val _showTaskSelectionModal = MutableStateFlow(false)
    val showTaskSelectionModal: StateFlow<Boolean> = _showTaskSelectionModal.asStateFlow()

    val availableTasks: StateFlow<List<Pair<String, String>>> =
        _studyPlans
            .map { planList ->
                planList
                    .map { studyPlan ->
                        studyPlan.title to "截止${studyPlan.timeDisplay}"
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _focusHistory = MutableStateFlow<List<FocusSessionRecord>>(emptyList())
    val focusHistory: StateFlow<List<FocusSessionRecord>> = _focusHistory.asStateFlow()

    private val _showFocusHistoryModal = MutableStateFlow(false)
    val showFocusHistoryModal: StateFlow<Boolean> = _showFocusHistoryModal.asStateFlow()

    private var timerJob: Job? = null

    private val _showAddEditPlanModal = MutableStateFlow(false)
    val showAddEditPlanModal: StateFlow<Boolean> = _showAddEditPlanModal.asStateFlow()

    private val _editingPlan = MutableStateFlow<StudyPlan?>(null)
    val editingPlan: StateFlow<StudyPlan?> = _editingPlan.asStateFlow()

    private val _showDeletePlanConfirmation = MutableStateFlow(false)
    val showDeletePlanConfirmation: StateFlow<Boolean> = _showDeletePlanConfirmation.asStateFlow()

    private val _planToDelete = MutableStateFlow<StudyPlan?>(null)
    val planToDelete: StateFlow<StudyPlan?> = _planToDelete

    init {
        loadInitialData()
        observeTimerStateChanges()
    }

    private fun loadInitialData() {
        _studyStats.value = listOf(
            StudyStat("今日专注", 0f, StatType.DAILY),
            StudyStat("本周专注", 0f, StatType.WEEKLY)
        )
        _timerState.update { currentState ->
            val initialTotalTime = calculateTotalTimeSeconds(currentState)
            currentState.copy(
                totalTimeSeconds = initialTotalTime,
                remainingTime = initialTotalTime,
                todayFocusMinutes = 0,
                currentSequenceId = currentState.currentSequenceId ?: generateNewSequenceId()
            )
        }
    }

    private fun observeTimerStateChanges() {
        viewModelScope.launch {
            _timerState.collect { state ->
                if (state.isRunning && state.remainingTime > 0) {
                    ensureTimerJobIsActive()
                } else {
                    timerJob?.cancel()
                }
            }
        }
    }

    private fun ensureTimerJobIsActive() {
        if (timerJob == null || !timerJob!!.isActive) {
            timerJob = viewModelScope.launch {
                while (_timerState.value.isRunning && _timerState.value.remainingTime > 0) {
                    delay(1000L)
                    onTick()
                }
            }
        }
    }

    fun onTick() {
        _timerState.update { currentState ->
            if (currentState.isRunning && currentState.remainingTime > 0) {
                val newRemainingTime = currentState.remainingTime - 1
                if (newRemainingTime == 0) {
                    handlePhaseCompletion(currentState)
                } else {
                    currentState.copy(remainingTime = newRemainingTime)
                }
            } else {
                currentState
            }
        }
    }

    private fun handlePhaseCompletion(completedState: TimerState): TimerState {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        var accumulatedFullMinutesThisPhase = 0

        if (completedState.isFocusPhase && completedState.currentSessionStartTime != null && completedState.currentSequenceId != null) {

            val addedFullMinutes = addFocusSessionRecord(
                startTime = completedState.currentSessionStartTime,
                endTime = now,
                taskName = completedState.selectedTask,
                status = FocusSessionRecord.SessionStatus.COMPLETED,
                sequenceId = completedState.currentSequenceId
            )
            if (addedFullMinutes != null) {
                accumulatedFullMinutesThisPhase = addedFullMinutes
            }
        }

        val nextCurrentRound: Int
        val nextIsFocusPhase: Boolean
        val nextIsLongBreak: Boolean

        if (completedState.isFocusPhase) {
            nextCurrentRound = completedState.currentRound + 1
            nextIsFocusPhase = false
            nextIsLongBreak =
                (completedState.currentRound % completedState.longBreakInterval == 0) && completedState.longBreakInterval > 0
        } else {
            nextCurrentRound = completedState.currentRound
            nextIsFocusPhase = true
            nextIsLongBreak = false
        }

        val newTotalTimeForNextPhase = calculateTotalTimeSeconds(
            isFocusPhase = nextIsFocusPhase,
            isLongBreak = nextIsLongBreak,
            timerState = completedState
        )

        var nextSessionStartTime: LocalDateTime? = null
        if (nextIsFocusPhase && completedState.autoStartNextRound) {
            nextSessionStartTime = now
        }

        return completedState.copy(
            isRunning = completedState.autoStartNextRound,
            isPaused = false,
            isInitial = !completedState.autoStartNextRound,
            remainingTime = newTotalTimeForNextPhase,
            totalTimeSeconds = newTotalTimeForNextPhase,
            currentRound = nextCurrentRound,
            isFocusPhase = nextIsFocusPhase,
            isLongBreak = nextIsLongBreak,
            currentSessionStartTime = nextSessionStartTime,
            todayFocusMinutes = completedState.todayFocusMinutes + accumulatedFullMinutesThisPhase
        )
    }

    fun onTimerAction(action: TimerControlAction) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

        _timerState.update { currentState ->
            var newCurrentState = currentState
            var accFullMinutesForTimerState = 0

            when (action) {
                TimerControlAction.START_INITIAL -> {
                    var newStartTime = currentState.currentSessionStartTime
                    var newSequenceId = currentState.currentSequenceId ?: generateNewSequenceId()
                    if (currentState.isFocusPhase && (currentState.isInitial || currentState.isPaused)) {
                        newStartTime = now
                    }
                    val timeForPhase =
                        if (currentState.isInitial || currentState.remainingTime == 0) {
                            calculateTotalTimeSeconds(currentState)
                        } else {
                            currentState.remainingTime
                        }
                    val totalTimeForPhase = calculateTotalTimeSeconds(currentState)
                    newCurrentState = currentState.copy(
                        isRunning = true, isInitial = false, isPaused = false,
                        remainingTime = timeForPhase, totalTimeSeconds = totalTimeForPhase,
                        currentSessionStartTime = newStartTime, currentSequenceId = newSequenceId
                    )
                }

                TimerControlAction.PAUSE -> {
                    newCurrentState = currentState.copy(isRunning = false, isPaused = true)
                }

                TimerControlAction.RESUME -> {
                    var newStartTime = currentState.currentSessionStartTime
                    var newSequenceId = currentState.currentSequenceId ?: generateNewSequenceId()
                    if (currentState.isFocusPhase && newStartTime == null && !currentState.isInitial) {
                        newStartTime = now
                    }
                    newCurrentState = currentState.copy(
                        isRunning = true, isPaused = false, isInitial = false,
                        currentSessionStartTime = newStartTime, currentSequenceId = newSequenceId
                    )
                }

                TimerControlAction.RESTART_COMPLETED -> {
                    var newStartTime = currentState.currentSessionStartTime
                    var newSequenceId = currentState.currentSequenceId ?: generateNewSequenceId()
                    if (currentState.isFocusPhase) {
                        newStartTime = now
                    }
                    val timeForPhase = calculateTotalTimeSeconds(currentState)
                    newCurrentState = currentState.copy(
                        remainingTime = timeForPhase, totalTimeSeconds = timeForPhase,
                        isRunning = true, isInitial = false, isPaused = false,
                        currentSessionStartTime = newStartTime, currentSequenceId = newSequenceId
                    )
                }

                TimerControlAction.RESET -> {
                    if (!currentState.isInitial && currentState.isFocusPhase &&
                        currentState.currentSessionStartTime != null && currentState.currentSequenceId != null
                    ) {
                        val addedFullMinutes = addFocusSessionRecord(
                            startTime = currentState.currentSessionStartTime,
                            endTime = now,
                            taskName = currentState.selectedTask,
                            status = FocusSessionRecord.SessionStatus.RESET,
                            sequenceId = currentState.currentSequenceId
                        )
                        if (addedFullMinutes != null) {
                            accFullMinutesForTimerState = addedFullMinutes
                        }
                    }
                    val initialFocusTime = currentState.focusDurationMinutes * 60
                    val newSeqId = generateNewSequenceId()
                    newCurrentState = currentState.copy(
                        currentRound = 1, isFocusPhase = true, isLongBreak = false,
                        totalTimeSeconds = initialFocusTime, isRunning = false, isPaused = false,
                        isInitial = true, remainingTime = initialFocusTime,
                        selectedTask = currentState.selectedTask,
                        currentSessionStartTime = null, currentSequenceId = newSeqId,
                        todayFocusMinutes = currentState.todayFocusMinutes + accFullMinutesForTimerState
                    )
                }
            }
            newCurrentState
        }
    }

    private fun calculateTotalTimeSeconds(
        isFocusPhase: Boolean,
        isLongBreak: Boolean,
        timerState: TimerState
    ): Int {
        return when {
            isFocusPhase -> timerState.focusDurationMinutes * 60
            isLongBreak -> timerState.longBreakMinutes * 60
            else -> timerState.shortBreakMinutes * 60
        }
    }

    private fun calculateTotalTimeSeconds(state: TimerState): Int {
        return calculateTotalTimeSeconds(state.isFocusPhase, state.isLongBreak, state)
    }

    private fun generateNewSequenceId(): String = UUID.randomUUID().toString()

    private fun addFocusSessionRecord(
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        taskName: String?,
        status: FocusSessionRecord.SessionStatus,
        sequenceId: String
    ): Int? {
        val durationMillis = endTime.toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds() - startTime.toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        val actualDurationInSeconds = (durationMillis / 1000).toInt()
        val fullMinutesCompleted = (durationMillis / (1000 * 60)).toInt()

        if (actualDurationInSeconds <= 0 && status != FocusSessionRecord.SessionStatus.RESET) {
            return null
        }

        val newRecord = FocusSessionRecord(
            startTime = startTime,
            endTime = endTime,
            durationSeconds = actualDurationInSeconds.coerceAtLeast(0),
            taskName = taskName,
            sessionStatus = status,
            roundId = sequenceId,
            isFocusPhase = true
        )

        var historyUpdated = false
        _focusHistory.update {
            val newList = (it + newRecord).sortedByDescending { record -> record.startTime }
            if (newList.size != it.size || newList != it) {
                historyUpdated = true
            }
            newList
        }

        if (historyUpdated && _selectedStat.value != null) {
            _selectedStatRecords.value =
                filterRecordsForStat(_selectedStat.value, _focusHistory.value)
        }

        updateStudyStats()

        return if (fullMinutesCompleted > 0) fullMinutesCompleted else 0
    }

    private fun updateStudyStats() {
        _studyStats.update { currentStats ->
            val nowDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val currentDate = nowDateTime.date

            val todayTotalSeconds = _focusHistory.value
                .filter { record ->
                    record.isFocusPhase && record.startTime.date == currentDate
                }
                .sumOf { record ->
                    record.durationSeconds.coerceAtLeast(0)
                }

            val dayOfWeekIso = currentDate.dayOfWeek.isoDayNumber
            val daysToSubtractForMonday = dayOfWeekIso - 1
            val startOfWeekDate = currentDate.minus(daysToSubtractForMonday, DateTimeUnit.DAY)
            val daysToAddForSunday = 7 - dayOfWeekIso
            val endOfWeekDate = currentDate.plus(daysToAddForSunday, DateTimeUnit.DAY)

            val weeklyTotalSeconds = _focusHistory.value
                .filter { record ->
                    record.isFocusPhase &&
                            record.startTime.date >= startOfWeekDate &&
                            record.startTime.date <= endOfWeekDate
                }
                .sumOf { record ->
                    record.durationSeconds.coerceAtLeast(0)
                }

            currentStats.map { stat ->
                when {
                    stat.type == StatType.DAILY && stat.title.contains("今日专注") -> {
                        stat.copy(value = todayTotalSeconds / 3600f)
                    }

                    stat.type == StatType.WEEKLY && stat.title.contains("本周专注") -> {
                        stat.copy(value = weeklyTotalSeconds / 3600f)
                    }

                    else -> stat
                }
            }
        }
    }

    fun toggleStudyMenu(show: Boolean? = null) {
        _showStudyMenu.update { show ?: !it }
    }

    fun selectPlan(plan: StudyPlan?) {
        _selectedPlan.value = plan
    }

    fun selectStat(stat: StudyStat?) {
        _selectedStat.value = stat

        _selectedStatRecords.value = filterRecordsForStat(stat, _focusHistory.value)
    }

    private fun filterRecordsForStat(
        stat: StudyStat?,
        history: List<FocusSessionRecord>
    ): List<FocusSessionRecord> {
        if (stat == null) {
            return emptyList()
        }

        val nowDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDate = nowDateTime.date

        return when (stat.type) {
            StatType.DAILY -> {
                history.filter { record ->
                    record.isFocusPhase && record.startTime.date == currentDate
                }.sortedByDescending { it.startTime }
            }

            StatType.WEEKLY -> {
                val dayOfWeekIso = currentDate.dayOfWeek.isoDayNumber
                val startOfWeekDate = currentDate.minus(dayOfWeekIso - 1, DateTimeUnit.DAY)
                val endOfWeekDate = currentDate.plus(7 - dayOfWeekIso, DateTimeUnit.DAY)
                history.filter { record ->
                    record.isFocusPhase &&
                            record.startTime.date >= startOfWeekDate &&
                            record.startTime.date <= endOfWeekDate
                }.sortedByDescending { it.startTime }
            }
        }
    }

    fun showTimerSettingsModal(show: Boolean) {
        _showTimerSettingsModal.value = show
        if (show) _showStudyMenu.value = false
    }

    fun showTaskSelectionModal(show: Boolean) {
        _showTaskSelectionModal.value = show
    }

    fun saveTimerSettings(newSettings: TimerState) {
        _timerState.update { currentState ->
            val updatedState = currentState.copy(
                focusDurationMinutes = newSettings.focusDurationMinutes,
                shortBreakMinutes = newSettings.shortBreakMinutes,
                longBreakMinutes = newSettings.longBreakMinutes,
                longBreakInterval = newSettings.longBreakInterval,
                autoStartNextRound = newSettings.autoStartNextRound
            )
            if (currentState.isInitial) {
                val newTotalTime = calculateTotalTimeSeconds(updatedState)
                updatedState.copy(
                    totalTimeSeconds = newTotalTime,
                    remainingTime = newTotalTime
                )
            } else {
                updatedState
            }
        }
        showTimerSettingsModal(false)
    }

    fun selectTaskForTimer(taskTitle: String) {
        _timerState.update { it.copy(selectedTask = taskTitle) }
        showTaskSelectionModal(false)
    }

    fun startStudyFromPlan(plan: StudyPlan) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        selectPlan(null)
        var statsNeedUpdate = false

        _timerState.update { currentState ->
            var accumulatedMinutesThisAction = 0
            if (!currentState.isInitial && currentState.isFocusPhase && currentState.currentSessionStartTime != null && currentState.currentSequenceId != null) {
                val addedDuration = addFocusSessionRecord(
                    startTime = currentState.currentSessionStartTime,
                    endTime = now,
                    taskName = currentState.selectedTask,
                    status = FocusSessionRecord.SessionStatus.INTERRUPTED,
                    sequenceId = currentState.currentSequenceId
                )
                if (addedDuration != null) {
                    accumulatedMinutesThisAction = addedDuration
                    if (addedDuration > 0) {
                        statsNeedUpdate = true
                    }
                }
            }

            val focusTime = currentState.focusDurationMinutes * 60
            val newSequenceId = generateNewSequenceId()

            currentState.copy(
                selectedTask = plan.title,
                isFocusPhase = true,
                isLongBreak = false,
                currentRound = 1,
                totalTimeSeconds = focusTime,
                remainingTime = focusTime,
                isRunning = true,
                isInitial = false,
                isPaused = false,
                currentSessionStartTime = now,
                currentSequenceId = newSequenceId,
                todayFocusMinutes = currentState.todayFocusMinutes + accumulatedMinutesThisAction
            )
        }
        if (statsNeedUpdate) {
            updateStudyStats()
        }
    }

    fun onAddStudyPlanClicked() {
        _editingPlan.value = null
        _selectedPlan.value = null
        _showAddEditPlanModal.value = true
    }

    fun onAddNewTaskClickedFromStudy() {
        onAddStudyPlanClicked()
        showTaskSelectionModal(false)
    }

    fun onEditPlanClicked(plan: StudyPlan) {
        _editingPlan.value = plan
        _selectedPlan.value = null
        _showAddEditPlanModal.value = true
    }

    fun onSaveStudyPlan(
        title: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        description: String?,
        subject: String,
        location: String?
    ) {
        _studyPlans.update { currentPlans ->
            val planToSave = _editingPlan.value
            val updatedPlans = if (planToSave != null) {
                currentPlans.map {
                    if (it.id == planToSave.id) {
                        val updatedPlan = planToSave.copy(
                            title = title,
                            startTime = startTime,
                            endTime = endTime,
                            description = description,
                            subject = subject,
                            location = location
                        )
                        updatedPlan
                    } else {
                        it
                    }
                }
            } else {
                val newId = (currentPlans.maxOfOrNull { it.id } ?: 0) + 1
                val newPlan = StudyPlan(
                    id = newId,
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    description = description,
                    subject = subject,
                    location = location
                )
                currentPlans + newPlan
            }
            updatedPlans.sortedBy { it.startTime }
        }
        _showAddEditPlanModal.value = false
        _editingPlan.value = null
    }

    fun onDismissAddEditPlanModal() {
        _showAddEditPlanModal.value = false
        _editingPlan.value = null
    }

    fun onDeletePlanClicked(plan: StudyPlan) {
        _planToDelete.value = plan
        _selectedPlan.value = null
        _showDeletePlanConfirmation.value = true
    }

    fun confirmDeletePlan() {
        _planToDelete.value?.let { planToRemove ->
            _studyPlans.update { currentPlans ->
                currentPlans.filterNot { it.id == planToRemove.id }
            }
        }
        _showDeletePlanConfirmation.value = false
        _planToDelete.value = null
    }

    fun onDismissDeletePlanConfirmation() {
        _showDeletePlanConfirmation.value = false
        _planToDelete.value = null
    }

    fun showFocusHistoryModal(show: Boolean) {
        _showFocusHistoryModal.value = show
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}