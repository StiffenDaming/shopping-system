<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-icon">
        <img src="https://www.saucedemo.com/static/media/login-robot.9c14f6b3.png" alt="logo" onerror="this.style.display='none'" />
      </div>
      <h2 class="login-title">购物商城系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>测试账号：</p>
        <p>管理员 admin / 123456</p>
        <p>普通用户 user / 123456</p>
      </div>
      <div class="login-footer">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push(userStore.isAdmin ? '/admin' : '/products')
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-box {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  width: 400px;
  max-width: 90vw;
}
.login-icon {
  text-align: center;
  margin-bottom: 20px;
}
.login-icon img {
  width: 80px;
  height: 80px;
}
.login-title {
  text-align: center;
  font-size: 24px;
  color: #333;
  margin-bottom: 30px;
}
.login-tips {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 20px;
  font-size: 13px;
  color: #909399;
  line-height: 1.8;
}
.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: #606266;
}
.login-footer a {
  color: #409eff;
}
</style>
