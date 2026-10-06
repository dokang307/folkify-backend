package com.folkify;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.SpringApplication;

/**
 * Chạy backend local KHÔNG cần Docker/Postgres cài sẵn: khởi động Postgres nhúng (zonky) rồi boot app.
 * Dùng cho demo/chụp màn hình/thử tay. Dữ liệu mất khi tắt (trừ khi đặt DEV_PG_DIR).
 *
 * <pre>
 * ./mvnw -q test-compile dependency:build-classpath -Dmdep.outputFile=target/dev-cp.txt -Dmdep.includeScope=test
 * java -cp "target/classes;target/test-classes;$(cat target/dev-cp.txt)" com.folkify.LocalDevServer
 * </pre>
 * Biến môi trường: DEV_PG_PORT (mặc định 55432), DEV_PG_DIR (thư mục dữ liệu, tuỳ chọn), SERVER_PORT, AI_SERVICE_URL.
 */
public final class LocalDevServer {

    private static final String DEFAULT_PG_PORT = "55432";

    private LocalDevServer() {}

    public static void main(String[] args) throws Exception {
        EmbeddedPostgres.Builder builder = EmbeddedPostgres.builder()
                .setPort(Integer.parseInt(System.getenv().getOrDefault("DEV_PG_PORT", DEFAULT_PG_PORT)));
        String dataDir = System.getenv("DEV_PG_DIR");
        if (dataDir != null && !dataDir.isBlank()) {
            builder.setDataDirectory(dataDir).setCleanDataDirectory(false);
        }
        EmbeddedPostgres postgres = builder.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                postgres.close();
            } catch (Exception ignored) {
                // tắt server, bỏ qua lỗi dọn dẹp
            }
        }));

        System.setProperty("spring.datasource.url", postgres.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        // R2 không dùng khi chạy local (upload file sẽ lỗi) — chỉ cần giá trị để app khởi động
        for (String key : new String[]{"r2.account-id", "r2.access-key-id", "r2.secret-access-key", "r2.bucket-name"}) {
            System.setProperty(key, "local-dev");
        }
        System.setProperty("r2.public-url", "http://localhost/r2"); // scalability-ok: placeholder, R2 disabled locally
        SpringApplication.run(FolkifyApplication.class, args);
    }
}
