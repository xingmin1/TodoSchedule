package com.example.todoschedule.domain.use_case.settings

import com.example.todoschedule.domain.model.Table
import kotlinx.datetime.*
import javax.inject.Inject

/**
 * Use case to validate if a list of selected tables have overlapping date ranges.
 */
class ValidateDefaultTableOverlapUseCase @Inject constructor() {

    data class ValidationResult(
        val isValid: Boolean,
        val conflictingTables: List<Pair<Table, Table>>? = null // List of pairs that conflict
    )

    operator fun invoke(selectedTables: List<Table>): ValidationResult {
        if (selectedTables.size <= 1) {
            return ValidationResult(isValid = true)
        }

        // Sort tables by start date
        val sortedTables = selectedTables.sortedBy { it.startDate }

        val conflictingPairs = mutableListOf<Pair<Table, Table>>()

        for (i in 0 until sortedTables.size - 1) {
            val currentTable = sortedTables[i]
            val nextTable = sortedTables[i + 1]

            val currentEndDate = calculateEndDate(currentTable.startDate, currentTable.totalWeeks)

            // Check for overlap: If the next table's start date is before or the same as the current table's end date
            if (nextTable.startDate <= currentEndDate) {
                conflictingPairs.add(Pair(currentTable, nextTable))
            }
        }

        return if (conflictingPairs.isEmpty()) {
            ValidationResult(isValid = true)
        } else {
            ValidationResult(isValid = false, conflictingTables = conflictingPairs)
        }
    }

    /**
     * Calculates the end date of a table's term.
     * The term includes the start date's week and lasts for totalWeeks.
     * Example: Start Mon, 1 week -> Ends Sun of that week.
     * Start Mon, 2 weeks -> Ends Sun of the next week.
     */
    private fun calculateEndDate(startDate: LocalDate, totalWeeks: Int): LocalDate {
        // Go to the end of the starting week (Sunday)
        val startDayOfWeek = startDate.dayOfWeek // MONDAY(1) to SUNDAY(7)
        val daysToEndOfWeek = DayOfWeek.SUNDAY.isoDayNumber - startDayOfWeek.isoDayNumber
        val endOfWeekStartDate = startDate.plus(daysToEndOfWeek, DateTimeUnit.DAY)

        // Add the remaining weeks (totalWeeks - 1 because the first week is already included)
        // And subtract one day because the end date is inclusive (Sunday)
        if (totalWeeks <= 1) {
            return endOfWeekStartDate
        }

        return endOfWeekStartDate.plus(totalWeeks - 1, DateTimeUnit.WEEK)

    }
} 