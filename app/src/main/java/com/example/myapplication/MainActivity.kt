package com.example.myapplication

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlin.math.max

class MainActivity : ComponentActivity(), SensorEventListener {
    lateinit var sm: SensorManager
    var rx by mutableStateOf(0f)
    var ry by mutableStateOf(0f)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding), rx, ry
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1)
    }

    override fun onPause() {
        super.onPause()
        sm.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            rx = rx * 0.9f + (-it.values[0]) * 0.1f
            ry = ry * 0.9f + (it.values[1]) * 0.1f
        }
    }
}

data class QSS(val text: String, val options: List<String>, val ans: Int)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, rx: Float, ry: Float) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val w = with(density) { configuration.screenWidthDp.dp.toPx() }
    val h = with(density) { configuration.screenHeightDp.dp.toPx() }
    val dot = with(density) { 30.dp.toPx() }
    var qs by remember { mutableStateOf(listOf<QSS>()) }
    LaunchedEffect(Unit) {
        try {
            val js = context.assets.open("myjson.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<QSS>>() {}.type
            qs = Gson().fromJson(js, type)
        } catch (e: Exception) {
            print("json error")
        }
    }
    if (qs.isEmpty()) return
    var qIdx by remember { mutableStateOf(0) }
    var qqIdx by remember { mutableStateOf(0) }
    val q = qs[qIdx]
    var cp by remember { mutableStateOf(Offset(w / 2, h / 2)) }
    var cal by remember { mutableStateOf(Offset.Zero) }
    var lock by remember { mutableStateOf(false) }
    var finish by remember { mutableStateOf(false) }
    var firstPage by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(0L) }
    var pt by remember { mutableStateOf(0f) }
    var sc by remember { mutableStateOf(0) }
    var wrong by remember { mutableStateOf(0) }
    var r1 by remember { mutableStateOf(Rect.Zero) }
    var r2 by remember { mutableStateOf(Rect.Zero) }
    var r3 by remember { mutableStateOf(Rect.Zero) }
    var r4 by remember { mutableStateOf(Rect.Zero) }
    var hover = 0
    if (!finish || !firstPage) {
        if (!lock) {
            if (r1.contains(cp)) hover = 1 else
                if (r2.contains(cp)) hover = 2 else
                    if (r3.contains(cp)) hover = 3 else
                        if (r4.contains(cp)) hover = 4
            if (hover > 0) {
                pt += 0.02f
                if (pt >= 1f) {
                    val ct = hover == q.ans
                    if (ct) sc += 10
                    else {
                        lock = true
                        locked = System.currentTimeMillis() + 2000
                        sc -= 5
                        wrong++
                    }
                    pt = 0f
                    cp = Offset(w / 2, h / 2)
                    qIdx = (qIdx + 1) % qs.size
                    qqIdx++
                    if (qqIdx == 10) {
                        finish = true
                    }
                }
            } else pt = 0f
        } else {
            pt = 0f
            hover = 0
        }
    }
    LaunchedEffect(rx, ry, pt) {
        while (true) {
            if (lock && System.currentTimeMillis() > locked) lock = false
            if (!finish && !firstPage) {
                cp = Offset(
                    (cp.x + (rx - cal.x)).coerceIn(0f, w - dot),
                    (cp.y + (ry - cal.y)).coerceIn(0f, h - dot)
                )
            }
            delay(16)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (firstPage) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(all = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GyroQuiz", style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(80.dp))
                Button(onClick = { firstPage = false }) { Text("Start") }
            }
        } else if (finish) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("總分 ： $sc")
                Text("正確率 ： ${(10 - wrong) / 10}")
                Spacer(Modifier.height(80.dp))
                Button(onClick = { finish = false }) { Text("重新挑戰") }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(all = 20.dp), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("題目", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    Text(q.text, style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold))
                    Text(
                        "分數 = $sc / 100",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "進度 = ${qqIdx + 1} / 10",
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .padding(all = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnsButton(
                            q.options[0],
                            hover == 1,
                            Modifier
                                .weight(1f)
                                .onGloballyPositioned { r1 = it.boundsInRoot() })
                        AnsButton(
                            q.options[1],
                            hover == 2,
                            Modifier
                                .weight(1f)
                                .onGloballyPositioned { r2 = it.boundsInRoot() })
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnsButton(
                            q.options[2],
                            hover == 3,
                            Modifier
                                .weight(1f)
                                .onGloballyPositioned { r3 = it.boundsInRoot() })
                        AnsButton(
                            q.options[3],
                            hover == 4,
                            Modifier
                                .weight(1f)
                                .onGloballyPositioned { r4 = it.boundsInRoot() })
                    }
                }
            }
            Box(Modifier.fillMaxSize()) {
                Button(
                    onClick = { cp = Offset(w / 2, h / 2); cal = Offset(rx, ry) },
                    Modifier.align(Alignment.TopEnd)
                ) {
                    Text("校正")
                }
            }
            Box(
                Modifier
                    .size(30.dp)
                    .offset { IntOffset(cp.x.toInt(), cp.y.toInt()) },
                Alignment.Center
            ) {
                if (pt > 0) {
                    CircularProgressIndicator(
                        progress = { pt },
                        Modifier.fillMaxSize(),
                        strokeWidth = 3.dp,
                        color = Color.Red
                    )
                }
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
            if (lock) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.7f)),
                    contentAlignment = Alignment.Center
                ) { Text("LOCK", style = TextStyle(fontSize = 60.sp, color = Color.Red)) }
            }
        }
    }
}

@Composable
fun AnsButton(x0: String, x1: Boolean, x2: Modifier) {
    val scale by animateFloatAsState(
        if (x1) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "scale"
    )
    Box(
        modifier = x2
            .fillMaxSize()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(
                RoundedCornerShape(20.dp)
            )
            .background(if (x1) Color.LightGray else Color.Gray)
            .border(
                width = if (x1) 2.dp else 1.dp,
                color = if (x1) Color.LightGray else Color.Gray,
                shape = RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = x0,
            fontSize = 48.sp
        )
    }
}
