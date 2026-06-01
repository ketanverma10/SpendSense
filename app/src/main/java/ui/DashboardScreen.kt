package ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.CategorySum
import data.DailySum
import data.MerchantSum
import utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
) {
    val totalSpent by viewModel.totalSpent.collectAsState()
    val txnCount by viewModel.transactionCount.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val topMerchant by viewModel.topMerchant.collectAsState()
    val dailySpending by viewModel.dailySpending.collectAsState()
    val year by viewModel.selectedYear.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val week by viewModel.selectedWeek.collectAsState()
    val drillDownCategory by viewModel.drillDownCategory.collectAsState()

    Scaffold(
        containerColor = Color(0xFF000000), // Pure Black for high-end look
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("SpendSense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Financial Intelligence", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings or Sync */ }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Sync", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                AnalyticsFilters(
                    selectedYear = year,
                    selectedMonth = month,
                    selectedWeek = week,
                    onYearSelect = { viewModel.setYear(it) },
                    onMonthSelect = { viewModel.setMonth(it) },
                    onWeekSelect = { viewModel.setWeek(it) }
                )
            }

            item {
                SummarySection(
                    totalSpent = totalSpent ?: 0.0,
                    txnCount = txnCount,
                    topCategory = if (drillDownCategory == null) (categoryBreakdown.firstOrNull()?.category ?: "N/A") else drillDownCategory!!,
                    topMerchant = topMerchant?.merchant ?: "N/A"
                )
            }

            item {
                ChartSection(
                    drillDownCategory = drillDownCategory,
                    categoryBreakdown = categoryBreakdown,
                    dailySpending = dailySpending,
                    onCategoryClick = { viewModel.drillDown(it) },
                    onBackClick = { viewModel.drillDown(null) }
                )
            }

            if (categoryBreakdown.isNotEmpty()) {
                item {
                    Text(
                        if (drillDownCategory == null) "Segment Analysis" else "Sub-category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                items(categoryBreakdown) { item ->
                    CategoryRow(
                        categorySum = item,
                        total = totalSpent ?: 1.0,
                        onClick = { if (drillDownCategory == null) viewModel.drillDown(item.category) }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AnalyticsFilters(
    selectedYear: Int,
    selectedMonth: Int,
    selectedWeek: Int?,
    onYearSelect: (Int) -> Unit,
    onMonthSelect: (Int) -> Unit,
    onWeekSelect: (Int?) -> Unit
) {
    val monthNames = remember {
        val sdf = SimpleDateFormat("MMMM", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        (0..11).map { m ->
            cal.set(Calendar.MONTH, m)
            sdf.format(cal.time)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DropdownFilter(
                label = "Year",
                selected = selectedYear.toString(),
                options = (2024..2027).map { it.toString() },
                onSelect = { onYearSelect(it.toInt()) },
                modifier = Modifier.weight(1f)
            )
            
            DropdownFilter(
                label = "Month",
                selected = monthNames[selectedMonth],
                options = monthNames,
                onSelect = { monthName ->
                    val index = monthNames.indexOf(monthName)
                    if (index != -1) onMonthSelect(index)
                },
                modifier = Modifier.weight(1.5f)
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedWeek == null,
                onClick = { onWeekSelect(null) },
                label = { Text("Full Month") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1A1A1A),
                    labelColor = Color.Gray
                ),
                border = null,
                shape = RoundedCornerShape(12.dp)
            )
            (1..5).forEach { w ->
                FilterChip(
                    selected = selectedWeek == w,
                    onClick = { onWeekSelect(w) },
                    label = { Text("Week $w") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1A1A1A),
                        labelColor = Color.Gray
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Color.Gray) },
            trailingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFF333333),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = Color(0xFF111111),
                focusedContainerColor = Color(0xFF111111)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1A1A1A))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SummarySection(
    totalSpent: Double,
    txnCount: Int,
    topCategory: String,
    topMerchant: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF6200EE), Color(0xFF3700B3))
                        )
                    )
                    .padding(28.dp)
            ) {
                Column {
                    Text("TOTAL BALANCE OUTFLOW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                    Text(
                        CurrencyFormatter.format(totalSpent),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = CircleShape
                        ) {
                            Text(
                                "$txnCount TRANSACTIONS",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Avg: ${CurrencyFormatter.format(if (txnCount > 0) totalSpent / txnCount else 0.0)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard(
                title = "TOP SEGMENT",
                value = topCategory,
                gradient = Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0F0F0F))),
                accentColor = Color(0xFF03DAC6),
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                title = "MAIN ENTITY",
                value = topMerchant,
                gradient = Brush.verticalGradient(listOf(Color(0xFF1A1A1A), Color(0xFF0F0F0F))),
                accentColor = Color(0xFFFF0266),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun InfoCard(title: String, value: String, gradient: Brush, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Box(modifier = Modifier.background(gradient).padding(16.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.size(28.dp, 3.dp).background(accentColor, CircleShape))
            }
        }
    }
}

@Composable
fun ChartSection(
    drillDownCategory: String?,
    categoryBreakdown: List<CategorySum>,
    dailySpending: List<DailySum>,
    onCategoryClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (drillDownCategory != null) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    if (drillDownCategory != null) "Drill-down Analysis" else if (categoryBreakdown.isNotEmpty()) "Capital Distribution" else "Daily Flow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(Modifier.height(32.dp))

            if (categoryBreakdown.isNotEmpty()) {
                DonutChart(data = categoryBreakdown, onCategoryClick = onCategoryClick)
            } else if (dailySpending.isNotEmpty()) {
                DailySpendingChart(data = dailySpending)
            } else {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("Insufficient Analytical Data", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
            }
        }
    }
}

