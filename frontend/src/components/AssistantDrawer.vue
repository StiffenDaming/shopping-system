<template>
  <el-drawer
    v-model="visible"
    title="客服助手"
    direction="rtl"
    size="420px"
    :close-on-click-modal="false"
    class="assistant-drawer"
    @open="onOpen"
  >
    <div class="assistant-body">
      <div ref="listRef" class="msg-list">
        <div class="msg assistant">
          <div class="bubble">
            你好，我是收藏品商城的客服助手。可以先点下面的问题，或者直接描述你的疑问。
          </div>
        </div>

        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
          <div class="bubble">
            <div class="text">{{ m.text }}</div>
            <div v-if="m.source" class="meta">
              {{ sourceLabel(m.source) }}
            </div>
          </div>
        </div>

        <div v-if="loading" class="msg assistant">
          <div class="bubble thinking">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>
        </div>
      </div>

      <div class="quick-panel">
        <div class="quick-title">猜你想问</div>
        <div class="chips">
          <span
            v-for="q in quickQuestions"
            :key="q.id"
            class="chip"
            @click="send(q.question)"
          >{{ q.question }}</span>
        </div>
      </div>

      <div class="input-row">
        <el-input
          v-model="draft"
          type="textarea"
          :rows="2"
          resize="none"
          maxlength="200"
          show-word-limit
          placeholder="描述你的问题，回车发送"
          @keydown.enter.exact.prevent="send()"
        />
        <el-button type="primary" :loading="loading" :disabled="!draft.trim()" @click="send()">
          发送
        </el-button>
      </div>

      <div v-if="remaining !== null" class="remaining">
        今日剩余智能问答次数：{{ remaining }}
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const listRef = ref()
const messages = ref([])
const quickQuestions = ref([])
const draft = ref('')
const loading = ref(false)
const remaining = ref(null)
let loaded = false

const SOURCE_LABELS = {
  FAQ: '知识库',
  LLM: '智能回答',
  FALLBACK: '未命中'
}
function sourceLabel(source) {
  return SOURCE_LABELS[source] || ''
}

async function loadQuickQuestions() {
  try {
    const res = await request.get('/assistant/faq')
    quickQuestions.value = (res.data || []).slice(0, 6)
  } catch (e) {
    quickQuestions.value = []
  }
}

function onOpen() {
  if (loaded) return
  loaded = true
  loadQuickQuestions()
}

async function scrollToBottom() {
  await nextTick()
  const el = listRef.value
  if (el) el.scrollTop = el.scrollHeight
}

async function send(question) {
  const text = (question ?? draft.value).trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', text })
  draft.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const res = await request.post('/assistant/chat', { question: text })
    const data = res.data || {}
    messages.value.push({
      role: 'assistant',
      text: data.answer || '（没有返回内容）',
      source: data.source
    })
    if (typeof data.remaining === 'number') {
      remaining.value = data.remaining
    }
  } catch (e) {
    // 业务异常（如次数用尽）由拦截器提示，这里补一条可见的兜底消息
    const msg = e?.response?.data?.message || '网络异常，请稍后再试'
    messages.value.push({ role: 'assistant', text: msg })
    ElMessage.warning(msg)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

watch(visible, (v) => {
  if (v) scrollToBottom()
})
</script>

<style scoped>
.assistant-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
}
.msg-list {
  flex: 1 1 auto;
  overflow-y: auto;
  padding-right: 4px;
  min-height: 0;
}
.msg {
  display: flex;
  margin-bottom: 12px;
}
.msg.assistant { justify-content: flex-start; }
.msg.user { justify-content: flex-end; }
.bubble {
  max-width: 78%;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}
.msg.assistant .bubble {
  background: #f4f6f9;
  color: #303133;
}
.msg.user .bubble {
  background: #409eff;
  color: #fff;
}
.meta {
  margin-top: 6px;
  font-size: 11px;
  opacity: 0.6;
}
.thinking {
  display: flex;
  align-items: center;
  gap: 4px;
}
.dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #909399;
  animation: blink 1.2s infinite ease-in-out;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0.25; }
  40% { opacity: 1; }
}
.quick-panel {
  flex: 0 0 auto;
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}
.quick-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 96px;
  overflow-y: auto;
}
.chip {
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 12px;
  padding: 4px 10px;
  cursor: pointer;
  transition: background 0.2s;
}
.chip:hover { background: #d9ecff; }
.input-row {
  flex: 0 0 auto;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.input-row :deep(.el-textarea) { flex: 1 1 auto; }
.input-row :deep(.el-button) { height: 40px; }
.remaining {
  flex: 0 0 auto;
  font-size: 11px;
  color: #c0c4cc;
  text-align: right;
}
</style>
