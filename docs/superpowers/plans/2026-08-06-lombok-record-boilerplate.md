# Plan: Lombok / record boilerplate

1. 根 `build.gradle` 為所有 subprojects 加 lombok；Boot 模組 annotationProcessor 順序 lombok → configuration-processor；`common/build.gradle` 去掉重複。
2. Entity 全部改 `@Getter`/`@Setter`，刪手寫存取子。
3. 可變 DTO 改 `@Data`；不可變 DTO／Event 改 `record`（必要時改呼叫端 accessor）。
4. `.\gradlew.bat check` 綠燈。
