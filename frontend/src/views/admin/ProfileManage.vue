<template>
  <div class="profile-page">
    <div class="page-head">
      <h2>用户画像</h2>
      <div class="head-actions">
        <el-button :loading="rebuilding" type="primary" @click="rebuild">重算画像</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="hint">
      画像由用户行为日志按时间衰减聚合而成：浏览 1 分、加购 3 分、下单 5 分，越久远的行为权重越低。
    </div>

    <el-table v-loading="loading" :data="paged" border stripe>
      <el-table-column label="ID" prop="userId" width="70" />
      <el-table-column label="用户名" prop="username" width="110" />
      <el-table-column label="活跃度" width="90">
        <template #default="{ row }">
          <el-tag :type="levelType(row.activityLevel)" size="small">{{ levelText(row.activityLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="价格偏好" width="110">
        <template #default="{ row }">
          <span v-if="row.pricePref !== null && row.pricePref !== undefined">¥{{ row.pricePref }}</span>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="浏览/加购/下单" width="120">
        <template #default="{ row }">{{ row.viewCnt }} / {{ row.cartCnt }} / {{ row.orderCnt }}</template>
      </el-table-column>
      <el-table-column label="兴趣标签 TOP5" min-width="260">
        <template #default="{ row }">
          <el-tag
            v-for="t in row.topTags"
            :key="t.name"
            class="tag"
            size="small"
            type="success"
          >{{ t.name }} {{ Number(t.weight).toFixed(2) }}</el-tag>
          <span v-if="!row.topTags || row.topTags.length === 0" class="muted">暂无行为数据</span>
        </template>
      </el-table-column>
      <el-table-column label="偏好分类 TOP5" min-width="260">
        <template #default="{ row }">
          <el-tag
            v-for="t in row.topCategories"
            :key="t.name"
            class="tag"
            size="small"
            type="warning"
          >{{ t.name }} {{ Number(t.weight).toFixed(2) }}</el-tag>
          <span v-if="!row.topCategories || row.topCategories.length === 0" class="muted">暂无行为数据</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        background
        layout="prev, pager, next, total"
        :page-size="pageSize"
        :total="profiles.length"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const profiles = ref([])
const loading = ref(false)
const rebuilding = ref(false)
const page = ref(1)
const pageSize = 10

const paged = computed(() => {
  const start = (page.value - 1) * pageSize
  return profiles.value.slice(start, start + pageSize)
})

async function load() {
  loading.value = true
  try {
    const res = await request.get('/admin/recommend/profiles')
    profiles.value = res.data || []
    page.value = 1
  } catch (e) {
    profiles.value = []
  } finally {
    loading.value = false
  }
}

async function rebuild() {
  rebuilding.value = true
  try {
    await request.post('/admin/recommend/rebuild')
    ElMessage.success('画像与热度已重算')
    await load()
  } finally {
    rebuilding.value = false
  }
}

function levelType(level) {
  if (level === 'HIGH') return 'danger'
  if (level === 'MID') return 'warning'
  return 'info'
}

function levelText(level) {
  if (level === 'HIGH') return '高'
  if (level === 'MID') return '中'
  return '低'
}

onMounted(load)
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-head h2 { font-size: 20px; color: #303133; }
.head-actions { display: flex; gap: 10px; }
.hint {
  font-size: 12px;
  color: #909399;
  background: #f4f8ff;
  border-left: 3px solid #409eff;
  padding: 8px 12px;
  border-radius: 4px;
  margin-bottom: 16px;
}
.tag { margin-right: 6px; margin-bottom: 4px; }
.muted { color: #c0c4cc; font-size: 12px; }
.pager { display: flex; justify-content: center; margin-top: 20px; }
</style>
