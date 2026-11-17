import request from './request'

/**
 * 行为埋点上报。
 *
 * 埋点属于旁路逻辑：失败必须静默，不能弹出错误提示影响用户操作。
 */
export function track(payload) {
  try {
    request.post('/behavior/track', payload).catch(() => {})
  } catch (e) {
    // 忽略埋点异常
  }
}

/**
 * 商品浏览（带停留时长，用于画像的时间加权）
 */
export function trackView(productId, stayMs, scene = 'DETAIL') {
  track({
    productId,
    behaviorType: 'VIEW',
    stayMs: stayMs || 0,
    scene
  })
}

/**
 * 加入购物车
 */
export function trackCart(productId, scene = 'DETAIL') {
  track({ productId, behaviorType: 'CART', scene })
}

/**
 * 搜索关键词
 */
export function trackSearch(keyword) {
  if (!keyword) return
  track({ behaviorType: 'SEARCH', keyword, scene: 'SEARCH' })
}
