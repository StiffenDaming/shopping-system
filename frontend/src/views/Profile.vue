<template>
  <div class="profile-page">
    <UserHeader />
    <div class="profile-main" v-loading="loading">
      <el-card class="info-card">
        <template #header>
          <div class="card-head">
            <span class="card-title">账号信息</span>
            <span class="card-sub">如需修改邮箱等信息，请联系管理员</span>
          </div>
        </template>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="用户名">{{ info.username }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ info.email || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="info.role === 'ADMIN' ? 'danger' : 'info'" size="small">
              {{ info.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatTime(info.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="pwd-card">
        <template #header>
          <div class="card-head">
            <span class="card-title">修改密码</span>
            <span class="card-sub">修改成功后需要重新登录</span>
          </div>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="pwd-form">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="form.oldPassword" type="password" show-password
                      placeholder="请输入当前密码" autocomplete="current-password" />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="form.newPassword" type="password" show-password
                      placeholder="6~20 个字符" autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" show-password
                      placeholder="请再次输入新密码" autocomplete="new-password" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">确认修改</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>

        <el-alert type="info" :closable="false" show-icon
                  title="忘记密码怎么办？">
          <template #default>
            请联系管理员，由管理员在「用户管理」中把密码重置为
            <strong>123456</strong>；您用 123456 登录后，即可在此页面改成自己的密码。
          </template>
        </el-alert>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const formRef = ref()
const info = reactive({ username: '', email: '', role: '', createTime: '' })

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6~20 个字符', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value && value === form.oldPassword) {
          callback(new Error('新密码不能与原密码相同'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function loadInfo() {
  loading.value = true
  try {
    const res = await request.get('/auth/info')
    Object.assign(info, {
      username: res.data.username,
      email: res.data.email,
      role: res.data.role,
      createTime: res.data.createTime
    })
  } catch (e) {
    // 错误提示已由请求拦截器处理
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await request.put('/auth/password', null, {
      params: { oldPassword: form.oldPassword, newPassword: form.newPassword }
    })
    await ElMessageBox.alert('密码已修改，请使用新密码重新登录。', '修改成功', {
      confirmButtonText: '去登录',
      type: 'success'
    })
    // 清除本地登录态，强制重新登录
    userStore.logout()
    router.push('/login')
  } catch (e) {
    // 原密码错误等业务异常由拦截器统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(loadInfo)
</script>

<style scoped>
.profile-main { max-width: 720px; margin: 24px auto; padding: 0 20px; }
.info-card { margin-bottom: 20px; }
.card-head { display: flex; align-items: baseline; gap: 12px; }
.card-title { font-size: 16px; font-weight: 600; color: #303133; }
.card-sub { font-size: 12px; color: #909399; }
.pwd-form { max-width: 460px; margin-top: 8px; }
</style>
