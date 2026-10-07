<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { BadgeCheck, KeyRound, Save, ShieldAlert, UserRound } from '@lucide/vue'
import { authApi } from '../api'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const profileRef = ref()
const passwordRef = ref()
const savingProfile = ref(false)
const savingPassword = ref(false)
const activeTab = ref('profile')

const profileForm = reactive({
  displayName: auth.user?.displayName || '',
  email: auth.user?.email || '',
  phone: auth.user?.phone || '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const profileRules = {
  displayName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

const validatePasswordConfirm = (_rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validatePasswordConfirm, trigger: 'blur' },
  ],
}

const roleText = computed(() => {
  if (auth.roles.includes('ADMIN')) return '系统管理员'
  if (auth.roles.includes('APPROVER')) return '审批管理员'
  return '普通用户'
})

async function saveProfile() {
  const valid = await profileRef.value.validate().catch(() => false)
  if (!valid) return
  savingProfile.value = true
  try {
    await authApi.updateProfile(profileForm)
    await auth.fetchMe()
    ElMessage.success('个人信息已更新')
  } finally {
    savingProfile.value = false
  }
}

async function changePassword() {
  const valid = await passwordRef.value.validate().catch(() => false)
  if (!valid) return
  savingPassword.value = true
  try {
    await authApi.changePassword({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码修改成功')
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div class="page profile-page">
    <header class="page-header">
      <div>
        <h2 class="page-title">个人资料</h2>
        <p class="page-subtitle">维护联系方式和登录密码，账号与角色由系统管理员管理。</p>
      </div>
    </header>

    <section class="profile-overview">
      <span class="profile-avatar">{{ auth.user?.displayName?.slice(0, 1) }}</span>
      <div>
        <h3>{{ auth.user?.displayName }}</h3>
        <p>{{ auth.user?.username }} · {{ roleText }}</p>
      </div>
      <el-tag :type="auth.user?.bookingAllowed ? 'success' : 'danger'" effect="light">
        {{ auth.user?.bookingAllowed ? '预约权限正常' : '禁止预约' }}
      </el-tag>
    </section>

    <el-alert
      v-if="auth.user && !auth.user.bookingAllowed"
      type="warning"
      :closable="false"
      show-icon
      :title="`当前账号已被禁止预约：${auth.user.bookingBlockReason || '请联系系统管理员'}`"
    />

    <section class="surface">
      <el-tabs v-model="activeTab" class="profile-tabs">
        <el-tab-pane name="profile">
          <template #label><UserRound :size="15" /> 基本信息</template>
          <el-form
            ref="profileRef"
            :model="profileForm"
            :rules="profileRules"
            label-position="top"
            class="profile-form"
          >
            <el-form-item label="姓名" prop="displayName">
              <el-input v-model="profileForm.displayName" maxlength="50" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="name@usst.edu.cn" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profileForm.phone" maxlength="30" />
            </el-form-item>
            <el-button type="primary" :loading="savingProfile" @click="saveProfile">
              <Save :size="16" /> 保存资料
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane name="password">
          <template #label><KeyRound :size="15" /> 修改密码</template>
          <el-form
            ref="passwordRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-position="top"
            class="profile-form"
          >
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input v-model="passwordForm.currentPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-button type="primary" :loading="savingPassword" @click="changePassword">
              <BadgeCheck :size="16" /> 更新密码
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section class="surface account-policy">
      <ShieldAlert :size="22" />
      <div>
        <strong>账号规则</strong>
        <p>姓名、邮箱和手机号可自行修改；用户名、角色和预约权限由系统管理员维护。</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.profile-overview {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  align-items: center;
  gap: 15px;
  padding: 20px;
  border: 1px solid var(--line);
  border-left: 4px solid var(--blue);
  border-radius: 8px;
  background: white;
}

.profile-avatar {
  display: grid;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 50%;
  background: var(--blue-soft);
  color: var(--blue);
  font-size: 21px;
  font-weight: 760;
}

.profile-overview h3 {
  margin: 0;
  font-size: 18px;
}

.profile-overview p {
  margin: 6px 0 0;
  color: var(--muted);
  font-size: 12px;
}

.profile-tabs {
  padding: 0 20px 20px;
}

.profile-form {
  width: min(520px, 100%);
  padding-top: 10px;
}

.account-policy {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  align-items: start;
  gap: 12px;
  padding: 18px;
  color: var(--muted);
}

.account-policy svg {
  color: var(--teal);
}

.account-policy strong {
  color: var(--ink);
}

.account-policy p {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 620px) {
  .profile-overview {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .profile-overview > .el-tag {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
