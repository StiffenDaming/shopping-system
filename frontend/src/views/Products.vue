<template>
  <div class="products-page">
    <UserHeader ref="headerRef" />
    <div class="products-main">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="query.keyword" placeholder="搜索商品..." style="width:240px" clearable @clear="loadProducts" @keyup.enter="loadProducts" />
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width:140px" @change="loadProducts">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="query.sort" placeholder="排序" style="width:160px" @change="loadProducts">
            <el-option label="默认排序" value="" />
            <el-option label="价格从低到高" value="price-asc" />
            <el-option label="价格从高到低" value="price-desc" />
            <el-option label="名称 A-Z" value="name-asc" />
            <el-option label="名称 Z-A" value="name-desc" />
            <el-option label="最新上架" value="newest" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button type="primary" @click="loadProducts">搜索</el-button>
        </div>
      </div>

      <div v-loading="loading" class="product-grid">
        <div v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
          <div class="product-img">
            <img :src="p.imageUrl" :alt="p.name" @error="handleImgError" />
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ p.name }}</h3>
            <p class="product-desc">{{ p.description }}</p>
            <div class="product-bottom">
              <span class="price">¥{{ p.price }}</span>
              <el-button type="primary" size="small" @click.stop="addToCart(p.id)">加入购物车</el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="!loading && products.length === 0" class="empty">
        <el-empty description="暂无商品" />
      </div>

      <div class="pagination" v-if="total > 0">
        <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.size" v-model:current-page="query.page" @current-change="loadProducts" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserHeader from '../components/UserHeader.vue'
import { useUserStore } from '../stores/user'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const headerRef = ref()
const loading = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  categoryId: null,
  sort: '',
  page: 1,
  size: 12
})

async function loadProducts() {
  loading.value = true
  try {
    const res = await request.get('/products/list', { params: query })
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

function goDetail(id) {
  router.push(`/product/${id}`)
}

async function addToCart(productId) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  await request.post('/cart', null, { params: { productId, quantity: 1 } })
  ElMessage.success('已加入购物车')
  headerRef.value?.fetchCartCount()
}

function handleImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect fill="%23f0f0f0" width="200" height="200"/><text x="50%25" y="50%25" font-size="14" fill="%23999" text-anchor="middle" dy=".3em">暂无图片</text></svg>'
}

onMounted(() => {
  loadProducts()
  loadCategories()
})
</script>

<style scoped>
.products-main { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  padding: 16px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}
.filter-left { display: flex; gap: 12px; }
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}
.product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.product-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  transform: translateY(-2px);
}
.product-img { width: 100%; height: 200px; overflow: hidden; background: #f5f5f5; }
.product-img img { width: 100%; height: 100%; object-fit: contain; }
.product-info { padding: 12px 16px; }
.product-name { font-size: 15px; color: #333; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-desc { font-size: 12px; color: #999; margin-bottom: 12px; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.product-bottom { display: flex; justify-content: space-between; align-items: center; }
.empty { padding: 60px 0; }
.pagination { display: flex; justify-content: center; margin-top: 30px; }
</style>
