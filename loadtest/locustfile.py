"""
FinTechDemo Locust baseline — login → POST /api/orders → GET /api/orders.

【職責】模擬 trader 登入後下單並列表查詢的最短壓測流程。
【技巧】on_start 取 JWT，後續 task 帶 Authorization；clientOrderId 用 uuid 避免冪等衝突。
【概念】預設對 order-service :8081；也可 --host http://localhost:8080 打 Gateway。

用法：
  locust -f locustfile.py --host http://localhost:8081 --headless -u 5 -r 1 -t 30s
"""

from __future__ import annotations

import os
import uuid

from locust import HttpUser, between, task

USERNAME = os.getenv("FINTECH_USER", "trader1")
PASSWORD = os.getenv("FINTECH_PASSWORD", "password")


class TradingUser(HttpUser):
    """
    【職責】每位虛擬使用者：登入一次，再循環下單＋列表。
    【技巧】wait_time 短間隔即可驗證管線；正式壓測再調高 users／run-time。
    【概念】JWT 放 header；列表用 page/size 對齊 API 契約。
    """

    wait_time = between(0.2, 0.8)

    def on_start(self):
        with self.client.post(
            "/api/auth/login",
            json={"username": USERNAME, "password": PASSWORD},
            name="POST /api/auth/login",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"login failed: {resp.status_code}")
                self.token = None
                return
            body = resp.json()
            self.token = body.get("token")
            if not self.token:
                resp.failure("login response missing token")
                return
            resp.success()
        self._auth = {"Authorization": f"Bearer {self.token}"}

    @task(2)
    def create_order(self):
        if not getattr(self, "token", None):
            return
        payload = {
            "clientOrderId": f"lt-{uuid.uuid4()}",
            "symbol": "AAPL",
            "side": "BUY",
            "quantity": 1,
            "price": 150.00,
        }
        self.client.post(
            "/api/orders",
            json=payload,
            headers=self._auth,
            name="POST /api/orders",
        )

    @task(3)
    def list_orders(self):
        if not getattr(self, "token", None):
            return
        self.client.get(
            "/api/orders?page=0&size=10",
            headers=self._auth,
            name="GET /api/orders",
        )
