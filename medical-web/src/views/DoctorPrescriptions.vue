<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span>我开具的处方</span>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table :data="row.items" size="small" border style="margin: 8px 20px; width: auto">
              <el-table-column prop="medicineName" label="药品" width="200" />
              <el-table-column prop="spec" label="规格" width="140" />
              <el-table-column prop="dosage" label="剂量" width="100" />
              <el-table-column prop="frequency" label="频次" width="120" />
              <el-table-column prop="duration" label="疗程" width="100" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="unitPrice" label="单价" width="90" />
              <el-table-column prop="totalPrice" label="小计" width="90" />
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="prescriptionNo" label="处方单号" width="220" />
        <el-table-column prop="patientName" label="患者" width="110" />
        <el-table-column prop="diagnosis" label="诊断" width="180" show-overflow-tooltip />
        <el-table-column prop="totalAmount" label="总金额(元)" width="120" />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="开方时间" width="170" />
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
import { pageDoctorPrescriptions } from '../api/clinic.js'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10 })

const statusTag = (status) => {
  const map = { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }
  return map[status] ?? 'info'
}

const load = async () => {
  loading.value = true
  try {
    const data = await pageDoctorPrescriptions(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
