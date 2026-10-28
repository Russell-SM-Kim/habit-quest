package com.lifereset.core.designsystem.component

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.theme.LrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 앱 전체 배경. 모든 화면은 이 위에 반투명 유리창으로 올라간다.
 *
 * - imageUri 가 없으면: 하늘색 바탕 + 흐릿한 구름 덩어리 (코드로 그림, 이미지 파일 필요 없음)
 * - imageUri 가 있으면: 그 사진을 흐리게(Android 12+) 깔고 살짝 어둡게 덮는다.
 *   사진은 설정 → 배경 이미지에서 고른다.
 *
 * 기본 배경 자체를 바꾸고 싶으면 [DefaultSky] 의 색과 위치만 고치면 된다.
 */
@Composable
fun LrBackground(
    imageUri: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(LrTheme.colors.sky)) {
        val image = rememberUriImage(imageUri)
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(18.dp) else Modifier),
            )
            Box(Modifier.fillMaxSize().background(Color(0x332A3440)))
        } else {
            DefaultSky()
        }
        content()
    }
}

/** 기본 하늘: 위는 밝은 하늘색, 아래는 연한 회청색, 구름 덩어리 몇 개. */
@Composable
private fun DefaultSky() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                0f to Color(0xFF8FB4D6),
                0.55f to Color(0xFF9FB5C9),
                1f to Color(0xFF7B8C9C),
            ),
        )
        // (가로 비율, 세로 비율, 반지름 비율, 불투명도)
        val clouds = listOf(
            floatArrayOf(0.15f, 0.18f, 0.45f, 0.55f),
            floatArrayOf(0.85f, 0.10f, 0.40f, 0.45f),
            floatArrayOf(0.55f, 0.42f, 0.55f, 0.35f),
            floatArrayOf(0.05f, 0.70f, 0.50f, 0.30f),
            floatArrayOf(0.95f, 0.80f, 0.45f, 0.28f),
        )
        val unit = maxOf(size.width, size.height)
        clouds.forEach { (x, y, r, a) ->
            val center = Offset(size.width * x, size.height * y)
            val radius = unit * r * 0.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = a), Color.White.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
    }
}

/** content:// 이미지를 화면 크기 정도로 줄여서 읽는다. 실패하면 null (기본 하늘로 대체). */
@Composable
private fun rememberUriImage(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        if (uri.isNullOrBlank()) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val parsed = Uri.parse(uri)
                val resolver = context.contentResolver
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val target = context.resources.displayMetrics.run { maxOf(widthPixels, heightPixels) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}
