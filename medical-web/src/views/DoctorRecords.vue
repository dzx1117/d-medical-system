<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span>我填写的病历</span>
      </template>

      <el-empty v-if="!loading && list.length === 0" description="暂无病历记录" />

      <el-table v-else :data="list" v-loading="loading" border stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <el-descriptions :column="2" border size="small" style="margin: 8px 20px; width: auto">
              <el-descriptions-item label="主诉">{{ row.chiefComplaint || '-' }}</el-descriptions-item>
              <el-descriptions-item label="诊断">{{ row.diagnosis || '-' }}</el-descriptions-item>
              <el-descriptions-item label="现病史" :span="2">{{ row.presentIllness || '-' }}</el-descriptions-item>
              <el-descriptions-item label="治疗方案" :span="2">{{ row.treatmentPlan || '-' }}</el-descriptions-item>
              <el-descriptions-item v-if="row.remark" label="备注" :span="2">{{ row.remark }}</el-descriptions-item>
            </el-descriptions>
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="患者" width="110" />
        <el-table-column prop="visitDate" label="就诊日期" width="120" />
        <el-table-column prop="departmentName" label="科室" width="130" />
        <el-table-column prop="diagnosis" label="诊断" show-overflow-tooltip />
        <el-table-column prop="createTime" label="填写时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openPrescription(row)">开处方</el-button>
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

    <!-- 开处方 -->
    <el-dialog v-model="rxVisible" title="开具处方" width="820px">
      <el-alert
        :title="`患者：${currentRecord?.patientName || ''}　诊断：${currentRecord?.diagnosis || '未填写'}`"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-form label-width="90px">
        <el-form-item v-for="(item, index) in rxForm.items" :key="index" :label="`药品 ${index + 1}`">
          <div class="rx-row">
            <el-select
              v-model="item.medicineId"
              placeholder="选择药品"
              filterable
              style="width: 260px"
              @change="(val) => onMedicineChange(index, val)"
            >
              <el-option
                v-for="m in medicines"
                :key="m.id"
                :label="`${m.name}（${m.spec || '-'}）库存${m.stock}`"
                :value="m.id"
              />
            </el-select>
            <el-input v-model="item.dosage" placeholder="剂量 如 1片" style="width: 120px" />
            <el-input v-model="item.frequency" placeholder="频次 如 每日3次" style="width: 150px" />
            <el-input v-model="item.duration" placeholder="疗程 如 7天" style="width: 120px" />
            <el-input-number v-model="item.quantity" :min="1" :max="99" style="width: 110px" />
            <el-button type="danger" link @click="removeItem(index)" :disabled="rxForm.items.length === 1">
              删除
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <el-button type="primary" plain size="small" @click="addItem">+ 添加药品</el-button>

      <div class="rx-total">
        合计金额：<span>¥{{ totalAmount }}</span>
      </div>

      <template #footer>
        <el-button @click="rxVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPrescription">开具处方</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageDoctorRecords, createPrescription } from '../api/clinic.js'
import { listMedicine } from '../api/basic.js'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const medicines = ref([])
const rxVisible = ref(false)
const currentRecord = ref(null)

const query = reactive({ pageNum: 1, pageSize: 10 })

const rxForm = reactive({ items: [newItem()] })

function newItem() {
  return { medicineId: null, dosage: '', frequency: '', duration: '', quantity: 1, price: 0 }
}

const totalAmount = computed(() =>
  rxForm.items.reduce((sum, it) => sum + (it.price || 0) * (it.quantity || 0), 0).toFixed(2)
)

const load = async () => {
  loading.value = true
  try {
    const data = await pageDoctorRecords(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const openPrescription = async (row) => {
  currentRecord.value = row
  rxForm.items = [newItem()]
  medicines.value = await listMedicine()
  rxVisible.value = true
}

const onMedicineChange = (index, medicineId) => {
  const m = medicines.value.find((x) => x.id === medicineId)
  rxForm.items[index].price = m ? Number(m.price) : 0
}

const addItem = () => rxForm.items.push(newItem())

const removeItem = (index) => rxForm.items.splice(index, 1)

const submitPrescription = async () => {
  const invalid = rxForm.items.some((it) => !it.medicineId || !it.quantity)
  if (invalid) {
    ElMessage.warning('请为每一行选择药品并填写数量')
    return
  }
  await ElMessageBox.confirm(`确认开具处方，合计 ¥${totalAmount.value}？`, '确认', { type: 'info' })

  saving.value = true
  try {
    await createPrescription({
      medicalRecordId: currentRecord.value.id,
      items: rxForm.items.map((it) => ({
        medicineId: it.medicineId,
        dosage: it.dosage,
        frequency: it.frequency,
        duration: it.duration,
        quantity: it.quantity
      }))
    })
    ElMessage.success('处方开具成功')
    rxVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rx-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.rx-total {
  margin-top: 16px;
  text-align: right;
  font-size: 15px;
  color: #666;
}

.rx-total span {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
