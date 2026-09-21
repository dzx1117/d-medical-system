<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span>欢迎使用智慧医院管理系统</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="当前用户">
          {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          {{ userTypeDesc }}
        </el-descriptions-item>
        <el-descriptions-item label="手机号">
          {{ maskedPhone }}
        </el-descriptions-item>
        <el-descriptions-item label="当前时间">
          {{ currentTime }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <template #header>
        <span>功能说明</span>
      </template>
      <el-alert
        v-for="tip in tips"
        :key="tip"
        :title="tip"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 8px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useUserStore } from '../store/index.js'

const userStore = useUserStore()
const currentTime = ref('')
let timer = null

const userTypeDesc = computed(() => {
  const map = { 0: '患者', 1: '医生', 2: '管理员' }
  return map[userStore.userInfo?.userType] ?? '未知'
})

// 手机号脱敏：138****8888
const maskedPhone = computed(() => {
  const phone = userStore.userInfo?.phone
  if (!phone || phone.length < 7) return phone || '-'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
})

const tips = computed(() => {
  const type = userStore.userInfo?.userType
  if (type === 2) {
    return [
      '科室管理：维护医院科室信息',
      '医生管理：新增医生账号并分配科室',
      '排班管理：为医生设置出诊日期、时段与号源数量',
      '药品管理：维护药品字典与库存'
    ]
  }
  if (type === 1) {
    return [
      '我的患者：查看预约了您号源的患者，可确认就诊',
      '我的病历：为就诊患者填写电子病历',
      '我的处方：根据病历开具处方，系统自动扣减药品库存'
    ]
  }
  return [
    '预约挂号：按科室、医生、日期筛选号源并预约',
    '我的预约：查看预约状态，可取消未就诊的预约',
    '我的病历：查看医生为您填写的就诊记录',
    '我的处方：查看处方明细，完成缴费与取药'
  ]
})

onMounted(() => {
  const tick = () => {
    currentTime.value = new Date().toLocaleString('zh-CN')
  }
  tick()
  timer = setInterval(tick, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>
