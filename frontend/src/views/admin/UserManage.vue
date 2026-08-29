<template>
  <div class="user-manage">
    <h2 class="page-title">用户管理</h2>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名/邮箱" style="width:260px" clearable @keyup.enter="loadUsers" @clear="loadUsers" />
      <el-button type="primary" @click="loadUsers">搜索</el-button>
    </div>
    <el-table :data="users" border v-loading="loading">
      <el-table-column label="ID" prop="id" width="60" />
      <el-table-column label="用户名" prop="username" width="120" />
      <el-table-column label="邮箱" prop="email" width="200" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" prop="createTime" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <template v-if="row.role !== 'ADMIN'">
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button size="small" type="danger" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="10" v-model:current-page="page" @current-change="loadUsers" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const res = await request.get('/admin/users', { params: { page: page.value, size: 10, keyword: keyword.value } })
    users.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await request.put(`/admin/users/${row.id}/status`, null, { params: { status: newStatus } })
  ElMessage.success('状态已更新')
  loadUsers()
}

async function resetPwd(row) {
  await ElMessageBox.confirm(`确定将「${row.username}」的密码重置为123456？`, '提示', { type: 'warning' })
  await request.put(`/admin/users/${row.id}/reset-password`)
  ElMessage.success('密码已重置为123456')
}

onMounted(loadUsers)
</script>

<style scoped>
.page-title { font-size: 22px; color: #333; margin-bottom: 20px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
