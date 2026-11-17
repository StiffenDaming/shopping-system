<template>
  <div class="banner-page">
    <div class="page-head">
      <h2>轮播图管理</h2>
      <el-button type="primary" @click="openCreate">新增轮播图</el-button>
    </div>

    <el-alert type="info" :closable="false" show-icon class="tip"
              title="轮播图显示在首页顶部左侧，建议使用 16:9 的图片；排序值越小越靠前，下架后前台不再展示。" />

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column label="预览" width="200">
        <template #default="{ row }">
          <img :src="row.imageUrl" class="thumb" alt="预览" @error="onImgError" />
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" width="140" />
      <el-table-column label="图片地址" prop="imageUrl" show-overflow-tooltip />
      <el-table-column label="排序" prop="sort" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'"
                     @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑轮播图' : '新增轮播图'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="https://... （16:9 图片）" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="仅后台识别用，可不填" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
          <span class="hint">越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                     active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-form-item v-if="form.imageUrl" label="预览">
          <img :src="form.imageUrl" class="preview" alt="预览" @error="onImgError" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const list = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const formRef = ref()

const form = reactive({ id: null, imageUrl: '', title: '', sort: 0, status: 1 })

const rules = {
  imageUrl: [{ required: true, message: '请输入图片地址', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const res = await request.get('/admin/banners')
    list.value = res.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { id: null, imageUrl: '', title: '', sort: 0, status: 1 })
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, {
    id: row.id,
    imageUrl: row.imageUrl,
    title: row.title || '',
    sort: row.sort ?? 0,
    status: row.status
  })
  dialogVisible.value = true
}

async function save() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (form.id) {
      await request.put('/admin/banners', { ...form })
      ElMessage.success('修改成功')
    } else {
      await request.post('/admin/banners', { ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    await load()
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await request.put('/admin/banners', { ...row, status: next })
  ElMessage.success(next === 1 ? '已上架' : '已下架')
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm('确定删除这张轮播图吗？', '删除确认', { type: 'warning' })
  await request.delete(`/admin/banners/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

function onImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180"><rect fill="%23f0f0f0" width="320" height="180"/><text x="50%25" y="50%25" font-size="14" fill="%23999" text-anchor="middle" dy=".3em">图片加载失败</text></svg>'
}

onMounted(load)
</script>

<style scoped>
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-head h2 { font-size: 20px; color: #303133; }
.tip { margin-bottom: 16px; }
.thumb { width: 160px; height: 90px; object-fit: cover; border-radius: 4px; background: #f5f5f5; }
.preview { width: 320px; height: 180px; object-fit: cover; border-radius: 4px; border: 1px solid #dcdfe6; }
.hint { margin-left: 10px; font-size: 12px; color: #909399; }
</style>
