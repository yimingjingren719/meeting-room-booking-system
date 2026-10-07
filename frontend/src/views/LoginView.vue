<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LockKeyhole, UserRound } from '@lucide/vue'
import AuthShell from '../components/AuthShell.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: 'user',
  password: 'user123',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.login(form)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

function useDemo(username, password) {
  form.username = username
  form.password = password
}
</script>

<template>
  <AuthShell>
    <div class="login-heading">
      <p class="eyebrow">校内统一身份入口</p>
      <h2>登录会议室预约系统</h2>
      <p>使用个人账号查看空闲时段、提交预约并跟踪审批进度。</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      size="large"
      @submit.prevent="submit"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" placeholder="请输入用户名">
          <template #prefix><UserRound :size="17" /></template>
        </el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          show-password
          placeholder="请输入密码"
          @keyup.enter="submit"
        >
          <template #prefix><LockKeyhole :size="17" /></template>
        </el-input>
      </el-form-item>
      <el-button
        class="submit-button"
        type="primary"
        native-type="submit"
        :loading="loading"
      >
        登录
      </el-button>
    </el-form>

    <div class="demo-login">
      <span>演示账号</span>
      <button type="button" @click="useDemo('user', 'user123')">普通用户</button>
      <button type="button" @click="useDemo('approver', 'approver123')">
        审批管理员
      </button>
      <button type="button" @click="useDemo('admin', 'admin123')">系统管理员</button>
    </div>

    <p class="switch-entry">
      还没有账号？
      <router-link to="/register">创建个人账号</router-link>
    </p>
  </AuthShell>
</template>

<style scoped>
.login-heading {
  margin-bottom: 28px;
}

.eyebrow {
  margin: 0 0 9px;
  color: var(--blue);
  font-size: 12px;
  font-weight: 750;
  letter-spacing: 0.06em;
}

.login-heading h2 {
  margin: 0;
  font-size: 29px;
  font-weight: 760;
}

.login-heading > p:last-child {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}

.submit-button {
  width: 100%;
  min-height: 44px;
  margin-top: 5px;
}

.demo-login {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fafbfc;
}

.demo-login span {
  color: var(--muted);
  font-size: 12px;
}

.demo-login button {
  padding: 4px 8px;
  border: 1px solid #d6dde8;
  border-radius: 6px;
  background: white;
  color: #344054;
  font-size: 11px;
  cursor: pointer;
}

.demo-login button:hover {
  border-color: #8bb2ff;
  color: var(--blue);
}

.switch-entry {
  margin: 22px 0 0;
  color: var(--muted);
  font-size: 13px;
  text-align: center;
}

.switch-entry a {
  color: var(--blue);
  font-weight: 700;
}
</style>

