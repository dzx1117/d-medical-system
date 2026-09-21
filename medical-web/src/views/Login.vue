<template>
  <div class="login-container">
    <el-card class="login-box" shadow="always">
      <template #header>
        <div class="login-title">智慧医院管理系统</div>
        <div class="login-sub">预约挂号 · 电子病历 · 处方管理</div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @keyup.enter="handleLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button link @click="$router.push('/register')" style="width: 100%">
            还没有账号？注册患者账号
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider>演示账号</el-divider>
      <div class="demo-tip">
        <div v-for="d in demos" :key="d.username" class="demo-item" @click="fillDemo(d)">
          <el-tag size="small" :type="d.type">{{ d.role }}</el-tag>
          <span>{{ d.username }} / {{ d.password }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login as loginApi } from '../api/auth.js'
import { useUserStore } from '../store/index.js'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demos = [
  { role: '管理员', username: 'admin', password: 'admin123', type: 'danger' }
]

const fillDemo = (d) => {
  form.username = d.username
  form.password = d.password
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await loginApi(form)
    userStore.setToken(data.token)
    userStore.setUserInfo(data.userInfo)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a5f7a 0%, #159895 100%);
}

.login-box {
  width: 420px;
}

.login-title {
  text-align: center;
  font-size: 21px;
  font-weight: bold;
  color: #1a5f7a;
}

.login-sub {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: 6px;
  letter-spacing: 1px;
}

.demo-tip {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.demo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px;
  border-radius: 4px;
  background: #f5f7fa;
  cursor: pointer;
  font-size: 13px;
  color: #666;
}

.demo-item:hover {
  background: #e8f0f4;
}
</style>
