<template>
  <div class="cart-page">
    <UserHeader />
    <div class="cart-main">
      <h2 class="page-title">购物车</h2>
      <div v-loading="loading">
        <el-table :data="cartItems" v-if="cartItems.length > 0" border>
          <el-table-column label="商品" min-width="300">
            <template #default="{ row }">
              <div class="cart-product">
                <img :src="row.productImage" class="cart-img" @error="handleImgError" />
                <span>{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="120" align="center">
            <template #default="{ row }"><span class="price">¥{{ row.price }}</span></template>
          </el-table-column>
          <el-table-column label="数量" width="160" align="center">
            <template #default="{ row }">
              <el-input-number v-model="row.quantity" :min="1" :max="row.productStock" size="small" @change="updateQty(row)" />
            </template>
          </el-table-column>
          <el-table-column label="小计" width="120" align="center">
            <template #default="{ row }"><span class="price">¥{{ (row.price * row.quantity).toFixed(2) }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button type="danger" text @click="removeItem(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="cartItems.length > 0" class="cart-footer">
          <div class="cart-total">
            共 {{ totalCount }} 件商品，合计：<span class="total-price">¥{{ totalAmount }}</span>
          </div>
          <el-button type="primary" size="large" @click="goCheckout">去结算</el-button>
        </div>

        <el-empty v-if="!loading && cartItems.length === 0" description="购物车是空的">
          <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
        </el-empty>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const cartItems = ref([])

const totalCount = computed(() => cartItems.value.reduce((s, i) => s + i.quantity, 0))
const totalAmount = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2))

async function loadCart() {
  loading.value = true
  try {
    const res = await request.get('/cart')
    cartItems.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function updateQty(row) {
  try {
    await request.put(`/cart/${row.id}`, null, { params: { quantity: row.quantity } })
  } catch (e) {
    loadCart()
  }
}

async function removeItem(id) {
  await ElMessageBox.confirm('确定要移除该商品吗？', '提示', { type: 'warning' })
  await request.delete(`/cart/${id}`)
  ElMessage.success('已移除')
  loadCart()
}

function goCheckout() {
  router.push('/checkout')
}

function handleImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60"><rect fill="%23eee" width="60" height="60"/></svg>'
}

onMounted(loadCart)
</script>

<style scoped>
.cart-main { max-width: 1000px; margin: 20px auto; padding: 0 20px; }
.page-title { font-size: 22px; color: #333; margin-bottom: 20px; }
.cart-product { display: flex; align-items: center; gap: 12px; }
.cart-img { width: 60px; height: 60px; object-fit: contain; border-radius: 4px; }
.cart-footer { display: flex; justify-content: flex-end; align-items: center; gap: 30px; margin-top: 20px; background: #fff; padding: 16px 24px; border-radius: 8px; }
.cart-total { font-size: 16px; color: #333; }
.total-price { font-size: 24px; color: #e1251b; font-weight: bold; }
</style>
