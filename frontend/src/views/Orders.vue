<template>
  <div class="orders-page">
    <UserHeader ref="headerRef" />
    <div class="orders-main">
      <h2 class="page-title">我的订单</h2>
      <div class="filter-bar">
        <el-radio-group v-model="statusFilter" @change="loadOrders">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="PENDING">待发货</el-radio-button>
          <el-radio-button label="SHIPPED">已发货</el-radio-button>
          <el-radio-button label="COMPLETED">已完成</el-radio-button>
          <el-radio-button label="CANCELLED">已取消</el-radio-button>
        </el-radio-group>
      </div>
      <div v-loading="loading">
        <div v-for="o in orders" :key="o.id" class="order-card">
          <div class="order-header">
            <span class="order-no">订单号：{{ o.orderNo }}</span>
            <span class="order-time">{{ o.createTime }}</span>
            <el-tag :type="statusType(o.status)" size="small">{{ statusText(o.status) }}</el-tag>
          </div>
          <div class="order-items">
            <div v-for="item in o.items" :key="item.id" class="order-item">
              <img :src="item.productImage" class="item-img" @error="handleImgError" />
              <div class="item-info">
                <div class="item-name">{{ item.productName }}</div>
                <div class="item-price">¥{{ item.productPrice }} × {{ item.quantity }}</div>
              </div>
            </div>
          </div>
          <div class="order-footer">
            <div class="order-address" v-if="o.receiverName">
              收货人：{{ o.receiverName }} {{ o.receiverPhone }} {{ o.receiverAddress }}
            </div>
            <div class="order-total">
              共 {{ o.items?.reduce((s, i) => s + i.quantity, 0) }} 件商品，合计：<span class="price">¥{{ o.totalAmount }}</span>
            </div>
            <div class="order-actions">
              <el-button v-if="o.status === 'PENDING'" type="danger" plain size="small" @click="cancelOrder(o.id)">取消订单</el-button>
              <el-button v-if="o.status === 'SHIPPED'" type="success" plain size="small" @click="confirmReceipt(o.id)">确认收货</el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
      </div>
      <div class="pagination" v-if="total > 0">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="10" v-model:current-page="page" @current-change="loadOrders" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import request from '../utils/request'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const statusFilter = ref('')
const headerRef = ref()

const statusMap = { PENDING: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const typeMap = { PENDING: 'warning', SHIPPED: 'primary', COMPLETED: 'success', CANCELLED: 'info' }
const statusText = (s) => statusMap[s] || s
const statusType = (s) => typeMap[s] || 'info'

async function loadOrders() {
  loading.value = true
  try {
    const res = await request.get('/orders', { params: { page: page.value, size: 10, status: statusFilter.value } })
    orders.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function cancelOrder(id) {
  await ElMessageBox.confirm('确定要取消此订单吗？', '提示', { type: 'warning' })
  await request.put(`/orders/${id}/cancel`)
  ElMessage.success('订单已取消')
  loadOrders()
}

async function confirmReceipt(id) {
  await ElMessageBox.confirm('确认已收到商品？', '确认收货', { type: 'success' })
  await request.put(`/orders/${id}/confirm`)
  ElMessage.success('已确认收货')
  loadOrders()
}

async function markOrdersAsRead() {
  try {
    await request.put('/orders/mark-read')
    // 刷新 Header 上的红点
    if (headerRef.value) {
      headerRef.value.fetchUnreadCount?.()
    }
  } catch (e) { /* ignore */ }
}

function handleImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60"><rect fill="%23eee" width="60" height="60"/></svg>'
}

onMounted(() => {
  loadOrders()
  markOrdersAsRead()
})
</script>

<style scoped>
.orders-main { max-width: 900px; margin: 20px auto; padding: 0 20px; }
.page-title { font-size: 22px; color: #333; margin-bottom: 20px; }
.filter-bar { margin-bottom: 20px; }
.order-card { background: #fff; border-radius: 8px; margin-bottom: 16px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.order-header { display: flex; align-items: center; gap: 16px; padding: 12px 20px; background: #f5f7fa; font-size: 13px; color: #606266; }
.order-no { font-weight: 500; }
.order-time { color: #909399; }
.order-items { padding: 16px 20px; }
.order-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; }
.item-img { width: 50px; height: 50px; object-fit: contain; border-radius: 4px; }
.item-info { flex: 1; }
.item-name { font-size: 14px; color: #333; }
.item-price { font-size: 12px; color: #999; }
.order-footer { display: flex; align-items: center; justify-content: space-between; padding: 12px 20px; border-top: 1px solid #f0f0f0; }
.order-address { font-size: 12px; color: #999; flex: 1; }
.order-total { font-size: 14px; color: #333; }
.order-actions { display: flex; gap: 8px; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
