package com.fintech.demo.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 【職責】FinTechDemo【主入口】Application（等同單專案的 main）。
 * 【技巧】IntelliJ Run Configuration 選這個類即可最短 Demo；前端另開 Vite。
 * 【概念】其他 *Application 是微服務加開項，不是「不知道開哪個」的替代主入口。
 *
 * <pre>
 * IntelliJ：Run → OrderServiceApplication
 * 前端：cd frontend; npm run dev → http://localhost:5173
 * 帳號：trader1 / password
 * </pre>
 */
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
