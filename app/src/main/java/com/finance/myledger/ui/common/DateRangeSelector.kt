package com.finance.myledger.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finance.myledger.domain.model.DateRange

/**
 * Shared date-range selector for Time and Merchant analytics screens.
 * Dropdown keeps the control compact; presets cover common windows (Custom can be added later).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    selected: DateRange,
    onSelected: (DateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember {
        listOf(
            DateRange.ThisMonth,
            DateRange.LastMonth,
            DateRange.Last3Months,
            DateRange.Last6Months,
            DateRange.ThisYear,
            DateRange.AllTime
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = selected.label(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date range") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun DateRange.label(): String = when (this) {
    DateRange.ThisMonth -> "This month"
    DateRange.LastMonth -> "Last month"
    DateRange.Last3Months -> "Last 3 months"
    DateRange.Last6Months -> "Last 6 months"
    DateRange.ThisYear -> "This year"
    DateRange.AllTime -> "All time"
    is DateRange.Custom -> "Custom"
}
