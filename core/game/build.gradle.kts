// 게임 규칙의 유일한 원본. 안드로이드도, 네트워크도, DB 도 모른다.
// 폰(app), 워치(wear), 서버(server)가 모두 이 모듈로 레벨·스탯을 계산한다.
plugins {
    alias(libs.plugins.lifereset.jvm.library)
}
