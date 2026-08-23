<template>
  <div class="dashboard" v-loading="loading">
    <h2 class="page-title">数据看板</h2>
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-label">总用户数</div>
        <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
        <div class="stat-extra">今日新增 {{ stats.todayUsers || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">商品总数</div>
        <div class="stat-value">{{ stats.totalProducts || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">订单总数</div>
        <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
        <div class="stat-extra">今日 {{ stats.todayOrders || 0 }} 单</div>
      </div>
      <div class="stat-card highlight">
        <div class="stat-label">总销售额</div>
        <div class="stat-value">¥{{ stats.totalRevenue || 0 }}</div>
      </div>
    </div>

    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-title">近7天订单趋势</div>
        <div ref="orderChartRef" style="height: 300px"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">各分类商品数</div>
        <div ref="categoryChartRef" style="height: 300px"></div>
      </div>
    </div>

    <div class="chart-card">
      <div class="chart-title">订单状态分布</div>
      <div class="status-list">
        <div class="status-item" v-for="(v, k) in stats.orderStatusCount" :key="k">
          <el-tag :type="typeMap[k]" size="large">{{ textMap[k] }}</el-tag>
          <span class="status-count">{{ v }} 单</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../../utils/request'

const loading = ref(false)
const stats = ref({})
const orderChartRef = ref()
const categoryChartRef = ref()

const textMap = { PENDING: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const typeMap = { PENDING: 'warning', SHIPPED: 'primary', COMPLETED: 'success', CANCELLED: 'info' }

async function loadStats() {
  loading.value = true
  try {
    const res = await request.get('/admin/stats')
    stats.value = res.data
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  // 订单趋势
  if (orderChartRef.value && stats.value.dailyOrders) {
    const chart = echarts.init(orderChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: stats.value.dailyOrders.map(d => d.date.slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ name: '订单数', type: 'bar', data: stats.value.dailyOrders.map(d => d.count), itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] } }]
    })
  }
  // 分类商品数
  if (categoryChartRef.value && stats.value.categoryProductCount) {
    const chart = echarts.init(categoryChartRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['40%', '70%'],
        data: stats.value.categoryProductCount.map(c => ({ name: c.name, value: c.count })),
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 }
      }]
    })
  }
}

onMounted(loadStats)
</script>

<style scoped>
.page-title { font-size: 22px; color: #333; margin-bottom: 20px; }
.stats-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.stat-card.highlight { background: linear-gradient(135deg, #ff9a56 0%, #ff6a88 100%); color: #fff; }
.stat-card.highlight .stat-label, .stat-card.highlight .stat-value { color: #fff; }
.stat-label { font-size: 14px; color: #909399; margin-bottom: 8px; }
.stat-value { font-size: 32px; font-weight: bold; color: #333; }
.stat-extra { font-size: 12px; color: #c0c4cc; margin-top: 4px; }
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
.chart-card { background: #fff; border-radius: 8px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); margin-bottom: 16px; }
.chart-title { font-size: 16px; font-weight: 500; color: #333; margin-bottom: 16px; }
.status-list { display: flex; gap: 30px; flex-wrap: wrap; }
.status-item { display: flex; align-items: center; gap: 10px; }
.status-count { font-size: 18px; font-weight: bold; color: #333; }
</style>
