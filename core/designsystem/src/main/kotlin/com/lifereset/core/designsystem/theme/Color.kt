package com.lifereset.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 앱 전체 색. 디자인을 바꿀 때 이 값만 고치면 모든 화면에 반영된다.
 * 이름은 '역할'로 짓는다 (gold 가 아니라 accent). 색을 바꿔도 이름은 그대로 맞다.
 *
 * 구성: 흐린 배경 사진 위에 흰 반투명 유리창, 연회색 메뉴 줄, 진회색 원형 아이콘.
 * 강조색(금색)은 '지금 선택된 것' 하나에만 쓴다.
 */
@Immutable
data class LrColors(
    /** 배경 사진이 없을 때의 바탕 */
    val sky: Color,
    /** 배경 위에 바로 놓이는 글자 (큰 제목, 메뉴 이름) */
    val onSky: Color,
    /** 흰 반투명 창 */
    val glass: Color,
    val glassBorder: Color,
    /** 창 안의 메뉴 줄 */
    val row: Color,
    val rowBorder: Color,
    /** 창 아래쪽 회색 띠 (요약, 다이얼로그 본문) */
    val band: Color,
    /** 제목 아래 얇은 선 */
    val rule: Color,
    /** 진회색 원형 아이콘 */
    val iconDisc: Color,
    val onIconDisc: Color,
    /** 회색 원형 메뉴 버튼 */
    val circle: Color,
    val circleRing: Color,
    val onCircle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** 선택 강조 (금색) */
    val accent: Color,
    val onAccent: Color,
    val track: Color,
    val trackBorder: Color,
    val exp: Color,
    val danger: Color,
    /** 다이얼로그 ○ / ✕ */
    val confirm: Color,
    val cancel: Color,
    /** 연출 뒤 막 */
    val scrim: Color,
)

val GlassColors = LrColors(
    sky = Color(0xFF6F8AA3),
    onSky = Color(0xFFFFFFFF),
    glass = Color(0xCCF6F7F9),
    glassBorder = Color(0xE6FFFFFF),
    row = Color(0xE0DEE0E4),
    rowBorder = Color(0xF2FFFFFF),
    band = Color(0x99C8CCD1),
    rule = Color(0xFF8C9097),
    iconDisc = Color(0xFF3E4146),
    onIconDisc = Color(0xFFFFFFFF),
    circle = Color(0xE0E4E5E7),
    circleRing = Color(0xD9FFFFFF),
    onCircle = Color(0xFF6B6F76),
    textPrimary = Color(0xFF3E4146),
    textSecondary = Color(0xFF5A5E65),
    textTertiary = Color(0xFF6B6F76),
    accent = Color(0xFFF0B400),
    onAccent = Color(0xFFFFFFFF),
    track = Color(0xFFD5D8DC),
    trackBorder = Color(0xFFA9ADB3),
    exp = Color(0xFF8CC63F),
    danger = Color(0xFFE04848),
    confirm = Color(0xFF2F80D8),
    cancel = Color(0xFFD84848),
    scrim = Color(0x8C969AA0),
)
