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
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const product = ref(null)
const quantity = ref(1)

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

onMounted(loadProduct)
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
</style>
