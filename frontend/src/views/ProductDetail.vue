<template>
  <div class="detail-page">
    <UserHeader />
    <div class="detail-main" v-loading="loading">
      <div v-if="product" class="detail-content">
        <div class="detail-img">
          <img :src="product.imageUrl" :alt="product.name" @error="handleImgError" />
        </div>
        <div class="detail-info">
          <h1 class="detail-name">{{ product.name }}</h1>
          <div class="detail-price">¥{{ product.price }}</div>
          <div class="detail-stock">库存：{{ product.stock }} 件</div>
          <div class="detail-desc">{{ product.description }}</div>
          <div class="detail-actions">
            <el-input-number v-model="quantity" :min="1" :max="product.stock" />
            <el-button type="primary" size="large" @click="addToCart">加入购物车</el-button>
            <el-button size="large" @click="buyNow">立即购买</el-button>
          </div>
        </div>
      </div>

      <div v-if="similar.length" class="similar-section">
        <div class="similar-head">
          <span class="similar-title">猜你喜欢</span>
          <span class="similar-sub">基于藏品标签相似度推荐</span>
        </div>
        <div class="similar-list">
          <div v-for="p in similar" :key="p.id" class="similar-card" @click="goProduct(p.id)">
            <img :src="p.imageUrl" :alt="p.name" @error="handleImgError" />
            <div class="similar-name">{{ p.name }}</div>
            <div class="similar-price">¥{{ p.price }}</div>
            <div class="similar-reason">{{ p.reason }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'
import { track } from '../utils/track'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const product = ref(null)
const quantity = ref(1)
const similar = ref([])

// 停留时长统计：进入详情页记时，离开时上报一次，供画像做时间加权
let enteredAt = 0
let viewSent = false

async function loadProduct() {
  loading.value = true
  try {
    const res = await request.get(`/products/detail/${route.params.id}`)
    product.value = res.data
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await request.post('/cart', null, { params: { productId: product.value.id, quantity: quantity.value } })
  ElMessage.success('已加入购物车')
  track({ productId: product.value.id, behaviorType: 'CART', scene: 'DETAIL' })
}

async function buyNow() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await request.post('/cart', null, { params: { productId: product.value.id, quantity: quantity.value } })
  router.push('/cart')
}

function handleImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400"><rect fill="%23f0f0f0" width="400" height="400"/><text x="50%25" y="50%25" font-size="16" fill="%23999" text-anchor="middle" dy=".3em">暂无图片</text></svg>'
}

// 上报浏览行为：带停留时长，作为用户画像的强信号
function sendView() {
  if (viewSent || !product.value) return
  viewSent = true
  track({
    productId: product.value.id,
    behaviorType: 'VIEW',
    stayMs: Math.max(Date.now() - enteredAt, 0),
    scene: 'DETAIL'
  })
}

// 猜你喜欢：内容过滤召回
async function loadSimilar() {
  if (!route.params.id) return
  try {
    const res = await request.get(`/recommend/similar/${route.params.id}`, { params: { limit: 4 } })
    similar.value = res.data || []
  } catch (e) {
    similar.value = []
  }
}

function goProduct(id) {
  router.push(`/product/${id}`)
}

function onPageHide() {
  sendView()
}

onMounted(async () => {
  await loadProduct()
  await loadSimilar()
  enteredAt = Date.now()
  viewSent = false
  window.addEventListener('pagehide', onPageHide)
})

onBeforeUnmount(() => {
  sendView()
  window.removeEventListener('pagehide', onPageHide)
})

// 详情页内跳转到另一件藏品时，组件不会重建，需要手动刷新
watch(() => route.params.id, async (val) => {
  if (!val) return
  sendView()
  viewSent = false
  await loadProduct()
  await loadSimilar()
  enteredAt = Date.now()
})
</script>

<style scoped>
.detail-main { max-width: 1000px; margin: 20px auto; padding: 0 20px; }
.detail-content { display: flex; gap: 40px; background: #fff; border-radius: 12px; padding: 30px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.detail-img { flex: 0 0 400px; }
.detail-img img { width: 100%; border-radius: 8px; }
.detail-info { flex: 1; display: flex; flex-direction: column; }
.detail-name { font-size: 24px; color: #333; margin-bottom: 16px; }
.detail-price { font-size: 32px; color: #e1251b; font-weight: bold; margin-bottom: 12px; }
.detail-stock { font-size: 14px; color: #999; margin-bottom: 20px; }
.detail-desc { font-size: 14px; color: #666; line-height: 1.8; margin-bottom: 30px; flex: 1; }
.detail-actions { display: flex; gap: 12px; align-items: center; }

/* 猜你喜欢 */
.similar-section { margin-top: 32px; }
.similar-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.similar-title { font-size: 18px; font-weight: bold; color: #333; }
.similar-sub { font-size: 12px; color: #999; }
.similar-list { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.similar-card {
  background: #fff; border-radius: 8px; padding: 12px;
  cursor: pointer; transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.similar-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.12); transform: translateY(-2px); }
.similar-card img { width: 100%; height: 120px; object-fit: contain; background: #f5f5f5; border-radius: 4px; }
.similar-name { font-size: 13px; color: #333; margin-top: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.similar-price { font-size: 15px; color: #e1251b; font-weight: bold; margin-top: 4px; }
.similar-reason { font-size: 11px; color: #909399; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
