<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span>我的病历</span>
      </template>

      <el-empty v-if="!loading && list.length === 0" description="暂无病历记录" />

      <el-timeline v-else>
        <el-timeline-item
          v-for="item in list"
          :key="item.id"
          :timestamp="`${item.visitDate} · ${item.departmentName || ''} ${item.doctorName || ''}`"
          placement="top"
          color="#1a5f7a"
        >
          <el-card shadow="hover">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="主诉">{{ item.chiefComplaint || '-' }}</el-descriptions-item>
              <el-descriptions-item label="现病史">{{ item.presentIllness || '-' }}</el-descriptions-item>
              <el-descriptions-item label="诊断">
                <el-tag type="danger" size="small">{{ item.diagnosis || '-' }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="治疗方案">{{ item.treatmentPlan || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="item.remark" label="备注">{{ item.remark }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-timeline-item>
      </el-timeline>

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
import { pageMyRecords } from '../api/clinic.js'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

const load = async () => {
  loading.value = true
  try {
    const data = await pageMyRecords(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
