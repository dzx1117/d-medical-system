<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="医生姓名">
          <el-input v-model="query.realName" placeholder="请输入姓名" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="所属科室">
          <el-select v-model="query.departmentId" placeholder="全部科室" clearable style="width: 180px">
            <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="openDialog()">新增医生</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column prop="username" label="登录账号" width="130" />
        <el-table-column prop="departmentName" label="所属科室" width="130" />
        <el-table-column prop="title" label="职称" width="130" />
        <el-table-column prop="phone" label="手机号" width="130">
          <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
        </el-table-column>
        <el-table-column prop="fee" label="挂号费(元)" width="110">
          <template #default="{ row }">{{ row.fee ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="specialty" label="擅长领域" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑医生' : '新增医生'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="登录账号" prop="username">
          <el-input v-model="form.username" :disabled="!!form.id" placeholder="医生登录系统的账号" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="设置初始密码" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" maxlength="11" />
        </el-form-item>
        <el-form-item label="所属科室" prop="departmentId">
          <el-select v-model="form.departmentId" placeholder="请选择科室" style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-select v-model="form.title" placeholder="请选择职称" style="width: 100%">
            <el-option v-for="t in titles" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="挂号费(元)">
          <el-input-number v-model="form.fee" :min="0" :precision="2" :step="5" />
        </el-form-item>
        <el-form-item label="擅长领域">
          <el-input v-model="form.specialty" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="医生简介">
          <el-input v-model="form.introduction" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageDoctor, saveDoctor, updateDoctor, deleteDoctor, listDepartment } from '../api/basic.js'

const titles = ['主任医师', '副主任医师', '主治医师', '住院医师']

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const departments = ref([])
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ pageNum: 1, pageSize: 10, realName: '', departmentId: null })

const form = reactive({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  departmentId: null,
  title: '',
  specialty: '',
  introduction: '',
  fee: 20
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请设置初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择所属科室', trigger: 'change' }],
  title: [{ required: true, message: '请选择职称', trigger: 'change' }]
}

const maskPhone = (phone) => {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

const load = async () => {
  loading.value = true
  try {
    const data = await pageDoctor(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.realName = ''
  query.departmentId = null
  query.pageNum = 1
  load()
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, { ...row, password: '' })
  } else {
    Object.assign(form, {
      id: null, username: '', password: '', realName: '', phone: '',
      departmentId: null, title: '', specialty: '', introduction: '', fee: 20
    })
  }
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateDoctor(form)
      ElMessage.success('修改成功')
    } else {
      await saveDoctor(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除医生「${row.realName}」吗？`, '提示', { type: 'warning' })
  await deleteDoctor(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  departments.value = await listDepartment()
  load()
})
</script>
