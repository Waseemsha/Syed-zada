package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InvoiceEntity
import com.example.data.ProductItem
import com.example.ui.theme.*
import com.example.utils.InvoicePdfHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InvoiceViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Workspace, 1: Saved Sheets, 2: Cloud Sync

    val activeInvoice by viewModel.currentActiveInvoice.collectAsState()
    val history by viewModel.invoiceHistory.collectAsState()

    // Clean Minimalism pure dark gradient ending on deep slate
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            DarkBackground, // #0A0A0A
            Color(0xFF0F172A)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = bgGradient)
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Elegant Brand Header
            BrandHeader()

            // Dashboard Segment Navigation Tabs
            TabRowSection(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                historyCount = history.size
            )

            HorizontalDivider(
                color = BorderColor,
                thickness = 1.dp
            )

            // Dynamic view loading depending on chosen section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> WorkspaceView(viewModel = viewModel, activeInvoice = activeInvoice)
                    1 -> HistoryView(viewModel = viewModel, history = history)
                    2 -> CloudSyncView(viewModel = viewModel)
                }
            }
        }

        // Live notification banner for background operations
        if (viewModel.syncStatusMessage.isNotBlank()) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearFeedbackMessage() }) {
                        Text("Dismiss", color = AccentCyan)
                    }
                },
                containerColor = DarkSurface,
                contentColor = TextPrimary
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (viewModel.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ElectricBlue,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(viewModel.syncStatusMessage, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun BrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "SELLER ASPIRE INDUSTRY PVT LTD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentCyan, // blue-500 equivalent style
                letterSpacing = 1.2.sp
            )
            Text(
                text = "LOGISTICS & SHIPPING MANAGEMENT",
                fontSize = 8.sp,
                color = TextSecondary, // slate-500
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Live digital platform state widget
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(AccentCyan, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SYS ACTIVE",
                fontSize = 8.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TabRowSection(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    historyCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val tabs = listOf(
            RowItem("Workspace", Icons.Default.Add),
            RowItem("History ($historyCount)", Icons.Default.List),
            RowItem("Cloud Sync", Icons.Default.Settings)
        )

        tabs.forEachIndexed { index, tab ->
            val isActive = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) ElectricBlue else DarkSurfaceVariant)
                    .border(
                        1.dp,
                        if (isActive) AccentCyan else BorderColor,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.text,
                        tint = if (isActive) Color.White else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tab.text,
                        color = if (isActive) Color.White else TextPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

data class RowItem(val text: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun WorkspaceView(
    viewModel: InvoiceViewModel,
    activeInvoice: InvoiceEntity?
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        // Step 1: Entry Form Card
        item {
            ProductInputCard(viewModel = viewModel)
        }

        // Step 2: Added Items List
        if (viewModel.draftProducts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Loading Sheet Items",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(
                        onClick = { viewModel.clearDraftSheet() }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear List", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset All", color = Color(0xFFFF5252), fontSize = 12.sp)
                    }
                }
            }

            itemsIndexed(viewModel.draftProducts) { index, item ->
                DraftItemRow(
                    item = item,
                    index = index,
                    onEdit = { viewModel.startEditingDraftItem(index) },
                    onDelete = { viewModel.deleteDraftItem(index) }
                )
            }

            // Real Time Totals Callout Board
            item {
                DraftTotalsBoard(viewModel = viewModel)
            }

            // Company/Header Setup Card
            item {
                CompanySetupCard(viewModel = viewModel)
            }

            // Quick invoice generation triggers
            item {
                Button(
                    onClick = {
                        viewModel.compileAndSaveInvoice { savedInvc ->
                            Toast.makeText(context, "Invoice Compiled & Saved Locally!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("compile_sheet_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D7CFF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Compile")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "COMPILE OFFICIAL SHIPPING SHEET",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            // Elegant empty state message with custom layout
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp)
                        .background(Color(0xFF131A2E), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF253352), RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Welcome icon",
                            tint = Color(0xFF2D7CFF).copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Draft Table is Empty",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enter product name, weight, and quantities in the form above to automatically compile calculations in real-time.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A5C6),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Compiled Print Preview panel
        if (activeInvoice != null) {
            item {
                Text(
                    text = "Professional Invoice / Shipping Sheet Preview",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF33C1FF),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                InvoiceLiveCard(invoice = activeInvoice!!)
            }

            item {
                InvoiceOptionsPanel(viewModel = viewModel, invoice = activeInvoice!!)
            }
        }
    }
}

@Composable
fun ProductInputCard(viewModel: InvoiceViewModel) {
    val isEditing = viewModel.editingProductIndex >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (isEditing) AccentCyan else BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Item #${viewModel.editingProductIndex + 1}" else "New Item Specification",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEditing) AccentCyan else Color.White
                )
                if (isEditing) {
                    TextButton(onClick = { viewModel.cancelEditing() }) {
                        Text("Cancel Edit", color = Color(0xFFFF5252), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Input Fields
            OutlinedTextField(
                value = viewModel.inputProductName,
                onValueChange = { viewModel.inputProductName = it },
                label = { Text("Product Name", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("product_name_input"),
                colors = defaultTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )

            // Fast suggestions row for product names
            FastProductSuggestions { viewModel.inputProductName = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.inputQuantity,
                    onValueChange = { viewModel.inputQuantity = it },
                    label = { Text("Pieces (Qty)", fontSize = 11.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quantity_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = defaultTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )

                OutlinedTextField(
                    value = viewModel.inputWeight,
                    onValueChange = { viewModel.inputWeight = it },
                    label = { Text("Wt (kg / piece)", fontSize = 11.sp) },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("weight_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = defaultTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                )
            }

            // Live weight multiplications readout
            if (viewModel.currentItemTotalWeight > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Line weight:", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        String.format(Locale.US, "%.3f kg", viewModel.currentItemTotalWeight),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }
            }

            OutlinedTextField(
                value = viewModel.inputColor,
                onValueChange = { viewModel.inputColor = it },
                label = { Text("Color", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("color_input"),
                colors = defaultTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
            )

            // Fast color suggestions
            FastColorSuggestions { viewModel.inputColor = it }

            if (viewModel.formError.isNotBlank()) {
                Text(
                    text = viewModel.formError,
                    color = Color(0xFFFF5252),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.addDraftItem() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("add_item_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEditing) AccentCyan else ElectricBlue,
                    contentColor = if (isEditing) Color.Black else Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if (isEditing) Icons.Default.Done else Icons.Default.Add, contentDescription = "Action")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isEditing) "APPLY ROW CORRECTION" else "ADD PRODUCT TO LIST",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
fun FastProductSuggestions(onClick: (String) -> Unit) {
    val items = listOf("Bobbins", "Copper Wire", "Alu Strips", "PVC Roll", "Wire Reels")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { label ->
            Box(
                modifier = Modifier
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable { onClick(label) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(label, fontSize = 9.sp, color = AccentCyan)
            }
        }
    }
}

@Composable
fun FastColorSuggestions(onClick: (String) -> Unit) {
    val items = listOf("Red", "Blue", "Black", "Yellow", "Copper", "Grey")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { label ->
            Box(
                modifier = Modifier
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .clickable { onClick(label) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(label, fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun DraftItemRow(
    item: ProductItem,
    index: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(item.color.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${item.quantity} pieces × ${item.weightPerPiece} kg",
                    fontSize = 11.5.sp,
                    color = TextSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.3f kg", item.totalWeight),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text("Total Weight", fontSize = 8.5.sp, color = TextSecondary)
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Row", tint = AccentCyan, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun DraftTotalsBoard(viewModel: InvoiceViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElectricBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LIVE CALCULATION",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f),
                    letterSpacing = 1.5.sp
                )
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "SYS_VER_4.2",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Pieces", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${viewModel.draftTotalPieces}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pcs", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 3.dp))
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Weight", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.US, "%.1f", viewModel.draftTotalWeight),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kg", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CompanySetupCard(viewModel: InvoiceViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Extra Shipping Metadata", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)

            OutlinedTextField(
                value = viewModel.savedBillingName,
                onValueChange = { viewModel.updateBillingName(it) },
                label = { Text("Carrier/Bill Heading (Optional)", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
            )

            OutlinedTextField(
                value = viewModel.inputRemarks,
                onValueChange = { viewModel.inputRemarks = it },
                label = { Text("Special Packing Remarks / Core notes", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 11.5.sp)
            )
        }
    }
}

@Composable
fun InvoiceLiveCard(invoice: InvoiceEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), // White paper theme
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Paper Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        invoice.clientName.uppercase(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color(0xFF0A0E1A)
                    )
                    Text(
                        "SHIPPING SHEET & SYSTEM INVOICE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D7CFF)
                    )
                    Text(
                        "Invoice ID: ${invoice.invoiceNumber}",
                        fontSize = 7.5.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                // Small circular logo design
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF2D7CFF), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SAI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE1E6EB))
            Spacer(modifier = Modifier.height(8.dp))

            // Inline Product Grid Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F6F9))
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text("ITEM", modifier = Modifier.weight(1.5f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("QTY", modifier = Modifier.weight(0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                Text("WT/PC", modifier = Modifier.weight(0.9f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                Text("TOTAL WT", modifier = Modifier.weight(1f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
                Text("COLOR", modifier = Modifier.weight(0.9f), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, textAlign = TextAlign.Center)
            }

            // Products list
            invoice.products.forEachIndexed { sIdx, prod ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Text(
                        text = "${sIdx + 1}. ${prod.name}",
                        modifier = Modifier.weight(1.5f),
                        fontSize = 8.sp,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(prod.quantity.toString(), modifier = Modifier.weight(0.7f), fontSize = 8.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                    Text(String.format(Locale.US, "%.2f", prod.weightPerPiece), modifier = Modifier.weight(0.9f), fontSize = 8.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                    Text(String.format(Locale.US, "%.2f", prod.totalWeight), modifier = Modifier.weight(1f), fontSize = 8.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                    Text(prod.color, modifier = Modifier.weight(0.9f), fontSize = 8.sp, color = Color.DarkGray, textAlign = TextAlign.Center)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE1E6EB))
            Spacer(modifier = Modifier.height(6.dp))

            // Print summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF4F6F9))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GRAND TOTALS:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text("${invoice.totalPieces} Pcs", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(String.format(Locale.US, "%.3f kg Load Limit", invoice.totalWeight), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            if (invoice.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Notes: ${invoice.remarks}", fontSize = 7.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Digital Signatory
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(60.dp).height(1.dp).background(Color.LightGray))
                    Text("Recipient Signature", fontSize = 6.5.sp, color = Color.Gray)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.width(60.dp).height(1.dp).background(Color.LightGray))
                    Text("Authorized Signatory (SAI)", fontSize = 6.5.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun InvoiceOptionsPanel(
    viewModel: InvoiceViewModel,
    invoice: InvoiceEntity
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Download PDF
        Button(
            onClick = {
                val file = InvoicePdfHelper.generateInvoicePdf(context, invoice)
                if (file != null) {
                    Toast.makeText(context, "PDF saved to app cache!", Toast.LENGTH_SHORT).show()
                    InvoicePdfHelper.shareInvoicePdf(context, file)
                } else {
                    Toast.makeText(context, "Error compiling PDF document", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Icon(Icons.Default.Share, contentDescription = "PDF File", modifier = Modifier.size(12.dp), tint = AccentCyan)
            Spacer(modifier = Modifier.width(6.dp))
            Text("PDF & Share", fontSize = 11.sp, color = TextPrimary)
        }

        // Print Out
        Button(
            onClick = {
                val file = InvoicePdfHelper.generateInvoicePdf(context, invoice)
                if (file != null) {
                    InvoicePdfHelper.printInvoice(context, file)
                } else {
                    Toast.makeText(context, "Error spawning print engine", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Print Out", modifier = Modifier.size(12.dp), tint = AccentCyan)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Print Sheet", fontSize = 11.sp, color = TextPrimary)
        }

        // Cloud Push / Sync
        Button(
            onClick = { viewModel.syncToGoogleSheets(invoice) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Cloud upload", modifier = Modifier.size(12.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Sync Row", fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
fun HistoryView(
    viewModel: InvoiceViewModel,
    history: List<InvoiceEntity>
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.List, contentDescription = "History empty", tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text("No Historic Invoices Found", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Compile and save shipping sheets on the Workspace tab. They will appear here for reprint, sharing, and batch logs.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
        ) {
            itemsIndexed(history) { index, doc ->
                HistoryInvoiceCard(viewModel = viewModel, invoice = doc)
            }
        }
    }
}

@Composable
fun HistoryInvoiceCard(
    viewModel: InvoiceViewModel,
    invoice: InvoiceEntity
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    val formattedDate = sdf.format(Date(invoice.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(invoice.invoiceNumber, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(formattedDate, fontSize = 10.sp, color = TextSecondary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val fileTemp = InvoicePdfHelper.generateInvoicePdf(context, invoice)
                            if (fileTemp != null) InvoicePdfHelper.printInvoice(context, fileTemp)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Print", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = {
                            val fileTemp = InvoicePdfHelper.generateInvoicePdf(context, invoice)
                            if (fileTemp != null) InvoicePdfHelper.shareInvoicePdf(context, fileTemp)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = AccentCyan, modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { viewModel.deleteInvoiceFromHistory(invoice) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${invoice.products.size} product categories loaded",
                    fontSize = 11.5.sp,
                    color = TextPrimary
                )

                Text(
                    text = String.format(Locale.US, "Weight Total: %.3f kg", invoice.totalWeight),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            Button(
                onClick = { viewModel.selectActiveInvoice(invoice) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(34.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Text("View Preview & Synced details", fontSize = 11.sp, color = TextPrimary)
            }
        }
    }
}

@Composable
fun CloudSyncView(viewModel: InvoiceViewModel) {
    val context = LocalContext.current
    var isEditingTutorialCode by remember { mutableStateOf(false) }

    val appsScriptPattern = """
function doPost(e) {
  try {
    var payload = JSON.parse(e.postData.contents);
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    
    // Add header row if first time running
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(["DateTime", "Invoice Number", "Client Heading", "Product Name", "Product Color", "Qty Pcs", "Weight Per Pc (kg)", "Total Weight (kg)", "Remarks"]);
    }
    
    // Append rows for each product listed inside payload
    for (var i = 0; i < payload.products.length; i++) {
      var item = payload.products[i];
      sheet.appendRow([
        payload.dateTime,
        payload.invoiceNumber,
        payload.clientName,
        item.name,
        item.color,
        item.quantity,
        item.weightPerPiece,
        item.totalWeight,
        payload.remarks
      ]);
    }
    
    return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
  } catch (error) {
    return ContentService.createTextOutput("Error: " + error.message).setMimeType(ContentService.MimeType.TEXT);
  }
}
""".trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = "Sync settings", tint = AccentCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Sheets Sync Engine", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Text(
                        "Hook this application to push record parameters straight onto actual Google Sheets in real-time.",
                        fontSize = 11.5.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = viewModel.googleSheetsUrl,
                        onValueChange = { viewModel.updateGoogleSheetsUrl(it) },
                        label = { Text("Google Web App Sync URL", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input"),
                        colors = defaultTextFieldColors(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Auto-Sync on save", fontSize = 13.sp, color = Color.White)
                            Text("Automates spreadsheet triggers", fontSize = 10.5.sp, color = TextSecondary)
                        }

                        Switch(
                            checked = viewModel.autoSaveToSheets,
                            onCheckedChange = { viewModel.updateAutoSaveToSheets(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricBlue
                            )
                        )
                    }
                }
            }
        }

        // Actionable accordion step-by-step tutorial card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.7f)),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditingTutorialCode = !isEditingTutorialCode },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = "Tutorial info", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("How to create Web URL guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(
                            imageVector = if (isEditingTutorialCode) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle accordion",
                            tint = TextSecondary
                        )
                    }

                    if (isEditingTutorialCode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text("Step 1: Open Google Sheets\nCreate a new sheet inside Google Drive, name it.", fontSize = 11.sp, color = TextPrimary)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Step 2: Enter Apps Script Window\nTap 'Extensions -> Apps Script' in Sheet menu.", fontSize = 11.sp, color = TextPrimary)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Step 3: Paste the custom code below:\nDelete current blank doGet files, paste script:", fontSize = 11.sp, color = TextPrimary)

                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = appsScriptPattern,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                        }

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Apps Script", appsScriptPattern)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Script Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .height(38.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Text("COPY SCRIPT CODE TO CLIPBOARD", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Step 4: Deploy\n- Click 'Deploy -> New deployment'\n- Select Gear icon -> 'Web App'\n- Execute as: 'Me'\n- Access limit: 'Anyone'\n- Authorize Google access permissions and copy deployment URL into Web App Sync input above!",
                            fontSize = 11.sp, color = TextPrimary, lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// Utility for typography scaling across devices
@Composable
fun ssp() = 13.sp

// Reusable textfield styling
@Composable
fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentCyan,
    unfocusedBorderColor = BorderColor,
    focusedTextColor = Color.White,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = AccentCyan,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurfaceVariant
)
