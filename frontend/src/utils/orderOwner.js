/**
 * 【職責】把訂單擁有者顯示成登入帳號（trader1／admin），不要用 #userId。
 * 【技巧】優先後端 username；舊 order-service 還沒帶欄位時，用種子 userId 對照。
 * 【概念】Demo 對帳要唸名稱：userId 1＝trader1、2＝admin。
 */

const SEED_USERNAMES = {
  1: 'trader1',
  2: 'admin'
};

/**
 * 【目的】回傳訂單擁有者帳號。
 * 【副作用】無。
 */
export function orderOwnerName(order) {
  if (order?.username) {
    return order.username;
  }
  const mapped = SEED_USERNAMES[Number(order?.userId)];
  return mapped || '';
}

/**
 * 【目的】判斷這筆訂單是否屬於目前登入者。
 * 【副作用】無。
 */
export function isOwnOrder(order, currentUsername) {
  const owner = orderOwnerName(order);
  return Boolean(owner && currentUsername && owner === currentUsername);
}
