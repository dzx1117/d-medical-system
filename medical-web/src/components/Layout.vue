<template>
  <el-container class="layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">智慧医院系统</div>
      <el-menu
        :default-active="$route.path"
        background-color="#1a5f7a"
        text-color="#e0f0f5"
        active-text-color="#ffd04b"
        router
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">智慧医院预约挂号与电子病历管理系统</div>
        <div class="header-right">
          <el-tag :type="userTypeTag" size="small">{{ userTypeDesc }}</el-tag>
          <span class="username">{{ userStore.userInfo?.realName || userStore.userInfo?.username }}</span>
          <el-button type="danger" size="small" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/index.js'
import { getUserInfo } from '../api/auth.js'

const router = useRouter()
const userStore = useUserStore()

// 按角色区分菜单：0患者 1医生 2管理员
const menus = computed(() => {
  const type = userStore.userInfo?.userType

  if (type === 2) {
    return [
      { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
      { path: '/statistics', title: '数据看板', icon: 'DataAnalysis' },
      { path: '/department', title: '科室管理', icon: 'OfficeBuilding' },
      { path: '/doctor', title: '医生管理', icon: 'User' },
      { path: '/schedule', title: '排班管理', icon: 'Calendar' },
      { path: '/medicine', title: '药品管理', icon: 'FirstAidKit' }
    ]
  }

  if (type === 1) {
    return [
      { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
      { path: '/doctor-appointments', title: '我的患者', icon: 'Tickets' },
      { path: '/doctor-records', title: '我的病历', icon: 'Document' },
      { path: '/doctor-prescriptions', title: '我的处方', icon: 'Memo' }
    ]
  }

  return [
    { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
    { path: '/book', title: '预约挂号', icon: 'Calendar' },
    { path: '/my-appointments', title: '我的预约', icon: 'Tickets' },
    { path: '/my-records', title: '我的病历', icon: 'Document' },
    { path: '/my-prescriptions', title: '我的处方', icon: 'Memo' }
  ]
})

const userTypeDesc = computed(() => {
  const map = { 0: '患者', 1: '医生', 2: '管理员' }
  return map[userStore.userInfo?.userType] ?? '未知'
})

const userTypeTag = computed(() => {
  const map = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[userStore.userInfo?.userType] ?? 'info'
})

onMounted(async () => {
  if (!userStore.userInfo) {
    try {
      const info = await getUserInfo()
      userStore.setUserInfo(info)
    } catch (e) {
      router.push('/login')
    }
  }
})

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.sidebar {
  background-color: #1a5f7a;
  overflow-y: auto;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 17px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.sidebar :deep(.el-menu) {
  border-right: none;
}

.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.header-title {
  font-size: 17px;
  font-weight: bold;
  color: #1a5f7a;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  color: #444;
}

.main {
  background-color: #f5f7fa;
}
</style>