@Composable
fun DonutChart(data: List<CategorySum>, onCategoryClick: (String) -> Unit) {
    val total = data.sumOf { it.total }
    val colors = listOf(
        Color(0xFFBB86FC), Color(0xFF03DAC6), Color(0xFF3700B3), 
        Color(0xFFFF0266), Color(0xFF00E5FF), Color(0xFFFFD600)
    )

    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1.1f), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                var startAngle = -90f
                data.forEachIndexed { index, item ->
                    val sweep = (item.total / total).toFloat() * 360f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = 38f, cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${data.size}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("GROUPS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }

        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            data.take(5).forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCategoryClick(item.category) }
                ) {
                    Box(Modifier.size(8.dp).background(colors[index % colors.size], CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(item.category, style = MaterialTheme.typography.labelMedium, color = Color.LightGray, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Text("${((item.total / total) * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DailySpendingChart(data: List<DailySum>) {
    val max = data.maxOfOrNull { it.total } ?: 1.0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            val heightPercent = (item.total / max).toFloat().coerceIn(0.05f, 1f)
            val animatedHeight by animateFloatAsState(
                targetValue = heightPercent, 
                animationSpec = tween(1500, easing = FastOutSlowInEasing),
                label = "barHeight"
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .fillMaxHeight(0.85f)
                        .weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedHeight)
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFBB86FC), Color(0xFF6200EE).copy(alpha = 0.1f))
                                )
                            )
                    )
                }
                
                Text(
                    text = SimpleDateFormat("dd", Locale.getDefault()).format(Date(item.day)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryRow(categorySum: CategorySum, total: Double, onClick: () -> Unit) {
    val percent = (categorySum.total / total).toFloat()
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F0F)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(categorySum.category, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color.White)
                Text(CurrencyFormatter.format(categorySum.total), fontWeight = FontWeight.ExtraBold, color = Color(0xFFBB86FC))
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = Color(0xFF6200EE),
                trackColor = Color(0xFF222222),
                strokeCap = StrokeCap.Round
            )
            Row(modifier = Modifier.padding(top = 10.dp)) {
                Text("${(percent * 100).toInt()}% OF TOTAL FLOW", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
}
