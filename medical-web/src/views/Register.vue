<template>
  <div class="register-container">
    <el-card class="register-box" shadow="hover">
      <template #header>
        <div class="register-title">患者注册</div>
      </template>
      <el-form :model="form" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" style="width: 100%">注册</el-button>
        </el-form-item>
        <el-form-item>
          <el-button link @click="$router.push('/login')" style="width: 100%">已有账号？去登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request.js'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: ''
})

const handleRegister = async () => {
  if (!form.username || !form.password || !form.realName || !form.phone) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await request.post('/auth/register', form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    console.error(error)
  }
}
</script>

<style scoped>
.register-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a5f7a 0%, #159895 100%);
}

.register-box {
  width: 400px;
}

.register-title {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  color: #1a5f7a;
}
</style>
