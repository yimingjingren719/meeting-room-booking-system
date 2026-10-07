<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '../components/AuthShell.vue'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  displayName: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
})

const validateConfirm = (_rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9_]{4,30}$/,
      message: '需为 4-30 位字母、数字或下划线',
      trigger: 'blur',
    },
  ],
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await auth.register({
      username: form.username,
      displayName: form.displayName,
      password: form.password,
      email: form.email || null,
      phone: form.phone || null,
    })
    ElMessage.success('账号创建成功')
    router.replace('/')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell>
    <div class="register-heading">
      <p class="eyebrow">新用户注册</p>
      <h2>创建个人账号</h2>
      <p>注册后即可查询会议室、提交预约并接收审批结果。</p>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="submit"
    >
      <div class="form-grid">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="姓名" prop="displayName">
          <el-input v-model="form.displayName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="name@usst.edu.cn" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
      </div>

      <el-button
        class="submit-button"
        type="primary"
        native-type="submit"
        :loading="loading"
      >
        创建账号
      </el-button>
    </el-form>

    <p class="switch-entry">
      已有账号？
      <router-link to="/login">返回登录</router-link>
    </p>
  </AuthShell>
</template>

<style scoped>
.register-heading {
  margin-bottom: 22px;
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--blue);
  font-size: 12px;
  font-weight: 750;
  letter-spacing: 0.06em;
}

.register-heading h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 760;
}

.register-heading > p:last-child {
  margin: 8px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

.submit-button {
  width: 100%;
  min-height: 43px;
  margin-top: 4px;
}

.switch-entry {
  margin: 20px 0 0;
  color: var(--muted);
  font-size: 13px;
  text-align: center;
}

.switch-entry a {
  color: var(--blue);
  font-weight: 700;
}

@media (max-width: 520px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
