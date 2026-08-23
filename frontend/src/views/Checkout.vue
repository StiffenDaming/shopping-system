<template>
  <div class="checkout-page">
    <UserHeader />
    <div class="checkout-main">
      <h2 class="page-title">确认订单</h2>
      <div class="checkout-content" v-loading="loading">
        <el-card class="address-card">
          <template #header>
            <div class="card-header">
              <span>收货信息</span>
              <el-select v-if="savedAddresses.length > 0" v-model="selectedAddressId" placeholder="选择已保存地址" style="width:280px" @change="onAddressSelect">
                <el-option v-for="addr in savedAddresses" :key="addr.id" :label="`${addr.receiverName} - ${addr.receiverAddress}`" :value="addr.id" />
              </el-select>
            </div>
          </template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-form-item label="收货人" prop="receiverName">
              <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" style="width:300px" />
            </el-form-item>
            <el-form-item label="手机号" prop="receiverPhone">
              <el-input v-model="form.receiverPhone" placeholder="请输入手机号" style="width:300px" />
            </el-form-item>
            <el-form-item label="收货地址" prop="receiverAddress">
              <el-input v-model="form.receiverAddress" type="textarea" :rows="2" placeholder="请输入详细收货地址" style="width:500px" />
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="goods-card">
          <template #header><span>商品清单</span></template>
          <el-table :data="cartItems" border>
            <el-table-column label="商品" min-width="250">
              <template #default="{ row }">
                <div class="cart-product">
                  <img :src="row.productImage" class="cart-img" @error="handleImgError" />
                  <span>{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120" align="center">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column label="数量" width="100" align="center" prop="quantity" />
            <el-table-column label="小计" width="120" align="center">
              <template #default="{ row }"><span class="price">¥{{ (row.price * row.quantity).toFixed(2) }}</span></template>
            </el-table-column>
          </el-table>
        </el-card>

        <div class="checkout-footer">
          <div class="checkout-total">
            共 {{ totalCount }} 件商品，合计：<span class="total-price">¥{{ totalAmount }}</span>
          </div>
          <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import request from '../utils/request'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const cartItems = ref([])
const savedAddresses = ref([])
const selectedAddressId = ref(null)

const form = reactive({ receiverName: '', receiverPhone: '', receiverAddress: '' })
const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

const totalCount = computed(() => cartItems.value.reduce((s, i) => s + i.quantity, 0))
const totalAmount = computed(() => cartItems.value.reduce((s, i) => s + i.price * i.quantity, 0).toFixed(2))

async function loadCart() {
  loading.value = true
  try {
    const res = await request.get('/cart')
    cartItems.value = res.data || []
    if (cartItems.value.length === 0) {
      ElMessage.warning('购物车为空')
      router.push('/products')
    }
  } finally {
    loading.value = false
  }
}

async function loadSavedAddresses() {
  try {
    const res = await request.get('/addresses')
    savedAddresses.value = res.data || []
    // 如果有默认地址，自动选中
    const defaultAddr = savedAddresses.value.find(a => a.isDefault === 1)
    if (defaultAddr) {
      selectedAddressId.value = defaultAddr.id
      fillAddress(defaultAddr)
    }
  } catch (e) { /* ignore */ }
}

function onAddressSelect(addrId) {
  const addr = savedAddresses.value.find(a => a.id === addrId)
  if (addr) fillAddress(addr)
}

function fillAddress(addr) {
  form.receiverName = addr.receiverName
  form.receiverPhone = addr.receiverPhone
  form.receiverAddress = addr.receiverAddress
}

async function submitOrder() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const res = await request.post('/orders/checkout', form)
    ElMessage.success('下单成功！')
    router.push('/orders')
  } catch (e) {
    // 错误已处理
  } finally {
    submitting.value = false
  }
}

function handleImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60"><rect fill="%23eee" width="60" height="60"/></svg>'
}

onMounted(() => {
  loadCart()
  loadSavedAddresses()
})
</script>

<style scoped>
.checkout-main { max-width: 900px; margin: 20px auto; padding: 0 20px; }
.page-title { font-size: 22px; color: #333; margin-bottom: 20px; }
.address-card, .goods-card { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.cart-product { display: flex; align-items: center; gap: 12px; }
.cart-img { width: 50px; height: 50px; object-fit: contain; }
.checkout-footer { display: flex; justify-content: flex-end; align-items: center; gap: 30px; background: #fff; padding: 16px 24px; border-radius: 8px; }
.checkout-total { font-size: 16px; }
.total-price { font-size: 24px; color: #e1251b; font-weight: bold; }
</style>
