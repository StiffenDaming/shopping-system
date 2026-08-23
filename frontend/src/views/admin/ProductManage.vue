<template>
  <div class="product-manage">
    <h2 class="page-title">商品管理</h2>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索商品名称" style="width:240px" clearable @keyup.enter="loadProducts" @clear="loadProducts" />
      <el-button type="primary" @click="loadProducts">搜索</el-button>
      <el-button type="success" @click="openDialog(null)">新增商品</el-button>
    </div>
    <el-table :data="products" border v-loading="loading">
      <el-table-column label="ID" prop="id" width="60" />
      <el-table-column label="图片" width="80">
        <template #default="{ row }">
          <img :src="row.imageUrl" style="width:50px;height:50px;object-fit:contain" @error="handleImgError" />
        </template>
      </el-table-column>
      <el-table-column label="名称" prop="name" min-width="160" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }"><span class="price">¥{{ row.price }}</span></template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">{{ row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button size="small" type="danger" @click="deleteProduct(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="10" v-model:current-page="page" @current-change="loadProducts" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingProduct.id ? '编辑商品' : '新增商品'" width="600px">
      <el-form ref="formRef" :model="editingProduct" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="editingProduct.name" placeholder="商品名称" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="editingProduct.price" :min="0" :precision="2" style="width:200px" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="editingProduct.stock" :min="0" style="width:200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="editingProduct.categoryId" placeholder="选择分类" style="width:200px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="editingProduct.imageUrl" placeholder="图片URL" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editingProduct.description" type="textarea" :rows="3" placeholder="商品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const loading = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)
const page = ref(1)
const keyword = ref('')
const dialogVisible = ref(false)
const formRef = ref()

const editingProduct = reactive({ id: null, name: '', price: 0, stock: 0, categoryId: null, imageUrl: '', description: '', status: 1 })
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await request.get('/products/admin', { params: { page: page.value, size: 10, keyword: keyword.value } })
    products.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  const res = await request.get('/categories/list')
  categories.value = res.data
}

function openDialog(product) {
  if (product) {
    Object.assign(editingProduct, product)
  } else {
    Object.assign(editingProduct, { id: null, name: '', price: 0, stock: 0, categoryId: null, imageUrl: '', description: '', status: 1 })
  }
  dialogVisible.value = true
}

async function saveProduct() {
  await formRef.value.validate()
  if (editingProduct.id) {
    await request.put('/products', { ...editingProduct })
    ElMessage.success('修改成功')
  } else {
    await request.post('/products', { ...editingProduct })
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
  loadProducts()
}

async function toggleStatus(row) {
  await request.put(`/products/${row.id}/status`, null, { params: { status: row.status === 1 ? 0 : 1 } })
  ElMessage.success('状态已更新')
  loadProducts()
}

async function deleteProduct(row) {
  await ElMessageBox.confirm(`确定删除商品「${row.name}」？`, '提示', { type: 'warning' })
  await request.delete(`/products/${row.id}`)
  ElMessage.success('删除成功')
  loadProducts()
}

function handleImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="50" height="50"><rect fill="%23eee" width="50" height="50"/></svg>'
}

onMounted(() => {
  loadProducts()
  loadCategories()
})
</script>

<style scoped>
.page-title { font-size: 22px; color: #333; margin-bottom: 20px; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; }
.pagination { display: flex; justify-content: center; margin-top: 20px; }
</style>
