<template>
  <div class="user-header">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/products" class="logo">购物商城</router-link>
      </div>
      <div class="header-nav">
        <router-link to="/products" class="nav-link">商品列表</router-link>
        <template v-if="userStore.isLoggedIn">
          <router-link to="/orders" class="nav-link">
            <el-badge :is-dot="unreadCount > 0" class="order-badge">
              我的订单
            </el-badge>
          </router-link>
          <router-link to="/address" class="nav-link">收货地址</router-link>
          <router-link to="/cart" class="nav-link cart-link">
            <el-badge :value="cartCount" :hidden="cartCount === 0" :max="99">
              购物车
            </el-badge>
          </router-link>
          <el-dropdown @command="handleCommand">
            <span class="nav-link user-name">
              {{ userStore.userInfo?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="address">收货地址</el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>管理后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-link">登录</router-link>
          <router-link to="/register" class="nav-link">注册</router-link>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const cartCount = ref(0)
const unreadCount = ref(0)
let pollTimer = null

async function fetchCartCount() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await request.get('/cart/count')
    cartCount.value = res.data || 0
  } catch (e) { /* ignore */ }
}

async function fetchUnreadCount() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await request.get('/orders/unread-count')
    unreadCount.value = res.data || 0
  } catch (e) { /* ignore */ }
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (cmd === 'admin') {
    router.push('/admin')
  } else if (cmd === 'orders') {
    router.push('/orders')
  } else if (cmd === 'address') {
    router.push('/address')
  }
}

defineExpose({ fetchCartCount, fetchUnreadCount })

onMounted(() => {
  fetchCartCount()
  fetchUnreadCount()
  // 每30秒轮询一次未读订单数
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.user-header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}
.header-nav {
  display: flex;
  align-items: center;
  gap: 24px;
}
.nav-link {
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  display: flex;
  align-items: center;
}
.nav-link:hover { color: #409eff; }
.user-name { color: #303133; font-weight: 500; }
.order-badge { margin-right: 4px; }
</style>
