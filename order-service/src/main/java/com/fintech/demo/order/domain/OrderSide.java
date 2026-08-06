package com.fintech.demo.order.domain;

/**
 * 【職責】訂單買賣方向列舉。
 * 【技巧】以 enum 取代字串，避免大小寫／拼字錯誤流入交易路徑。
 * 【概念】BUY 扣現金加持倉；SELL 扣持倉加現金（見 TradingService.execute）。
 */
public enum OrderSide {
    BUY,
    SELL
}
