<template>
  <div class="products-page">
    <UserHeader ref="headerRef" />
    <div class="products-main">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="query.keyword" placeholder="搜索商品..." style="width:240px" clearable @clear="applyFilter" @keyup.enter="applyFilter" />
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width:140px" @change="applyFilter">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="query.sort" placeholder="排序" style="width:160px" @change="applyFilter">
            <el-option label="默认排序（最新上架）" value="" />
            <el-option label="价格从低到高" value="price-asc" />
            <el-option label="价格从高到低" value="price-desc" />
            <el-option label="随机排序" value="random" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button type="primary" @click="applyFilter">搜索</el-button>
        </div>
      </div>

      <div class="hero-toggle">
        <el-button link type="primary" size="small" @click="toggleHero">
          <el-icon v-if="heroCollapsed"><ArrowDown /></el-icon>
          <el-icon v-else><ArrowUp /></el-icon>
          {{ heroCollapsed ? '展开轮播图与热藏榜' : '收起轮播图与热藏榜' }}
        </el-button>
      </div>

      <div v-show="!heroCollapsed" class="hero-row">
        <div class="hero-carousel">
          <el-carousel v-if="banners.length" height="100%" :interval="4000" arrow="hover"
                       indicator-position="inside">
            <el-carousel-item v-for="b in banners" :key="b.id">
              <img :src="b.imageUrl" :alt="b.title || '轮播图'" class="hero-img"
                   @error="handleImgError" />
            </el-carousel-item>
          </el-carousel>
          <div v-else class="hero-empty">暂无轮播图</div>
        </div>

        <div v-if="hotProducts.length" class="hero-hot">
          <div class="hot-head">
            <span class="hot-title">热藏榜</span>
            <span class="hot-sub">近 7 天</span>
          </div>
          <div class="hot-list">
            <div v-for="(p, i) in hotProducts" :key="p.id" class="hot-item" @click="goDetail(p.id)">
              <span class="hot-rank" :class="'r' + (i + 1)">{{ i + 1 }}</span>
              <div class="hot-text">
                <div class="hot-name">{{ p.name }}</div>
                <div class="hot-reason">{{ p.reason }}</div>
              </div>
            </div>
          </div>
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

      <div v-if="forYou.length" class="rec-section">
        <div class="rec-head">
          <span class="rec-title">为你推荐</span>
          <span class="rec-sub">基于你的浏览与加购偏好</span>
        </div>
        <div class="rec-list">
          <div v-for="p in forYou" :key="p.id" class="rec-card" @click="goDetail(p.id)">
            <img :src="p.imageUrl" :alt="p.name" @error="handleImgError" />
            <div class="rec-name">{{ p.name }}</div>
            <div class="rec-price">¥{{ p.price }}</div>
            <div class="rec-reason">{{ p.reason }}</div>
          </div>
        </div>
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
import { track, trackSearch } from '../utils/track'

const router = useRouter()
const userStore = useUserStore()
const headerRef = ref()
const loading = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)
const hotProducts = ref([])
const forYou = ref([])
const banners = ref([])
// 顶部轮播与热藏榜是否收起：筛选商品时自动收起，避免挡住结果
const heroCollapsed = ref(false)

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
    if (query.keyword) {
      trackSearch(query.keyword)
    }
  } finally {
    loading.value = false
  }
}

// 筛选类操作：先收起顶部轮播与热藏榜，让商品直接进入视野，并把页码重置回第一页
function applyFilter() {
  heroCollapsed.value = true
  query.page = 1
  loadProducts()
}

function toggleHero() {
  heroCollapsed.value = !heroCollapsed.value
}

// 首页轮播图
async function loadBanners() {
  try {
    const res = await request.get('/banners/list')
    banners.value = res.data || []
  } catch (e) {
    banners.value = []
  }
}

// 热藏榜：热度召回，游客也能看到（右侧竖排，与轮播图等高）
async function loadHot() {
  try {
    const res = await request.get('/recommend/hot', { params: { limit: 6 } })
    hotProducts.value = res.data || []
  } catch (e) {
    hotProducts.value = []
  }
}

// 为你推荐：未登录时后端自动退化为热度榜（冷启动）
async function loadForYou() {
  try {
    const res = await request.get('/recommend/for-you', { params: { limit: 4 } })
    forYou.value = res.data || []
  } catch (e) {
    forYou.value = []
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
  track({ productId, behaviorType: 'CART', scene: 'LIST' })
}

function handleImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200"><rect fill="%23f0f0f0" width="200" height="200"/><text x="50%25" y="50%25" font-size="14" fill="%23999" text-anchor="middle" dy=".3em">暂无图片</text></svg>'
}

onMounted(() => {
  loadProducts()
  loadCategories()
  loadBanners()
  loadHot()
  loadForYou()
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

.hero-toggle {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
}
.hero-toggle .el-button { font-size: 12px; }

/* 首页 3:1 区域：左侧轮播图（16:9）+ 右侧热藏榜竖排，两者等高 */
.hero-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  align-items: stretch;
}
.hero-carousel {
  flex: 3 1 0;
  min-width: 0;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}
.hero-carousel :deep(.el-carousel),
.hero-carousel :deep(.el-carousel__container) {
  height: 100%;
}
.hero-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  font-size: 13px;
}
.hero-hot {
  flex: 1 1 0;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  padding: 12px 14px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.hot-head { display: flex; align-items: baseline; gap: 8px; margin-bottom: 8px; flex: 0 0 auto; }
.hot-title { font-size: 15px; font-weight: bold; color: #e1251b; }
.hot-sub { font-size: 11px; color: #999; }
.hot-list {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 0;
}
.hot-item { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 3px 0; min-height: 0; }
.hot-item:hover .hot-name { color: #e1251b; }
.hot-rank {
  flex: 0 0 18px;
  width: 18px; height: 18px; line-height: 18px; text-align: center;
  border-radius: 4px; font-size: 11px; color: #fff; background: #c0c4cc;
}
.hot-rank.r1 { background: #e1251b; }
.hot-rank.r2 { background: #f56c6c; }
.hot-rank.r3 { background: #f89898; }
.hot-text { min-width: 0; flex: 1 1 auto; }
.hot-name {
  font-size: 13px; color: #333;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.hot-reason {
  font-size: 11px; color: #999;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
@media (max-width: 900px) {
  .hero-row { flex-direction: column; }
  .hero-carousel { width: 100%; }
  .hero-hot { height: 300px; }
}

/* 为你推荐 */
.rec-section { margin-top: 40px; }
.rec-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 14px; }
.rec-title { font-size: 18px; font-weight: bold; color: #333; }
.rec-sub { font-size: 12px; color: #999; }
.rec-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px; }
.rec-card {
  background: #fff; border-radius: 8px; padding: 12px;
  cursor: pointer; transition: box-shadow 0.2s, transform 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.rec-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.12); transform: translateY(-2px); }
.rec-card img { width: 100%; height: 110px; object-fit: contain; background: #f5f5f5; border-radius: 4px; }
.rec-name { font-size: 13px; color: #333; margin-top: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-price { font-size: 15px; color: #e1251b; font-weight: bold; margin-top: 4px; }
.rec-reason { font-size: 11px; color: #909399; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
