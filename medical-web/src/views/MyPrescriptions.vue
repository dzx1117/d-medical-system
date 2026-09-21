<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span>我的处方</span>
      </template>

      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-table :data="row.items" size="small" border style="margin: 8px 20px; width: auto">
              <el-table-column prop="medicineName" label="药品" width="200" />
              <el-table-column prop="spec" label="规格" width="140" />
              <el-table-column prop="dosage" label="剂量" width="100" />
              <el-table-column prop="frequency" label="频次" width="110" />
              <el-table-column prop="duration" label="疗程" width="100" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="unitPrice" label="单价" width="90" />
              <el-table-column prop="totalPrice" label="小计" width="90" />
            </el-table>
          </template>
        </el-table-column>
        <el-table-column prop="prescriptionNo" label="处方单号" width="220" />
        <el-table-column prop="doctorName" label="开方医生" width="110" />
        <el-table-column prop="diagnosis" label="诊断" width="160" show-overflow-tooltip />
        <el-table-column prop="totalAmount" label="总金额(元)" width="120" />
        <el-table-column prop="statusDesc" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="开方时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" link type="primary" @click="pay(row)">缴费</el-button>
            <el-button v-else-if="row.status === 1" link type="success" @click="pickup(row)">取药</el-button>
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
import { pageMyPrescriptions, payPrescription, pickupPrescription } from '../api/clinic.js'

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
    const data = await pageMyPrescriptions(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const pay = async (row) => {
  await ElMessageBox.confirm(`确认缴费 ¥${row.totalAmount}？`, '缴费确认', { type: 'info' })
  await payPrescription(row.id)
  ElMessage.success('缴费成功')
  load()
}

const pickup = async (row) => {
  await ElMessageBox.confirm('确认已取药？', '提示', { type: 'info' })
  await pickupPrescription(row.id)
  ElMessage.success('取药成功')
  load()
}

onMounted(load)
</script>
