package com.tradenex.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tradenex.app.data.AppState
import com.tradenex.app.ui.theme.Tx
import kotlin.math.PI
import kotlin.math.sin

/** Animated aurora background shared by every screen. */
@Composable
fun TxBackground(content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "aurora")
    val a by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "a",
    )
    Box(Modifier.fillMaxSize().background(Tx.Bg)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.radialGradient(
                    listOf(Tx.Gold.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width * (0.95f - 0.35f * a), size.height * 0.04f),
                    radius = size.width * 0.9f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Tx.Green.copy(alpha = 0.11f), Color.Transparent),
                    center = Offset(size.width * (0.05f + 0.3f * a), size.height * 0.45f),
                    radius = size.width * 0.8f,
                ),
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Tx.Red.copy(alpha = 0.09f), Color.Transparent),
                    center = Offset(size.width, size.height * (0.95f - 0.25f * a)),
                    radius = size.width * 0.8f,
                ),
            )
        }
        if (AppState.settings.particles && AppState.settings.animations > 0) {
            val tick by t.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
                label = "tick",
            )
            Canvas(Modifier.fillMaxSize()) { drawParticles(tick, if (AppState.settings.animations > 1) 46 else 26) }
        }
        content()
    }
}

/** Slow rising gold dust. Positions are a pure function of the index, so there is no state to keep. */
private fun DrawScope.drawParticles(tick: Float, count: Int) {
    for (i in 0 until count) {
        val seed = (i * 7919 % 1000) / 1000f
        val seed2 = (i * 104729 % 997) / 997f
        val speed = 0.6f + seed2 * 0.9f
        val phase = (tick * speed + seed) % 1f
        val x = size.width * seed2 + sin((phase * 2f * PI + i).toFloat()) * 14f
        val y = size.height * (1.05f - phase * 1.1f)
        val fade = sin(phase * PI).toFloat().coerceIn(0f, 1f)
        val rad = (0.8f + seed * 1.8f) * density
        drawCircle(Tx.GoldHi.copy(alpha = 0.55f * fade), rad, Offset(x, y))
        drawCircle(Tx.Gold.copy(alpha = 0.14f * fade), rad * 3.2f, Offset(x, y))
    }
}

/** Corner radius chosen in Settings (0 sharp, 1 normal, 2 round). */
fun cardRadius(): androidx.compose.ui.unit.Dp = when (AppState.settings.cardRadius) {
    0 -> 10.dp
    2 -> 26.dp
    else -> 18.dp
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    border: Color = Tx.Gold.copy(alpha = if (Tx.Luxe) 0.5f else 0.38f),
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cardRadius())
    val luxe = Tx.Luxe
    val sweepOn = luxe && AppState.settings.shimmer && AppState.settings.animations > 0
    val sweep = if (sweepOn) {
        val t = rememberInfiniteTransition(label = "sweep")
        val v by t.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing)),
            label = "v",
        )
        v
    } else {
        -1f
    }
    val fill = if (luxe) {
        Brush.linearGradient(listOf(Tx.CardA, Tx.CardB))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.02f)))
    }
    Box(
        modifier
            .shadow(
                if (luxe) 14.dp else 10.dp, shape,
                ambientColor = Tx.Gold.copy(alpha = if (luxe) 0.35f else 0.25f),
                spotColor = Tx.Gold.copy(alpha = if (luxe) 0.35f else 0.25f),
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .drawWithContent {
                drawContent()
                if (sweep > -0.9f) {
                    val x = size.width * sweep
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Tx.GoldPale.copy(alpha = 0.10f), Color.Transparent),
                            start = Offset(x - size.width * 0.25f, 0f),
                            end = Offset(x + size.width * 0.25f, size.height),
                        ),
                    )
                }
            },
        content = content,
    )
}

@Composable
fun TxHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (onBack != null) {
            IconBox(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت", onBack)
        }
        Text(title, color = Tx.Text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
fun IconBox(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Tx.Gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = Tx.Gold)
    }
}

@Composable
fun TxChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) Tx.Gold.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f), label = "chipbg")
    val bd by animateColorAsState(if (selected) Tx.Gold else Color.White.copy(alpha = 0.16f), label = "chipbd")
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, bd, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Tx.GoldPale else Tx.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "btn")
    val glow by t.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "glow",
    )
    val shape = RoundedCornerShape(cardRadius())
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(14.dp, shape, ambientColor = Tx.Gold.copy(alpha = glow), spotColor = Tx.Gold.copy(alpha = glow))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Tx.GoldHi, Tx.GoldDeep)))
            .border(1.dp, Tx.GoldPale, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Tx.Ink, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun StatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier, sub: String? = null) {
    GlassCard(modifier.height(68.dp)) {
        Column(Modifier.fillMaxSize().padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = Tx.Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            if (sub != null) Text(sub, color = Tx.Muted, fontSize = 10.sp)
        }
    }
}

@Composable
fun SectionTitle(text: String, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Tx.Text, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = Tx.Muted, fontSize = 11.sp)
    }
}

@Composable
fun PulseDot(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dot")
    val s by t.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "s",
    )
    Box(modifier.size(14.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(8.dp).scale(s).clip(CircleShape).background(color.copy(alpha = 0.35f)))
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun TxBottomBar(current: String, onNavigate: (String) -> Unit) {
    val moreRoutes = setOf("more", "sessions", "strategies", "calendar", "settings", "tags", "themes")
    val shape = RoundedCornerShape(26.dp)
    val t = rememberInfiniteTransition(label = "fab")
    val pulse by t.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).navigationBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Tx.NavA, Tx.NavB)))
                .border(1.dp, Tx.Gold.copy(alpha = 0.45f), shape),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            NavBtn("خانه", Icons.Rounded.Home, current == "home") { onNavigate("home") }
            NavBtn("گزارش‌ها", Icons.Rounded.BarChart, current == "perf") { onNavigate("perf") }
            Spacer(Modifier.width(64.dp))
            NavBtn("آموزش", Icons.Rounded.MenuBook, current == "edu") { onNavigate("edu") }
            NavBtn("بیشتر", Icons.Rounded.GridView, current in moreRoutes) { onNavigate("more") }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-22).dp)
                .scale(pulse)
                .size(58.dp)
                .shadow(18.dp, CircleShape, ambientColor = Tx.Gold, spotColor = Tx.Gold)
                .clip(CircleShape)
                .background(if (Tx.Luxe) Brush.radialGradient(listOf(Tx.GoldHi, Tx.GoldDeep)) else Brush.radialGradient(listOf(Color(0xFF3A3320), Color(0xFF0B0B0E))))
                .border(2.dp, Tx.GoldPale, CircleShape)
                .clickable { onNavigate("add") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, "ثبت معامله جدید", tint = if (Tx.Luxe) Tx.Ink else Tx.Gold, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun NavBtn(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val c by animateColorAsState(if (selected) Tx.Gold else Tx.Muted, label = "nav")
    Column(
        Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = c, modifier = Modifier.size(24.dp))
        Text(label, color = c, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
