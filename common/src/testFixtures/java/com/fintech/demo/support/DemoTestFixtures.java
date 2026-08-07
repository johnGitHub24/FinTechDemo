package com.fintech.demo.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 【職責】測試用 JSON fixture 載入器：從 {@code docs/test-data/{category}/{caseId}.json} 讀取。
 * 【技巧】自 {@code user.dir} 向上尋找含 {@code docs/test-data} 的專案根（多模組 Gradle 相容）。
 * 【概念】把案例資料與測試程式分離，同一份 JSON 可對照規格書 Case ID（對齊 TradingCRUD CrudTestFixtures）。
 * 【邊界】不負責解析／斷言，只回傳字串內容。
 */
public final class DemoTestFixtures {

    private DemoTestFixtures() {
    }

    /**
     * 【職責】載入指定分類與 Case ID 的 JSON 字串。
     * 【技巧】路徑相對專案根 {@code docs/test-data/...}。
     * 【概念】Case ID 與檔名對齊（如 ORDER-001-SUCCESS），方便追蹤規格與簡報。
     *
     * @param category 子目錄（auth／order／risk／account）
     * @param caseId   不含副檔名的案例檔名
     * @return JSON 字串
     */
    public static String loadJson(String category, String caseId) {
        Path path = resolveFixture(category, caseId);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load fixture: " + category + "/" + caseId + " @ " + path, e);
        }
    }

    private static Path resolveFixture(String category, String caseId) {
        Path relative = Paths.get("docs", "test-data", category, caseId + ".json");
        Path dir = Paths.get("").toAbsolutePath().normalize();
        for (int i = 0; i < 8 && dir != null; i++) {
            Path candidate = dir.resolve(relative);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        Path fallback = Paths.get("").toAbsolutePath().normalize().resolve(relative);
        if (Files.isRegularFile(fallback)) {
            return fallback;
        }
        throw new UncheckedIOException(
                new IOException("Fixture not found: " + relative + " (searched from " + Paths.get("").toAbsolutePath() + ")"));
    }
}
