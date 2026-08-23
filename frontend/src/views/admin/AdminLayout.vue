<template>
  <div class="admin-layout">
    <div class="admin-sidebar">
      <div class="sidebar-logo">管理后台</div>
      <el-menu :default-active="route.path" router class="sidebar-menu">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon><span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon><span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><List /></el-icon><span>订单管理</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-bottom">
        <el-button text @click="$router.push('/products')">
          <el-icon><Back /></el-icon> 返回前台
        </el-button>
        <el-button text @click="handleLogout">退出登录</el-button>
      </div>
    </div>
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; }
.admin-sidebar { width: 220px; background: #304156; display: flex; flex-direction: column; flex-shrink: 0; }
.sidebar-logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 20px; font-weight: bold; border-bottom: 1px solid #3a4a5e; }
.sidebar-menu { border-right: none; background: transparent; }
.sidebar-menu .el-menu-item { color: #bfcbd9; }
.sidebar-menu .el-menu-item:hover { background: #263445; color: #fff; }
.sidebar-menu .el-menu-item.is-active { background: #409eff; color: #fff; }
.sidebar-bottom { margin-top: auto; padding: 16px; display: flex; flex-direction: column; gap: 8px; }
.sidebar-bottom .el-button { color: #bfcbd9; justify-content: flex-start; }
.admin-content { flex: 1; background: #f0f2f5; overflow-y: auto; padding: 20px; }
</style>
