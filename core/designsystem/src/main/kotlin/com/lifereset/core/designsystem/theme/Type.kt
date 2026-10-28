package com.lifereset.core.designsystem.theme

import android.graphics.Typeface
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 글꼴.
 * 제목·숫자는 좁고 각진 글꼴(condensed)을 쓴다. 기본은 기기에 있는 sans-serif-condensed.
 * 원하는 글꼴로 바꾸려면:
 *   1) TTF 파일을 core/designsystem/src/main/res/font/ 에 넣는다 (예: display_semibold.ttf, 소문자·밑줄만)
 *   2) 아래 Display 를 FontFamily(Font(R.font.display_semibold, FontWeight.SemiBold)) 로 바꾼다
 *   추천: Saira Semi Condensed (Google Fonts, 무료 OFL 라이선스)
 * 한글 본문은 기기 기본 글꼴을 쓴다.
 */
object LrFonts {
    val Display: FontFamily = FontFamily(Typeface.create("sans-serif-condensed", Typeface.NORMAL))
    val Body: FontFamily = FontFamily.Default
}

@Immutable
data class LrTypography(
    /** 배경 위 큰 제목 (Day 23 / 66) */
    val hero: TextStyle,
    /** 창 제목 (Daily Quest, Status) */
    val panelTitle: TextStyle,
    /** 메뉴 줄 이름 */
    val rowTitle: TextStyle,
    /** 큰 숫자: 레벨, 능력치 */
    val number: TextStyle,
    val numberSmall: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val caption: TextStyle,
    val code: TextStyle,
)

private val skyShadow = Shadow(color = Color(0x73203040), offset = Offset(0f, 2f), blurRadius = 10f)

val DefaultTypography = LrTypography(
    hero = TextStyle(fontFamily = LrFonts.Display, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 42.sp, shadow = skyShadow),
    panelTitle = TextStyle(fontFamily = LrFonts.Display, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    rowTitle = TextStyle(fontFamily = LrFonts.Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    number = TextStyle(fontFamily = LrFonts.Display, fontWeight = FontWeight.Bold, fontSize = 30.sp),
    numberSmall = TextStyle(fontFamily = LrFonts.Display, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    title = TextStyle(fontFamily = LrFonts.Body, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    body = TextStyle(fontFamily = LrFonts.Body, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyStrong = TextStyle(fontFamily = LrFonts.Body, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    caption = TextStyle(fontFamily = LrFonts.Body, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    code = TextStyle(fontFamily = LrFonts.Display, fontWeight = FontWeight.Bold, fontSize = 12.sp),
)

/** 배경 위에 바로 쓰는 글자용 그림자 (사진이 밝아도 읽히게). */
val SkyTextShadow: Shadow = Shadow(color = Color(0x99141E2D), offset = Offset(0f, 1f), blurRadius = 6f)
