<template>
  <div class="order-manage">
    <h2 class="page-title">订单管理</h2>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索订单号/用户名" style="width:240px" clearable @keyup.enter="loadOrders" @clear="loadOrders" />
      <el-select v-model="statusFilter" placeholder="订单状态" clearable style="width:140px" @change="loadOrders">
        <el-option label="待发货" value="PENDING" />
        <el-option label="已发货" value="SHIPPED" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-button type="primary" @click="loadOrders">搜索</el-button>
    </div>
    <el-table :data="orders" border v-loading="loading">
      <el-table-column label="订单号" prop="orderNo" width="200" />
      <el-table-column label="用户" prop="username" width="100" />
      <el-table-column label="金额" width="100">
        <template #default="{ row }"><span class="price">¥{{ row.totalAmount }}</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="typeMap[row.status]">{{ textMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收货人" prop="receiverName" width="100" />
      <el-table-column label="电话" prop="receiverPhone" width="130" />
      <el-table-column label="下单时间" prop="createTime" width="180" />
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="primary" @click="shipOrder(row)">发货</el-button>
          <el-button v-if="row.status === 'SHIPPED'" size="small" type="success" @click="completeOrder(row)">强制完成</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="10" v-model:current-page="page" @current-change="loadOrders" />
    </div>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ textMap[currentOrder.status] }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ currentOrder.username }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ currentOrder.totalAmount }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ currentOrder.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ currentOrder.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ currentOrder.receiverAddress }}</el-descriptions-item>
        </el-descriptions>
        <h4 style="margin:16px 0 8px">商品明细</h4>
        <el-table :data="currentOrder.items" border size="small">
          <el-table-column label="商品" prop="productName" />
          <el-table-column label="单价" prop="productPrice" width="100">
            <template #default="{ row }">¥{{ row.productPrice }}</template>
          </el-table-column>
          <el-table-column label="数量" prop="quantity" width="80" />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const statusFilter = ref('')
const detailVisible = ref(false)
const currentOrder = ref(null)

const textMap = { PENDING: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const typeMap = { PENDING: 'warning', SHIPPED: 'primary', COMPLETED: 'success', CANCELLED: 'info' }

async function loadOrders() {
  loading.value = true
  try {
    const res = await request.get('/orders/admin', { params: { page: page.value, size: 10, status: statusFilter.value, keyword: keyword.value } })
    orders.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function viewDetail(row) {
  const res = await request.get(`/orders/admin/${row.id}`)
  currentOrder.value = { ...res.data, username: row.username }
  detailVisible.value = true
}

async function shipOrder(row) {
  await ElMessageBox.confirm(`确定将订单「${row.orderNo}」发货？`, '发货确认')
  await request.put(`/orders/admin/${row.id}/ship`)
  ElMessage.success('订单已发货')
  loadOrders()
}

async function completeOrder(row) {
  await ElMessageBox.confirm(`确定强制完成订单「${row.orderNo}」？`, '强制完成确认')
  await request.put(`/orders/admin/${row.id}/complete`)
  ElMessage.success('订单已完成')
  loadOrders()
}

onMounted(loadOrders)
</script>

<style scoped>
.page-title { font-size: 22px; color: #333; margin-bottom: 20px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
