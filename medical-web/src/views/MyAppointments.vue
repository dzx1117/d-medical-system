<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true">
        <el-form-item label="预约状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待就诊" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="appointmentNo" label="预约单号" width="200" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column prop="title" label="职称" width="120" />
        <el-table-column prop="departmentName" label="科室" width="120" />
        <el-table-column prop="appointmentDate" label="就诊日期" width="120" />
        <el-table-column prop="timeSlotDesc" label="时段" width="80" />
        <el-table-column prop="fee" label="挂号费" width="90" />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              link
              type="danger"
              @click="cancel(row)"
            >取消预约</el-button>
            <span v-else style="color: #bbb">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="load"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageMyAppointments, cancelAppointment } from '../api/clinic.js'

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10, status: null })

const statusTag = (status) => {
  const map = { 0: 'warning', 1: 'success', 2: 'info' }
  return map[status] ?? 'info'
}

const load = async () => {
  loading.value = true
  try {
    const data = await pageMyAppointments(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.status = null
  query.pageNum = 1
  load()
}

const cancel = async (row) => {
  await ElMessageBox.confirm('取消后将释放号源，确定取消该预约吗？', '提示', { type: 'warning' })
  await cancelAppointment(row.id)
  ElMessage.success('已取消预约')
  load()
}

onMounted(load)
</script>
