<template>
  <div class="address-page">
    <UserHeader />
    <div class="address-main">
      <div class="page-header">
        <h2 class="page-title">收货地址管理</h2>
        <el-button type="primary" @click="openDialog(null)">新增地址</el-button>
      </div>

      <div v-loading="loading">
        <div v-for="addr in addresses" :key="addr.id" class="address-card">
          <div class="address-info">
            <div class="address-top">
              <span class="receiver-name">{{ addr.receiverName }}</span>
              <span class="receiver-phone">{{ addr.receiverPhone }}</span>
              <el-tag v-if="addr.isDefault === 1" type="danger" size="small">默认</el-tag>
            </div>
            <div class="receiver-address">{{ addr.receiverAddress }}</div>
          </div>
          <div class="address-actions">
            <el-button v-if="addr.isDefault !== 1" text type="primary" @click="setDefault(addr.id)">设为默认</el-button>
            <el-button text type="primary" @click="openDialog(addr)">编辑</el-button>
            <el-button text type="danger" @click="deleteAddress(addr.id)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!loading && addresses.length === 0" description="暂无收货地址" />
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑地址' : '新增地址'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="详细地址" prop="receiverAddress">
          <el-input v-model="form.receiverAddress" type="textarea" :rows="2" placeholder="请输入详细收货地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import request from '../utils/request'

const loading = ref(false)
const submitting = ref(false)
const addresses = ref([])
const dialogVisible = ref(false)
const editing = ref(null)
const formRef = ref()

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  isDefault: 0
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  receiverAddress: [{ required: true, message: '请输入详细收货地址', trigger: 'blur' }]
}

async function loadAddresses() {
  loading.value = true
  try {
    const res = await request.get('/addresses')
    addresses.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(addr) {
  editing.value = addr
  if (addr) {
    form.receiverName = addr.receiverName
    form.receiverPhone = addr.receiverPhone
    form.receiverAddress = addr.receiverAddress
    form.isDefault = addr.isDefault
  } else {
    form.receiverName = ''
    form.receiverPhone = ''
    form.receiverAddress = ''
    form.isDefault = 0
  }
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (editing.value) {
      await request.put(`/addresses/${editing.value.id}`, form)
      ElMessage.success('地址修改成功')
    } else {
      await request.post('/addresses', form)
      ElMessage.success('地址添加成功')
    }
    dialogVisible.value = false
    loadAddresses()
  } finally {
    submitting.value = false
  }
}

async function setDefault(id) {
  await request.put(`/addresses/${id}/default`)
  ElMessage.success('已设为默认地址')
  loadAddresses()
}

async function deleteAddress(id) {
  await ElMessageBox.confirm('确定删除此收货地址？', '提示', { type: 'warning' })
  await request.delete(`/addresses/${id}`)
  ElMessage.success('地址已删除')
  loadAddresses()
}

onMounted(loadAddresses)
</script>

<style scoped>
.address-main { max-width: 900px; margin: 20px auto; padding: 0 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { font-size: 22px; color: #333; }
.address-card { background: #fff; border-radius: 8px; padding: 16px 20px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.address-info { flex: 1; }
.address-top { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.receiver-name { font-size: 16px; font-weight: 500; color: #333; }
.receiver-phone { font-size: 14px; color: #606266; }
.receiver-address { font-size: 14px; color: #909399; }
.address-actions { display: flex; gap: 4px; }
</style>
