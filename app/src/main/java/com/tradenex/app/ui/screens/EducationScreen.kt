package com.tradenex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tradenex.app.data.AppState
import com.tradenex.app.data.ContentKind
import com.tradenex.app.data.Lesson
import com.tradenex.app.ui.components.GlassCard
import com.tradenex.app.ui.components.IconBox
import com.tradenex.app.ui.components.SectionTitle
import com.tradenex.app.ui.components.TxChip
import com.tradenex.app.ui.theme.Tx

private val filters = listOf("همه", "متن", "عکس", "ویدیو", "ترکیبی")

private fun group(k: ContentKind): Int = when (k) {
    ContentKind.TEXT -> 1
    ContentKind.IMAGE -> 2
    ContentKind.VIDEO -> 3
    else -> 4
}

private fun icons(k: ContentKind): List<ImageVector> = when (k) {
    ContentKind.TEXT -> listOf(Icons.Rounded.TextFields)
    ContentKind.IMAGE -> listOf(Icons.Rounded.Image)
    ContentKind.VIDEO -> listOf(Icons.Rounded.Videocam)
    ContentKind.TEXT_IMAGE -> listOf(Icons.Rounded.TextFields, Icons.Rounded.Image)
    ContentKind.IMAGE_TEXT -> listOf(Icons.Rounded.Image, Icons.Rounded.TextFields)
    ContentKind.TEXT_VIDEO -> listOf(Icons.Rounded.TextFields, Icons.Rounded.Videocam)
    ContentKind.VIDEO_TEXT -> listOf(Icons.Rounded.Videocam, Icons.Rounded.TextFields)
}

private fun tint(k: ContentKind): Color = when (group(k)) {
    1 -> Tx.Gold
    2 -> Tx.Green
    3 -> Tx.Purple
    else -> Tx.Blue
}

@Composable
fun EducationScreen(nav: NavController) {
    var filter by remember { mutableStateOf(0) }
    var creating by remember { mutableStateOf<ContentKind?>(null) }
    var title by remember { mutableStateOf("") }
    var qrInfo by remember { mutableStateOf(false) }

    val shown = AppState.lessons.filter { filter == 0 || group(it.kind) == filter }

    Page(title = "آموزش و دانش") {
        Reveal(0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filters.forEachIndexed { i, f -> TxChip(f, filter == i, { filter = i }) }
            }
        }
        shown.forEachIndexed { i, l ->
            Reveal(i + 1) { LessonCard(l, onShare = { qrInfo = true }) }
        }
        Reveal(shown.size + 1) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("ساخت محتوای جدید", "هر ترکیبی که بخواهی")
                    ContentKind.entries.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { k -> KindTile(k, Modifier.weight(1f)) { creating = k; title = "" } }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Tx.Blue.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable { qrInfo = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Rounded.QrCode2, null, tint = Tx.Blue)
                            Text("  دریافت درس با QR", color = Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    creating?.let { kind ->
        AlertDialog(
            onDismissRequest = { creating = null },
            containerColor = Color(0xFF14141A),
            title = { Text(kind.label, color = Tx.Text) },
            text = { TxField(title, { title = it }, "عنوان درس") },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        AppState.lessons.add(Lesson(System.currentTimeMillis(), title.trim(), kind, 0f))
                    }
                    creating = null
                }) { Text("افزودن", color = Tx.Gold) }
            },
            dismissButton = { TextButton(onClick = { creating = null }) { Text("انصراف", color = Tx.Muted) } },
        )
    }
    if (qrInfo) {
        AlertDialog(
            onDismissRequest = { qrInfo = false },
            containerColor = Color(0xFF14141A),
            title = { Text("اشتراک با QR", color = Tx.Text) },
            text = { Text("ساخت و اسکن QR برای ارسال درس به دوستان در نسخه بعدی اضافه می‌شود.", color = Tx.Muted) },
            confirmButton = { TextButton(onClick = { qrInfo = false }) { Text("باشه", color = Tx.Gold) } },
        )
    }
}

@Composable
private fun LessonCard(l: Lesson, onShare: () -> Unit) {
    val color = tint(l.kind)
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.12f))
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Row { icons(l.kind).forEach { Icon(it, null, tint = color, modifier = Modifier.size(if (icons(l.kind).size > 1) 20.dp else 28.dp)) } }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(l.title, color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Text(l.kind.label, color = Tx.Muted, fontSize = 11.sp)
                Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))) {
                    Box(Modifier.fillMaxWidth(l.progress.coerceIn(0.02f, 1f)).height(5.dp).clip(RoundedCornerShape(3.dp)).background(color))
                }
            }
            IconBox(Icons.Rounded.QrCode2, "اشتراک با QR", onShare)
        }
    }
}

@Composable
private fun KindTile(k: ContentKind, modifier: Modifier, onClick: () -> Unit) {
    val color = tint(k)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .height(72.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Tx.Gold.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row { icons(k).forEach { Icon(it, null, tint = color, modifier = Modifier.size(20.dp)) } }
        Text(k.label, color = Tx.Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}
